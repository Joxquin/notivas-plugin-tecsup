package me.joxquin.notivas.plugins.tecsup.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TecsupUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val studentCode: String = "",
    val campus: String = "",
    val career: String = ""
)

@Serializable
data class TecsupAcademicPeriod(
    val id: String,
    val name: String,
    val isCurrent: Boolean = false,
    val startDate: String = "",
    val endDate: String = ""
)

@Serializable
data class TecsupGradeProgress(
    val sectionId: String = "",
    val courseName: String = "",
    val courseCode: String = "",
    val currentAverage: Double? = null,
    val status: String = "",
    val evaluations: List<TecsupEvaluation> = emptyList()
)

@Serializable
data class TecsupEvaluation(
    val id: String = "",
    val name: String = "",
    val weightPercentage: Double = 0.0,
    val grade: Double? = null,
    val date: String = "",
    val isEvaluated: Boolean = false
)

@Serializable
data class TecsupAttendanceItem(
    val sectionId: String = "",
    val courseName: String = "",
    val totalClasses: Int = 0,
    val attendedClasses: Int = 0,
    val absentClasses: Int = 0,
    val tardyClasses: Int = 0,
    val maxAllowedAbsences: Int = 0,
    val attendancePercentage: Double = 100.0,
    val isAtRisk: Boolean = false
)

@Serializable
data class TecsupScheduleItem(
    val id: String = "",
    val sectionId: String = "",
    val courseName: String = "",
    val dayOfWeek: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val classroom: String = "",
    val building: String = "",
    val teacherName: String = "",
    val eventType: String = "Class"
)

@Serializable
data class TecsupDashboardSummary(
    val user: TecsupUser? = null,
    val currentPeriod: TecsupAcademicPeriod? = null,
    val todayClasses: List<TecsupScheduleItem> = emptyList(),
    val attendanceAlerts: List<TecsupAttendanceItem> = emptyList(),
    val coursesCount: Int = 0,
    val isSessionValid: Boolean = false
)
