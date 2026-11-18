package me.joxquin.notivas.plugins.tecsup.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.joxquin.notivas.plugins.tecsup.model.TecsupAcademicPeriod
import me.joxquin.notivas.plugins.tecsup.model.TecsupAttendanceItem
import me.joxquin.notivas.plugins.tecsup.model.TecsupEvaluation
import me.joxquin.notivas.plugins.tecsup.model.TecsupGradeProgress
import me.joxquin.notivas.plugins.tecsup.model.TecsupScheduleItem
import me.joxquin.notivas.plugins.tecsup.model.TecsupUser
import me.joxquin.notivas.plugins.tecsup.session.TecsupSessionManager

class TecsupApiClient(
    private val sessionManager: TecsupSessionManager,
    private val customHttpClient: HttpClient? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client: HttpClient = customHttpClient ?: HttpClient {
        install(ContentNegotiation) {
            json(this@TecsupApiClient.json)
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        defaultRequest {
            url(TecsupEndpoints.BASE_URL)
        }
    }

    private fun getAuthToken(): String {
        return sessionManager.currentToken ?: throw IllegalStateException("No active Tecsup session")
    }

    suspend fun registerSession(
        deviceId: String = "notivas-kmp",
        brand: String = "Android",
        model: String = "KMP",
        osVersion: String = "14",
        appVersion: String = "4.0.7",
        platform: String = "Android"
    ): Result<Unit> = runCatching {
        client.post(TecsupEndpoints.SESSION_LOGIN) {
            header("Authorization", "Bearer ${getAuthToken()}")
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("deviceId", deviceId)
                    put("firebaseToken", "")
                    put("brand", brand)
                    put("model", model)
                    put("osVersion", osVersion)
                    put("appVersion", appVersion)
                    put("platform", platform)
                    put("loginTime", "2026-11-18T16:00:00Z")
                }.toString()
            )
        }
    }

    suspend fun fetchUserInfo(): Result<TecsupUser> = runCatching {
        val token = getAuthToken()
        val localUser = sessionManager.parseUserFromToken(token)

        val response = try {
            client.get(TecsupEndpoints.USER_INFO) {
                header("Authorization", "Bearer $token")
            }
        } catch (e: Exception) {
            if (localUser != null) return@runCatching localUser
            throw e
        }

        if (response.status == HttpStatusCode.OK) {
            val rawJson = response.bodyAsText()
            val obj = json.decodeFromString(JsonObject.serializer(), rawJson)
            val remoteUser = TecsupUser(
                id = obj["id"]?.jsonPrimitive?.content ?: localUser?.id ?: "",
                name = obj["name"]?.jsonPrimitive?.content ?: obj["fullName"]?.jsonPrimitive?.content ?: localUser?.name ?: "",
                email = obj["email"]?.jsonPrimitive?.content ?: localUser?.email ?: "",
                studentCode = obj["studentCode"]?.jsonPrimitive?.content ?: obj["code"]?.jsonPrimitive?.content ?: localUser?.studentCode ?: "",
                campus = obj["campus"]?.jsonPrimitive?.content ?: localUser?.campus ?: "Tecsup",
                career = obj["career"]?.jsonPrimitive?.content ?: localUser?.career ?: "Estudiante"
            )
            sessionManager.saveToken(token, remoteUser)
            remoteUser
        } else {
            localUser ?: run {
                sessionManager.markExpired()
                error("Session expired or invalid: ${response.status}")
            }
        }
    }

    suspend fun fetchAcademicPeriods(): Result<List<TecsupAcademicPeriod>> = runCatching {
        val response = client.get(TecsupEndpoints.ACADEMIC_PERIODS) {
            header("Authorization", "Bearer ${getAuthToken()}")
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            error("Unauthorized")
        }
        val rawJson = response.bodyAsText()
        val array = json.decodeFromString(JsonArray.serializer(), rawJson)
        array.map { element ->
            val obj = element.jsonObject
            TecsupAcademicPeriod(
                id = obj["id"]?.jsonPrimitive?.content ?: "",
                name = obj["name"]?.jsonPrimitive?.content ?: "",
                isCurrent = obj["isCurrent"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                startDate = obj["startDate"]?.jsonPrimitive?.content ?: "",
                endDate = obj["endDate"]?.jsonPrimitive?.content ?: ""
            )
        }
    }

    suspend fun fetchGrades(academicPeriodId: String): Result<List<TecsupGradeProgress>> = runCatching {
        val response = client.get(TecsupEndpoints.gradeProgressStatus(academicPeriodId)) {
            header("Authorization", "Bearer ${getAuthToken()}")
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            error("Unauthorized")
        }
        val rawJson = response.bodyAsText()
        val array = json.decodeFromString(JsonArray.serializer(), rawJson)
        array.map { elem ->
            val obj = elem.jsonObject
            val evaluations = obj["evaluations"]?.jsonArray?.map { evalElem ->
                val evalObj = evalElem.jsonObject
                TecsupEvaluation(
                    id = evalObj["id"]?.jsonPrimitive?.content ?: "",
                    name = evalObj["name"]?.jsonPrimitive?.content ?: "",
                    weightPercentage = evalObj["weight"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    grade = evalObj["grade"]?.jsonPrimitive?.doubleOrNull,
                    date = evalObj["date"]?.jsonPrimitive?.content ?: "",
                    isEvaluated = evalObj["isEvaluated"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                )
            } ?: emptyList()

            TecsupGradeProgress(
                sectionId = obj["sectionId"]?.jsonPrimitive?.content ?: "",
                courseName = obj["courseName"]?.jsonPrimitive?.content ?: obj["name"]?.jsonPrimitive?.content ?: "",
                courseCode = obj["courseCode"]?.jsonPrimitive?.content ?: "",
                currentAverage = obj["average"]?.jsonPrimitive?.doubleOrNull,
                status = obj["status"]?.jsonPrimitive?.content ?: "",
                evaluations = evaluations
            )
        }
    }

    suspend fun fetchAttendance(): Result<List<TecsupAttendanceItem>> = runCatching {
        val response = client.get(TecsupEndpoints.ATTENDANCE_SUMMARY) {
            header("Authorization", "Bearer ${getAuthToken()}")
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            error("Unauthorized")
        }
        val rawJson = response.bodyAsText()
        val array = json.decodeFromString(JsonArray.serializer(), rawJson)
        array.map { elem ->
            val obj = elem.jsonObject
            val total = obj["totalClasses"]?.jsonPrimitive?.intOrNull ?: 0
            val absent = obj["absentClasses"]?.jsonPrimitive?.intOrNull ?: 0
            val maxAllowed = obj["maxAllowedAbsences"]?.jsonPrimitive?.intOrNull ?: 0
            val attended = obj["attendedClasses"]?.jsonPrimitive?.intOrNull ?: (total - absent)
            val percentage = if (total > 0) ((attended.toDouble() / total) * 100.0) else 100.0

            TecsupAttendanceItem(
                sectionId = obj["sectionId"]?.jsonPrimitive?.content ?: "",
                courseName = obj["courseName"]?.jsonPrimitive?.content ?: "",
                totalClasses = total,
                attendedClasses = attended,
                absentClasses = absent,
                tardyClasses = obj["tardyClasses"]?.jsonPrimitive?.intOrNull ?: 0,
                maxAllowedAbsences = maxAllowed,
                attendancePercentage = percentage,
                isAtRisk = maxAllowed > 0 && absent >= (maxAllowed - 1)
            )
        }
    }

    suspend fun fetchSchedule(): Result<List<TecsupScheduleItem>> = runCatching {
        val response = client.get(TecsupEndpoints.SCHEDULE) {
            header("Authorization", "Bearer ${getAuthToken()}")
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            error("Unauthorized")
        }
        val rawJson = response.bodyAsText()
        val array = json.decodeFromString(JsonArray.serializer(), rawJson)
        array.map { elem ->
            val obj = elem.jsonObject
            TecsupScheduleItem(
                id = obj["id"]?.jsonPrimitive?.content ?: "",
                sectionId = obj["sectionId"]?.jsonPrimitive?.content ?: "",
                courseName = obj["courseName"]?.jsonPrimitive?.content ?: "",
                dayOfWeek = obj["dayOfWeek"]?.jsonPrimitive?.content ?: "",
                startTime = obj["startTime"]?.jsonPrimitive?.content ?: "",
                endTime = obj["endTime"]?.jsonPrimitive?.content ?: "",
                classroom = obj["classroom"]?.jsonPrimitive?.content ?: "",
                building = obj["building"]?.jsonPrimitive?.content ?: "",
                teacherName = obj["teacherName"]?.jsonPrimitive?.content ?: "",
                eventType = obj["type"]?.jsonPrimitive?.content ?: "Class"
            )
        }
    }
}
