package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HospitalDao {
    // Doctors
    @Query("SELECT * FROM doctors")
    fun getAllDoctors(): Flow<List<DoctorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: DoctorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctors(doctors: List<DoctorEntity>)

    // Appointments
    @Query("SELECT * FROM appointments ORDER BY timestamp DESC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointment(id: Int)

    // Medical Reports
    @Query("SELECT * FROM reports ORDER BY id DESC")
    fun getAllReports(): Flow<List<MedicalReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: MedicalReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<MedicalReportEntity>)

    @Update
    suspend fun updateReport(report: MedicalReportEntity)

    // Messaging
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE (senderId = :pId AND receiverId = :dId) OR (senderId = :dId AND receiverId = :pId) ORDER BY timestamp ASC")
    fun getMessagesBetween(pId: String, dId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    // Prescriptions
    @Query("SELECT * FROM prescriptions ORDER BY id DESC")
    fun getAllPrescriptions(): Flow<List<PrescriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescriptions(prescriptions: List<PrescriptionEntity>)

    // Billing
    @Query("SELECT * FROM billing ORDER BY id DESC")
    fun getAllBilling(): Flow<List<BillingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBilling(billing: BillingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillings(billings: List<BillingEntity>)

    @Update
    suspend fun updateBilling(billing: BillingEntity)

    // Facility Resources
    @Query("SELECT * FROM resources")
    fun getAllResources(): Flow<List<FacilityResourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: FacilityResourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResources(resources: List<FacilityResourceEntity>)

    @Update
    suspend fun updateResource(resource: FacilityResourceEntity)
}
