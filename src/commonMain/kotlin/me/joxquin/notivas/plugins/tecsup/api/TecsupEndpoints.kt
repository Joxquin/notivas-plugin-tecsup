package me.joxquin.notivas.plugins.tecsup.api

object TecsupEndpoints {
    const val BASE_URL = "https://tecsup-api.u-planner.com/v3/"
    const val LOGIN_URL = "https://tecsup-api.u-planner.com/v3/api/auth/loginAzure?p=B2C_1A_IDP_GOOGLE_SELECTACCOUNT&returnTo=com.tecsup.uexperience://"
    const val REDIRECT_SCHEME = "com.tecsup.uexperience"
    const val REDIRECT_AUTH_HOST = "login_auth"

    // User & Auth
    const val USER_INFO = "api/user-api/user-info/v4"
    const val SUITE_TOKEN = "api/user-api/suite-token"

    // Periods & Grades
    const val ACADEMIC_PERIODS = "api/ux-api/academicPeriod/academic-periods/v2"
    fun gradeProgressStatus(academicPeriodId: String) = "api/ux-api/grade/$academicPeriodId/grade-progress-status/v2"
    fun sectionGradeDetail(sectionId: String) = "api/ux-api/grade/$sectionId/studentDetail/v2"

    // Attendance
    const val ATTENDANCE_SUMMARY = "api/ux-api/attendance/user-attendance/v4"
    fun sectionAttendanceSummary(sectionId: String) = "api/ux-api/attendance/sections/$sectionId/summary"
    fun sectionAttendanceUserDetail(sectionId: String, userId: String) = "api/ux-api/attendance/sections/$sectionId/users/$userId/detail"

    // Schedule & Classes
    const val SCHEDULE = "api/ux-api/schedule/v4"
    const val EVENTS_TODAY = "api/ux-api/events/current-day/v2"
    const val SECTIONS = "api/ux-api/sections/v2"
}
