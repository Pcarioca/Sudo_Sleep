package com.example.sudosleep.data.remote

import com.example.sudosleep.data.remote.dto.AcademicEventRequestDto
import com.example.sudosleep.data.remote.dto.AcademicEventResponseDto
import com.example.sudosleep.data.remote.dto.ActivityRequirementRequestDto
import com.example.sudosleep.data.remote.dto.ActivityRequirementResponseDto
import com.example.sudosleep.data.remote.dto.GradeRequestDto
import com.example.sudosleep.data.remote.dto.GradeResponseDto
import com.example.sudosleep.data.remote.dto.SemesterRequestDto
import com.example.sudosleep.data.remote.dto.SemesterResponseDto
import com.example.sudosleep.data.remote.dto.SubjectRequestDto
import com.example.sudosleep.data.remote.dto.SubjectResponseDto
import com.example.sudosleep.data.remote.dto.SubjectUpdateRequestDto
import com.example.sudosleep.data.remote.dto.TimetableEntryRequestDto
import com.example.sudosleep.data.remote.dto.TimetableEntryResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AcademicApi {

    // ---- Semesters ----
    @GET("api/semesters")
    suspend fun getMySemesters(): List<SemesterResponseDto>

    @POST("api/semesters")
    suspend fun createMySemester(@Body request: SemesterRequestDto): SemesterResponseDto

    // ---- Subjects ----
    @GET("api/semesters/{semesterId}/subjects")
    suspend fun getSubjects(@Path("semesterId") semesterId: Long): List<SubjectResponseDto>

    @POST("api/semesters/{semesterId}/subjects")
    suspend fun createSubject(
        @Path("semesterId") semesterId: Long,
        @Body request: SubjectRequestDto
    ): SubjectResponseDto

    @PUT("api/subjects/{id}")
    suspend fun updateSubject(
        @Path("id") id: Long,
        @Body request: SubjectUpdateRequestDto
    ): SubjectResponseDto

    @DELETE("api/subjects/{id}")
    suspend fun deleteSubject(@Path("id") id: Long): Response<Unit>

    // ---- Activity Requirements ----
    @POST("api/subjects/{subjectId}/requirements")
    suspend fun addRequirement(
        @Path("subjectId") subjectId: Long,
        @Body request: ActivityRequirementRequestDto
    ): ActivityRequirementResponseDto

    @DELETE("api/requirements/{id}")
    suspend fun deleteRequirement(@Path("id") id: Long): Response<Unit>

    // ---- Timetable & Per-Week Cancellations ----
    @GET("api/semesters/{semesterId}/timetable")
    suspend fun getTimetable(
        @Path("semesterId") semesterId: Long,
        @Query("week") week: Int? = null
    ): List<TimetableEntryResponseDto>

    @POST("api/requirements/{requirementId}/timetable-entries")
    suspend fun createTimetableEntry(
        @Path("requirementId") requirementId: Long,
        @Query("force") force: Boolean = true,
        @Body request: TimetableEntryRequestDto
    ): TimetableEntryResponseDto

    @PUT("api/timetable-entries/{id}")
    suspend fun updateTimetableEntry(
        @Path("id") id: Long,
        @Query("force") force: Boolean = true,
        @Body request: TimetableEntryRequestDto
    ): TimetableEntryResponseDto

    @DELETE("api/timetable-entries/{id}")
    suspend fun deleteTimetableEntry(@Path("id") id: Long): Response<Unit>

    @PUT("api/timetable-entries/{id}/cancellations/{isoYearWeek}")
    suspend fun cancelTimetableEntryForWeek(
        @Path("id") id: Long,
        @Path("isoYearWeek") isoYearWeek: Int
    ): TimetableEntryResponseDto

    @DELETE("api/timetable-entries/{id}/cancellations/{isoYearWeek}")
    suspend fun uncancelTimetableEntryForWeek(
        @Path("id") id: Long,
        @Path("isoYearWeek") isoYearWeek: Int
    ): TimetableEntryResponseDto

    // ---- Grades ----
    @GET("api/subjects/{subjectId}/grades")
    suspend fun getGrades(@Path("subjectId") subjectId: Long): List<GradeResponseDto>

    @POST("api/subjects/{subjectId}/grades")
    suspend fun addGrade(
        @Path("subjectId") subjectId: Long,
        @Body request: GradeRequestDto
    ): GradeResponseDto

    @DELETE("api/grades/{id}")
    suspend fun deleteGrade(@Path("id") id: Long): Response<Unit>

    // ---- Academic Events (Exams, Midterms, Deadlines) ----
    @GET("api/semesters/{semesterId}/events")
    suspend fun getEvents(@Path("semesterId") semesterId: Long): List<AcademicEventResponseDto>

    @POST("api/semesters/{semesterId}/events")
    suspend fun createEvent(
        @Path("semesterId") semesterId: Long,
        @Body request: AcademicEventRequestDto
    ): AcademicEventResponseDto

    @PUT("api/events/{id}")
    suspend fun updateEvent(
        @Path("id") id: Long,
        @Body request: AcademicEventRequestDto
    ): AcademicEventResponseDto

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long): Response<Unit>
}
