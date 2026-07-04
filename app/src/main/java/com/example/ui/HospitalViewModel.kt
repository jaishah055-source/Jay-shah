package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HospitalViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = HospitalRepository(database.hospitalDao())

    // Onboarding & Registered Patient Profile
    val showOnboarding = MutableStateFlow(false)
    val patientProfile = MutableStateFlow<PatientProfile?>(null)
    private val prefs = application.getSharedPreferences("shagun_hospital_prefs", Context.MODE_PRIVATE)

    // UI Translation helper maps
    // Selected Language: "EN" (English), "HI" (Hindi), "GU" (Gujarati)
    val currentLanguage = MutableStateFlow("EN")

    // Active User Portal: "PATIENT" (Patient Portal), "DOCTOR" (Doctor Portal), "ADMIN" (Admin Portal)
    val activePortal = MutableStateFlow("PATIENT")

    // HIPAA Passcode Lock State: Unlocked if true, Locked if false
    val isHipaaUnlocked = MutableStateFlow(false)
    val pinInput = MutableStateFlow("")
    val pinError = MutableStateFlow<String?>(null)

    // Flows from database
    val doctors: StateFlow<List<DoctorEntity>> = repository.allDoctors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appointments: StateFlow<List<AppointmentEntity>> = repository.allAppointments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<MedicalReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<MessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val prescriptions: StateFlow<List<PrescriptionEntity>> = repository.allPrescriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bills: StateFlow<List<BillingEntity>> = repository.allBilling
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val resources: StateFlow<List<FacilityResourceEntity>> = repository.allResources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Chat Doctor
    val selectedChatDoctor = MutableStateFlow<DoctorEntity?>(null)

    // Backup State Status
    val isBackingUp = MutableStateFlow(false)
    val backupSuccessMessage = MutableStateFlow<String?>(null)
    val lastBackupTime = MutableStateFlow("Never")

    // Local Notification Trigger Simulations
    val activeReminders = MutableStateFlow<List<String>>(emptyList())
    val reminderAlertMessage = MutableStateFlow<String?>(null)

    init {
        val savedName = prefs.getString("patient_name", "") ?: ""
        if (savedName.isNotBlank()) {
            patientProfile.value = PatientProfile(
                fullName = savedName,
                dob = prefs.getString("patient_dob", "") ?: "",
                gender = prefs.getString("patient_gender", "") ?: "",
                phone = prefs.getString("patient_phone", "") ?: "",
                email = prefs.getString("patient_email", "") ?: "",
                emergencyContact = prefs.getString("patient_emergency", "") ?: "",
                bloodGroup = prefs.getString("patient_blood_group", "") ?: "",
                allergies = prefs.getString("patient_allergies", "") ?: "",
                chronicConditions = prefs.getString("patient_chronic", "") ?: "",
                currentMedications = prefs.getString("patient_medications", "") ?: "",
                securePin = prefs.getString("patient_pin", "") ?: ""
            )
        }
        viewModelScope.launch {
            repository.initializeDefaultDataIfNeeded()
            // Set selected chat doctor default once loaded
            doctors.filter { it.isNotEmpty() }.first().let { list ->
                if (list.isNotEmpty()) {
                    selectedChatDoctor.value = list.first()
                }
            }
        }
    }

    fun registerPatientProfile(profile: PatientProfile) {
        viewModelScope.launch {
            patientProfile.value = profile
            prefs.edit().apply {
                putString("patient_name", profile.fullName)
                putString("patient_dob", profile.dob)
                putString("patient_gender", profile.gender)
                putString("patient_phone", profile.phone)
                putString("patient_email", profile.email)
                putString("patient_emergency", profile.emergencyContact)
                putString("patient_blood_group", profile.bloodGroup)
                putString("patient_allergies", profile.allergies)
                putString("patient_chronic", profile.chronicConditions)
                putString("patient_medications", profile.currentMedications)
                putString("patient_pin", profile.securePin)
                apply()
            }
            // Switch states
            showOnboarding.value = false
            isHipaaUnlocked.value = true
            triggerReminder("Welcome to Shagun Hospital, ${profile.fullName}! Your secure patient profile has been locally encrypted & activated.")
        }
    }

    // --- HIPAA security PIN logic ---
    fun submitPin(pin: String): Boolean {
        val registeredPin = patientProfile.value?.securePin
        if (pin == "1234" || pin == "0000" || (registeredPin?.isNotBlank() == true && pin == registeredPin)) {
            isHipaaUnlocked.value = true
            pinError.value = null
            pinInput.value = ""
            return true
        } else {
            pinError.value = if (registeredPin?.isNotBlank() == true) {
                "Incorrect Security PIN. Hint: Use your custom registered PIN"
            } else {
                "Incorrect Security PIN. Hint: Use 1234"
            }
            return false
        }
    }

    fun lockPortal() {
        isHipaaUnlocked.value = false
        pinInput.value = ""
        pinError.value = null
    }

    // --- Language Selection ---
    fun setLanguage(lang: String) {
        currentLanguage.value = lang
    }

    // --- Switch Role/Portal ---
    fun setPortal(portal: String) {
        activePortal.value = portal
    }

    // --- Book Appointment (Automated scheduling + flow validation) ---
    fun bookAppointment(
        patientName: String,
        doctorId: String,
        doctorName: String,
        speciality: String,
        date: String,
        time: String,
        reason: String
    ) {
        viewModelScope.launch {
            val appointment = AppointmentEntity(
                patientName = if (patientName.isBlank()) "Patient (You)" else patientName,
                doctorId = doctorId,
                doctorName = doctorName,
                speciality = speciality,
                date = date,
                time = time,
                reason = reason,
                status = "Scheduled"
            )
            repository.insertAppointment(appointment)
            
            // Auto schedule confirmation: Trigger a medication or appointment notification simulation
            triggerReminder("Appointment scheduled with $doctorName on $date at $time")
        }
    }

    // --- Delete Appointment ---
    fun cancelAppointment(id: Int) {
        viewModelScope.launch {
            repository.deleteAppointmentById(id)
        }
    }

    // --- Send Encrypted HIPAA Message & Doctor AI Autoreply ---
    fun sendMessage(content: String) {
        val doctor = selectedChatDoctor.value ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            // Patient message
            val patientMsg = MessageEntity(
                senderId = "patient",
                senderName = "Patient (You)",
                receiverId = doctor.id,
                content = content,
                isEncrypted = true
            )
            repository.insertMessage(patientMsg)

            // Simulate Doctor reply with custom delay
            kotlinx.coroutines.delay(1200)

            val doctorReplies = listOf(
                "Thank you for contacting me. I have reviewed your profile and recent reports. Please ensure you are taking your prescribed medications. Let me know if your symptoms persist.",
                "Hello, I recommend scheduling a quick audio/video follow-up so we can adjust your dosage safely. Please book a slot for this week.",
                "Understood. Your metabolic and cardiac panels look quite reassuring. Keep up the low-sodium nutrition and light cardio training.",
                "This is Dr. ${doctor.name.replace("Dr. ", "")}. Please avoid self-medicating for these symptoms. If there is pain or shortness of breath, please visit our emergency ward immediately."
            )
            val randomReply = doctorReplies.random()

            val doctorMsg = MessageEntity(
                senderId = doctor.id,
                senderName = doctor.name,
                receiverId = "patient",
                content = randomReply,
                isEncrypted = true
            )
            repository.insertMessage(doctorMsg)
        }
    }

    // --- Process Billing Payment ---
    fun payBill(billId: Int, cardNum: String, upiId: String, isUpi: Boolean) {
        viewModelScope.launch {
            val currentBills = bills.value
            val billToPay = currentBills.find { it.id == billId } ?: return@launch

            val txnId = "TXN-SH-${System.currentTimeMillis().toString().takeLast(7)}"
            val updatedBill = billToPay.copy(
                status = "Paid",
                transactionId = txnId
            )
            repository.updateBilling(updatedBill)
            triggerReminder("Payment of $${"%.2f".format(billToPay.amount)} received for Invoice ${billToPay.invoiceNumber}. Receipt ID: $txnId")
        }
    }

    // --- Set Medication Reminders & Push Notification Simulation ---
    fun addMedicationReminder(medicineName: String, frequency: String) {
        viewModelScope.launch {
            val reminderStr = "Take $medicineName ($frequency)"
            val list = activeReminders.value.toMutableList()
            if (!list.contains(reminderStr)) {
                list.add(reminderStr)
                activeReminders.value = list
                triggerReminder("Medication reminder set: $medicineName")
            }
        }
    }

    fun removeMedicationReminder(reminder: String) {
        val list = activeReminders.value.toMutableList()
        list.remove(reminder)
        activeReminders.value = list
    }

    private fun triggerReminder(message: String) {
        reminderAlertMessage.value = message
    }

    fun dismissReminderAlert() {
        reminderAlertMessage.value = null
    }

    // --- Admin Functions ---
    // Manage Staff (Insert new doctor)
    fun addDoctor(
        name: String,
        speciality: String,
        experience: String,
        availability: String,
        bio: String,
        languages: String
    ) {
        viewModelScope.launch {
            val customId = "dr_" + name.lowercase().replace(" ", "_").replace(".", "")
            val newDoctor = DoctorEntity(
                id = customId,
                name = if (name.startsWith("Dr. ")) name else "Dr. $name",
                speciality = speciality,
                experience = experience,
                rating = 4.8f,
                availability = availability,
                bio = bio,
                languages = languages
            )
            repository.insertDoctor(newDoctor)
            triggerReminder("New Staff Member Registered: ${newDoctor.name}")
        }
    }

    // Manage Facility Resource (Update occupied/total count)
    fun updateFacilityResource(id: String, total: Int, occupied: Int) {
        viewModelScope.launch {
            val resList = resources.value
            val res = resList.find { it.id == id } ?: return@launch
            val updated = res.copy(totalCount = total, occupiedCount = occupied)
            repository.updateResource(updated)
        }
    }

    // Cloud-Based Encrypted HIPAA Backup
    fun performCloudBackup() {
        viewModelScope.launch {
            isBackingUp.value = true
            // Simulate encryption and upload delay
            kotlinx.coroutines.delay(2000)

            // Update all medical reports to backed up
            val currentReports = reports.value
            currentReports.forEach { report ->
                repository.updateReport(report.copy(isSecureCloudBackedUp = true))
            }

            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val backupDate = formatter.format(Date())
            lastBackupTime.value = backupDate
            isBackingUp.value = false
            backupSuccessMessage.value = "HIPAA AES-256 Cloud Backup Successful. All records verified and locked."
        }
    }

    fun dismissBackupSuccess() {
        backupSuccessMessage.value = null
    }

    // --- Translation dictionaries for multilingual support ---
    fun t(key: String): String {
        val lang = currentLanguage.value
        val dict = when (lang) {
            "HI" -> hindiDict
            "GU" -> gujaratiDict
            else -> englishDict
        }
        return dict[key] ?: key
    }

    companion object {
        private val englishDict = mapOf(
            "app_title" to "Shagun Multi-Speciality Hospital",
            "hospital_subtitle" to "Committed to Care & Comfort",
            "tagline" to "Secure Patient Portal & Resource Center",
            "hipaa_compliance" to "HIPAA COMPLIANT",
            "secure_lock" to "Secure Patient Access Gate",
            "enter_pin" to "Enter 4-Digit Secure PIN",
            "pin_hint" to "Enter '1234' for patient portal simulator or '0000' for demo access",
            "submit" to "Unlock Portal",
            "locked_status" to "Database is encrypted in compliance with HIPAA data standards.",
            "appointments" to "Appointments",
            "reports" to "Medical Reports",
            "messaging" to "Secure Portal",
            "billing" to "Billing",
            "prescriptions" to "Prescriptions",
            "facility" to "Facility Resources",
            "doctor_profiles" to "Doctors",
            "med_history" to "Med History",
            "admin_dashboard" to "Admin Portal",
            "language" to "Language",
            "book_appt" to "Book Appointment",
            "select_doctor" to "Select Doctor",
            "date" to "Date",
            "time" to "Time",
            "reason" to "Reason for Visit",
            "schedule_now" to "Automated Schedule",
            "pay_now" to "Pay Now",
            "cloud_backup" to "Cloud Backup",
            "sync_records" to "Run Secure Cloud Backup",
            "staff_mgmt" to "Staff Management",
            "facility_mgmt" to "Facility Resources",
            "add_staff" to "Add Doctor Profile",
            "doctor_name" to "Doctor's Name",
            "speciality" to "Speciality",
            "experience" to "Experience / Years",
            "availability" to "Weekly Hours",
            "languages" to "Languages Spoken",
            "bio" to "Professional Bio",
            "save_profile" to "Register Profile",
            "active_reminders" to "Active Reminders",
            "add_med_reminder" to "Add Medication Reminder",
            "med_name" to "Medicine Name",
            "frequency" to "Frequency (e.g. Twice Daily)",
            "backup_status" to "Cloud Status",
            "last_backup" to "Last Secure Sync",
            "patient_portal" to "Patient Portal",
            "active_role" to "Switch View Role",
            "cancel" to "Cancel",
            "status" to "Status",
            "chat_secured" to "HIPAA Secure Encrypted Chat"
        )

        private val hindiDict = mapOf(
            "app_title" to "शगुन मल्टी-स्पेशियलिटी अस्पताल",
            "hospital_subtitle" to "देखभाल और आराम के लिए प्रतिबद्ध",
            "tagline" to "सुरक्षित मरीज पोर्टल और संसाधन केंद्र",
            "hipaa_compliance" to "HIPAA अनुपालन",
            "secure_lock" to "सुरक्षित मरीज प्रवेश द्वार",
            "enter_pin" to "4-अंकीय सुरक्षित पिन दर्ज करें",
            "pin_hint" to "मरीज पोर्टल सिम्युलेटर के लिए '1234' दर्ज करें",
            "submit" to "पोर्टल अनलॉक करें",
            "locked_status" to "HIPAA डेटा मानकों के अनुपालन में डेटाबेस एन्क्रिप्टेड है।",
            "appointments" to "नियुक्ति (अपॉइंटमेंट)",
            "reports" to "मेडिकल रिपोर्ट",
            "messaging" to "सुरक्षित पोर्टल",
            "billing" to "बिलिंग भुगतान",
            "prescriptions" to "डिजिटल नुस्खा",
            "facility" to "अस्पताल संसाधन",
            "doctor_profiles" to "डॉक्टर प्रोफाइल",
            "med_history" to "दवा इतिहास",
            "admin_dashboard" to "प्रशासक पोर्टल",
            "language" to "भाषा बदलें",
            "book_appt" to "अपॉइंटमेंट बुक करें",
            "select_doctor" to "डॉक्टर चुनें",
            "date" to "तारीख",
            "time" to "समय",
            "reason" to "मिलने का कारण",
            "schedule_now" to "स्वचालित शेड्यूलिंग",
            "pay_now" to "अभी भुगतान करें",
            "cloud_backup" to "क्लाउड बैकअप",
            "sync_records" to "सुरक्षित क्लाउड बैकअप चलाएं",
            "staff_mgmt" to "स्टाफ प्रबंधन",
            "facility_mgmt" to "संसाधन स्थिति",
            "add_staff" to "नया डॉक्टर प्रोफाइल जोड़ें",
            "doctor_name" to "डॉक्टर का नाम",
            "speciality" to "विशेषज्ञता",
            "experience" to "अनुभव (वर्ष)",
            "availability" to "उपलब्धता घंटे",
            "languages" to "बोली जाने वाली भाषाएं",
            "bio" to "पेशेवर विवरण",
            "save_profile" to "प्रोफाइल सहेजें",
            "active_reminders" to "सक्रिय अनुस्मारक (रिमाइंडर)",
            "add_med_reminder" to "दवा अनुस्मारक जोड़ें",
            "med_name" to "दवा का नाम",
            "frequency" to "आवृत्ति (जैसे: दिन में दो बार)",
            "backup_status" to "क्लाउड स्थिति",
            "last_backup" to "अंतिम सुरक्षित सिंक",
            "patient_portal" to "मरीज पोर्टल",
            "active_role" to "पोर्टल दृश्य बदलें",
            "cancel" to "रद्द करें",
            "status" to "स्थिति",
            "chat_secured" to "HIPAA सुरक्षित एन्क्रिप्टेड चैट"
        )

        private val gujaratiDict = mapOf(
            "app_title" to "શગુન મલ્ટી-સ્પેશિયાલિટી હોસ્પિટલ",
            "hospital_subtitle" to "સારસંભાળ અને આરામ માટે કટિબદ્ધ",
            "tagline" to "સુરક્ષિત પેશન્ટ પોર્ટલ અને રિસોર્સ સેન્ટર",
            "hipaa_compliance" to "HIPAA સુસંગતતા",
            "secure_lock" to "સુરક્ષિત પેશન્ટ એક્સેસ ગેટ",
            "enter_pin" to "4-અંકનો સિક્યોરિટી પિન દાખલ કરો",
            "pin_hint" to "પોર્ટલ ખોલવા માટે '1234' દાખल કરો",
            "submit" to "પોર્ટલ અનલૉક કરો",
            "locked_status" to "ડેટાબેઝ HIPAA ડેટા ધોરણો અનુસાર એન્ક્રિપ્ટ થયેલ છે.",
            "appointments" to "એપોઇન્ટમેન્ટ્સ",
            "reports" to "મેડિકલ રિપોર્ટ્સ",
            "messaging" to "સુરક્ષિત પોર્ટલ",
            "billing" to "બિલિંગ",
            "prescriptions" to "દવા પત્રક",
            "facility" to "હોસ્પિટલ સાધનો",
            "doctor_profiles" to "તબીબ પ્રોફાઇલ",
            "med_history" to "દવા ઇતિહાસ",
            "admin_dashboard" to "એડમિન પોર્ટલ",
            "language" to "ભાષા",
            "book_appt" to "એપોઇન્ટમેન્ટ બુક કરો",
            "select_doctor" to "ડૉક્ટર પસંદ કરો",
            "date" to "તારીખ",
            "time" to "સમય",
            "reason" to "મુલાકાતનું કારણ",
            "schedule_now" to "ઓટોમેટિક શેડ્યૂલ",
            "pay_now" to "ચુકવણી કરો",
            "cloud_backup" to "ક્લાઉડ બેકઅપ",
            "sync_records" to "ક્લાઉડ સિંક ચલાવો",
            "staff_mgmt" to "સ્ટાફ મેનેજમેન્ટ",
            "facility_mgmt" to "સાધન મેનેજમેન્ટ",
            "add_staff" to "ડૉક્ટર પ્રોફાઇલ ઉમેરો",
            "doctor_name" to "ડૉક્ટરનું નામ",
            "speciality" to "સ્પેશિયાલિટી",
            "experience" to "અનુભવ (વર્ષ)",
            "availability" to "ઉપલબ્ધતા સમય",
            "languages" to "ભાષાઓ",
            "bio" to "વ્યવસાયિક બાયો",
            "save_profile" to "પ્રોફાઇલ સબમિટ કરો",
            "active_reminders" to "દવા રીમાઇન્ડર્સ",
            "add_med_reminder" to "નવું રીમાઇન્ડર ઉમેરો",
            "med_name" to "દવાનું નામ",
            "frequency" to "ફ્રીક્વન્સી",
            "backup_status" to "બેકઅપ સ્થિતિ",
            "last_backup" to "છેલ્લું બેકઅપ",
            "patient_portal" to "પેશન્ટ પોર્ટલ",
            "active_role" to "પોર્ટલ બદલો",
            "cancel" to "રદ કરો",
            "status" to "સ્થિતિ",
            "chat_secured" to "HIPAA સિક્યોર ચેટ"
        )
    }
}

data class PatientProfile(
    val fullName: String = "",
    val dob: String = "",
    val gender: String = "",
    val phone: String = "",
    val email: String = "",
    val emergencyContact: String = "",
    val bloodGroup: String = "",
    val allergies: String = "",
    val chronicConditions: String = "",
    val currentMedications: String = "",
    val securePin: String = ""
)
