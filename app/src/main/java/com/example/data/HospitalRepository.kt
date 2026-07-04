package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class HospitalRepository(private val dao: HospitalDao) {

    // Flows
    val allDoctors: Flow<List<DoctorEntity>> = dao.getAllDoctors()
    val allAppointments: Flow<List<AppointmentEntity>> = dao.getAllAppointments()
    val allReports: Flow<List<MedicalReportEntity>> = dao.getAllReports()
    val allMessages: Flow<List<MessageEntity>> = dao.getAllMessages()
    val allPrescriptions: Flow<List<PrescriptionEntity>> = dao.getAllPrescriptions()
    val allBilling: Flow<List<BillingEntity>> = dao.getAllBilling()
    val allResources: Flow<List<FacilityResourceEntity>> = dao.getAllResources()

    // Message conversations
    fun getMessagesBetween(pId: String, dId: String): Flow<List<MessageEntity>> {
        return dao.getMessagesBetween(pId, dId)
    }

    // Insert / updates
    suspend fun insertDoctor(doctor: DoctorEntity) = dao.insertDoctor(doctor)
    
    suspend fun insertAppointment(appointment: AppointmentEntity) = dao.insertAppointment(appointment)
    suspend fun updateAppointment(appointment: AppointmentEntity) = dao.updateAppointment(appointment)
    suspend fun deleteAppointmentById(id: Int) = dao.deleteAppointment(id)

    suspend fun insertReport(report: MedicalReportEntity) = dao.insertReport(report)
    suspend fun updateReport(report: MedicalReportEntity) = dao.updateReport(report)

    suspend fun insertMessage(message: MessageEntity) = dao.insertMessage(message)

    suspend fun insertPrescription(prescription: PrescriptionEntity) = dao.insertPrescription(prescription)

    suspend fun insertBilling(billing: BillingEntity) = dao.insertBilling(billing)
    suspend fun updateBilling(billing: BillingEntity) = dao.updateBilling(billing)

    suspend fun insertResource(resource: FacilityResourceEntity) = dao.insertResource(resource)
    suspend fun updateResource(resource: FacilityResourceEntity) = dao.updateResource(resource)

    // Seeding default data if empty
    suspend fun initializeDefaultDataIfNeeded() {
        val doctors = allDoctors.first()
        if (doctors.isEmpty()) {
            // Seed Doctors
            val defaultDoctors = listOf(
                DoctorEntity(
                    id = "dr_shailesh_mehta",
                    name = "Dr. Shailesh Mehta",
                    speciality = "Cardiologist",
                    experience = "15+ Years",
                    rating = 4.9f,
                    availability = "Mon - Fri, 9:00 AM - 1:00 PM",
                    bio = "Senior Interventional Cardiologist. Expert in coronary angioplasty, heart failure management, and vascular interventions. Formerly at AIIMS.",
                    languages = "English, Hindi, Gujarati"
                ),
                DoctorEntity(
                    id = "dr_ananya_sharma",
                    name = "Dr. Ananya Sharma",
                    speciality = "Pediatrician",
                    experience = "10+ Years",
                    rating = 4.8f,
                    availability = "Mon - Sat, 2:00 PM - 6:00 PM",
                    bio = "Dedicated Pediatrician specializing in neonatology, child immunization, developmental tracking, and adolescent healthcare.",
                    languages = "English, Hindi, Punjabi"
                ),
                DoctorEntity(
                    id = "dr_rajesh_khanna",
                    name = "Dr. Rajesh Khanna",
                    speciality = "Orthopedic Surgeon",
                    experience = "12+ Years",
                    rating = 4.7f,
                    availability = "Tue & Thu, 10:00 AM - 3:00 PM",
                    bio = "Specialist in Joint Replacements, Knee Arthroscopy, Complex Trauma, and Spine Care. Dedicated to restoring full mobility.",
                    languages = "English, Hindi"
                ),
                DoctorEntity(
                    id = "dr_meera_nair",
                    name = "Dr. Meera Nair",
                    speciality = "Obstetrician & Gynecologist",
                    experience = "14+ Years",
                    rating = 4.9f,
                    availability = "Mon - Fri, 3:00 PM - 7:00 PM",
                    bio = "Expert in high-risk pregnancy management, fetal medicine, laparoscopic gynecological surgeries, and women's health counseling.",
                    languages = "English, Hindi, Malayalam, Tamil"
                ),
                DoctorEntity(
                    id = "dr_vikram_patel",
                    name = "Dr. Vikram Patel",
                    speciality = "Neurologist",
                    experience = "18+ Years",
                    rating = 5.0f,
                    availability = "Wed & Fri, 11:00 AM - 4:00 PM",
                    bio = "Renowned Neurologist specializing in stroke therapy, epilepsy treatment, Parkinson's disease, and neurodegenerative disorders.",
                    languages = "English, Hindi, Gujarati, Marathi"
                )
            )
            dao.insertDoctors(defaultDoctors)

            // Seed Facility Resources
            val defaultResources = listOf(
                FacilityResourceEntity("general_beds", "General Ward Beds", 120, 85),
                FacilityResourceEntity("icu_beds", "Intensive Care Unit (ICU) Beds", 30, 18),
                FacilityResourceEntity("ventilators", "Advanced Ventilator Units", 15, 9),
                FacilityResourceEntity("oxygen", "High-Flow Oxygen Cylinders", 200, 140)
            )
            dao.insertResources(defaultResources)

            // Seed Medical Reports
            val defaultReports = listOf(
                MedicalReportEntity(
                    patientName = "Patient (You)",
                    reportName = "Comprehensive Metabolic Panel (CMP)",
                    reportDate = "2026-06-18",
                    doctorName = "Dr. Ananya Sharma",
                    department = "Pathology",
                    summary = "Standard metabolic blood chemistry screening showing normal organ and glucose metabolism values.",
                    keyMetrics = "Hemoglobin:14.2 g/dL (Normal),Fasting Glucose:94 mg/dL (Normal),Urea Nitrogen (BUN):15 mg/dL (Normal),Creatinine:0.9 mg/dL (Normal),Serum Sodium:140 mEq/L (Normal)",
                    doctorNotes = "Excellent metabolic readings. Patient should maintain regular hydration and low sodium intake. Schedule routine tracking in 1 year.",
                    isSecureCloudBackedUp = true
                ),
                MedicalReportEntity(
                    patientName = "Patient (You)",
                    reportName = "Cardiac Stress Electrocardiogram (ECG)",
                    reportDate = "2026-06-25",
                    doctorName = "Dr. Shailesh Mehta",
                    department = "Cardiology",
                    summary = "Treadmill stress test with active ECG, BP, and heart rate monitoring to verify myocardial perfusion.",
                    keyMetrics = "Resting Heart Rate:68 bpm,Peak Heart Rate:145 bpm,BP Peak Response:135/82 mmHg,ST Segment:Unremarkable,Exercise Duration:12 mins",
                    doctorNotes = "Excellent exercise tolerance. No evidence of ischemia or exercise-induced arrhythmia. Fit for aerobic physical training.",
                    isSecureCloudBackedUp = true
                ),
                MedicalReportEntity(
                    patientName = "Patient (You)",
                    reportName = "Lumbar Spine MRI (Non-Contrast)",
                    reportDate = "2026-07-02",
                    doctorName = "Dr. Vikram Patel",
                    department = "Radiology & Neurology",
                    summary = "High-resolution magnetic resonance imaging of the lumbar-sacral spinal column.",
                    keyMetrics = "L1-L2 Disc:Intact,L3-L4 Disc:Mild Degeneration,L4-L5 Disc:Normal Height,Spinal Canal:Normal dimensions,Nerve Roots:Clear of impingement",
                    doctorNotes = "Minor age-appropriate dehydration of the L3-L4 disc without herniation or stenosis. Recommend core strengthening exercises and avoiding heavy lifting.",
                    isSecureCloudBackedUp = false
                )
            )
            dao.insertReports(defaultReports)

            // Seed Prescriptions
            val defaultPrescriptions = listOf(
                PrescriptionEntity(
                    patientName = "Patient (You)",
                    doctorName = "Dr. Shailesh Mehta",
                    diagnosis = "Primary Hypertension Risk & Hyperlipidemia Tracker",
                    date = "2026-06-25",
                    medicationList = "Atorvastatin (10mg) - 1 Tablet - Once daily at night\nMetoprolol Succinate (25mg) - 1 Tablet - Once daily after breakfast",
                    instructions = "Avoid high saturated fats and excessive salt. Monitor home blood pressure twice weekly. Refill prescription in 60 days."
                ),
                PrescriptionEntity(
                    patientName = "Patient (You)",
                    doctorName = "Dr. Vikram Patel",
                    diagnosis = "Mild Musculoskeletal Lumbar Back Ache",
                    date = "2026-07-02",
                    medicationList = "Methylcobalamin (1500mcg) - 1 Capsule - Once daily for nerve health\nNaproxen (250mg) - 1 Tablet - Twice daily as needed for pain",
                    instructions = "Apply warm fomentation over back. Start core rehabilitation physiotherapy. Avoid prolonged sitting."
                )
            )
            dao.insertPrescriptions(defaultPrescriptions)

            // Seed Billing Invoices
            val defaultBillings = listOf(
                BillingEntity(
                    invoiceNumber = "INV-2026-0105",
                    patientName = "Patient (You)",
                    description = "Specialist Consultation Fee (Cardiology) - Dr. Shailesh Mehta",
                    amount = 120.00,
                    date = "2026-06-25",
                    status = "Paid",
                    transactionId = "TXN-SH-9182301"
                ),
                BillingEntity(
                    invoiceNumber = "INV-2026-0112",
                    patientName = "Patient (You)",
                    description = "Advanced Lumbar Spine MRI Scan (Diagnostic Imaging)",
                    amount = 350.00,
                    date = "2026-07-02",
                    status = "Paid",
                    transactionId = "TXN-SH-9204851"
                ),
                BillingEntity(
                    invoiceNumber = "INV-2026-0155",
                    patientName = "Patient (You)",
                    description = "Pathology Lab Blood Assay & Metabolic Panel",
                    amount = 85.00,
                    date = "2026-06-18",
                    status = "Paid",
                    transactionId = "TXN-SH-9143321"
                ),
                BillingEntity(
                    invoiceNumber = "INV-2026-0210",
                    patientName = "Patient (You)",
                    description = "Follow-up Neurology Outpatient Consultation - Dr. Vikram Patel",
                    amount = 100.00,
                    date = "2026-07-04",
                    status = "Unpaid"
                )
            )
            dao.insertBillings(defaultBillings)
        }
    }
}
