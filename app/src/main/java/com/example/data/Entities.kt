package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val speciality: String,
    val experience: String,
    val rating: Float,
    val availability: String,
    val bio: String,
    val languages: String = "English, Hindi"
)

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientName: String,
    val doctorId: String,
    val doctorName: String,
    val speciality: String,
    val date: String,
    val time: String,
    val reason: String,
    val status: String = "Scheduled", // Scheduled, Completed, Cancelled
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reports")
data class MedicalReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientName: String,
    val reportName: String,
    val reportDate: String,
    val doctorName: String,
    val department: String,
    val summary: String,
    val keyMetrics: String, // comma separated key:value pairs or JSON
    val doctorNotes: String,
    val isSecureCloudBackedUp: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String, // "patient" or doctorId
    val senderName: String,
    val receiverId: String, // doctorId or "patient"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = true // Always true for HIPAA
)

@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientName: String,
    val doctorName: String,
    val diagnosis: String,
    val date: String,
    val medicationList: String, // Comma or newline separated list
    val instructions: String
)

@Entity(tableName = "billing")
data class BillingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val patientName: String,
    val description: String,
    val amount: Double,
    val date: String,
    val status: String, // Unpaid, Paid
    val transactionId: String = ""
)

@Entity(tableName = "resources")
data class FacilityResourceEntity(
    @PrimaryKey val id: String, // "general_beds", "icu_beds", "ventilators", "oxygen"
    val resourceName: String,
    val totalCount: Int,
    val occupiedCount: Int
)
