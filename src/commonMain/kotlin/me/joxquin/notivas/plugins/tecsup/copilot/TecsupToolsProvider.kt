package me.joxquin.notivas.plugins.tecsup.copilot

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import me.joxquin.notivas.plugins.tecsup.api.TecsupApiClient
import me.joxquin.notivas.plugins.tecsup.session.TecsupSessionManager

data class PluginToolDefinition(
    val name: String,
    val description: String,
    val parametersJson: String
)

class TecsupToolsProvider(
    private val apiClient: TecsupApiClient,
    private val sessionManager: TecsupSessionManager
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val toolDefinitions: List<PluginToolDefinition> = listOf(
        PluginToolDefinition(
            name = "get_tecsup_schedule",
            description = "Obtiene el horario de clases y laboratorios del estudiante en Tecsup.",
            parametersJson = """
                {
                  "type": "object",
                  "properties": {
                    "day": {
                      "type": "string",
                      "description": "Dia de la semana opcional para filtrar (ej. 'Lunes', 'Martes', 'Hoy')."
                    }
                  }
                }
            """.trimIndent()
        ),
        PluginToolDefinition(
            name = "get_tecsup_grades",
            description = "Obtiene las calificaciones ponderadas y el estado de los cursos de Tecsup.",
            parametersJson = """
                {
                  "type": "object",
                  "properties": {
                    "courseName": {
                      "type": "string",
                      "description": "Nombre o fragmento del curso de Tecsup a consultar."
                    }
                  }
                }
            """.trimIndent()
        ),
        PluginToolDefinition(
            name = "get_tecsup_attendance",
            description = "Obtiene el reporte de asistencias, inasistencias y límite máximo de faltas en Tecsup.",
            parametersJson = """
                {
                  "type": "object",
                  "properties": {
                    "onlyAlerts": {
                      "type": "boolean",
                      "description": "Si es true, solo retorna cursos donde las inasistencias estan cerca del limite maximo."
                    }
                  }
                }
            """.trimIndent()
        )
    )

    fun canHandle(toolName: String): Boolean {
        return toolName in listOf("get_tecsup_schedule", "get_tecsup_grades", "get_tecsup_attendance")
    }

    suspend fun executeTool(toolName: String, args: JsonObject): String {
        if (!sessionManager.isConnected) {
            return """{"error": "La sesion de Tecsup no esta activa. Inicia sesion en la tarjeta de Tecsup en Utilidades."}"""
        }

        return when (toolName) {
            "get_tecsup_schedule" -> {
                val result = apiClient.fetchSchedule()
                result.fold(
                    onSuccess = { items ->
                        json.encodeToString(items)
                    },
                    onFailure = { error ->
                        """{"error": "Fallo al obtener horario de Tecsup: ${error.message}"}"""
                    }
                )
            }
            "get_tecsup_grades" -> {
                val periodsResult = apiClient.fetchAcademicPeriods()
                val periods = periodsResult.getOrNull() ?: emptyList()
                val currentPeriod = periods.firstOrNull { it.isCurrent } ?: periods.firstOrNull()

                if (currentPeriod == null) {
                    """{"error": "No se encontraron periodos academicos activos en Tecsup."}"""
                } else {
                    val gradesResult = apiClient.fetchGrades(currentPeriod.id)
                    gradesResult.fold(
                        onSuccess = { items ->
                            json.encodeToString(items)
                        },
                        onFailure = { error ->
                            """{"error": "Fallo al obtener notas de Tecsup: ${error.message}"}"""
                        }
                    )
                }
            }
            "get_tecsup_attendance" -> {
                val attendanceResult = apiClient.fetchAttendance()
                attendanceResult.fold(
                    onSuccess = { items ->
                        json.encodeToString(items)
                    },
                    onFailure = { error ->
                        """{"error": "Fallo al obtener asistencias de Tecsup: ${error.message}"}"""
                    }
                )
            }
            else -> """{"error": "Herramienta no implementada: $toolName"}"""
        }
    }
}
