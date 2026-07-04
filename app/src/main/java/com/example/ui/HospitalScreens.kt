package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShagunHospitalApp(viewModel: HospitalViewModel) {
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isLocked by viewModel.isHipaaUnlocked.collectAsState()
    val alertMsg by viewModel.reminderAlertMessage.collectAsState()
    val backupMsg by viewModel.backupSuccessMessage.collectAsState()

    // Main Scaffold with central states
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.shagun_logo_icon),
                            contentDescription = "Hospital Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SHAGUN MULTI-SPECIALITY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = if (!isLocked) "Secure Gateway" else viewModel.t("app_title"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    // Quick language switcher
                    LanguageSwitcher(
                        selected = currentLang,
                        onSelected = { viewModel.setLanguage(it) }
                    )
                    if (isLocked) {
                        IconButton(
                            onClick = { viewModel.lockPortal() },
                            modifier = Modifier.testTag("lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Lock HIPAA session",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isLocked) {
                val showOnboarding by viewModel.showOnboarding.collectAsState()
                if (showOnboarding) {
                    PatientOnboardingScreen(viewModel)
                } else {
                    // Secure Gateway passcode check
                    SecureGatewayScreen(viewModel)
                }
            } else {
                // Unlocked app interface
                MainPortalContainer(viewModel)
            }

            // In-App Alert Overlay Simulation (Push Notification Alerts)
            alertMsg?.let { msg ->
                InAppNotificationToast(
                    message = msg,
                    onDismiss = { viewModel.dismissReminderAlert() }
                )
            }

            // Backup Sync complete overlay
            backupMsg?.let { msg ->
                BackupCompletedDialog(
                    message = msg,
                    onDismiss = { viewModel.dismissBackupSuccess() }
                )
            }
        }
    }
}

// Language Switcher Dropdown Menu
@Composable
fun LanguageSwitcher(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier
                .height(36.dp)
                .padding(end = 8.dp)
                .testTag("lang_selector_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.Language, contentDescription = "Languages", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = selected, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("English (EN)", fontSize = 14.sp) },
                onClick = { onSelected("EN"); expanded = false },
                modifier = Modifier.testTag("lang_en")
            )
            DropdownMenuItem(
                text = { Text("हिंदी (HI)", fontSize = 14.sp) },
                onClick = { onSelected("HI"); expanded = false },
                modifier = Modifier.testTag("lang_hi")
            )
            DropdownMenuItem(
                text = { Text("ગુજરાતી (GU)", fontSize = 14.sp) },
                onClick = { onSelected("GU"); expanded = false },
                modifier = Modifier.testTag("lang_gu")
            )
        }
    }
}

// Secure HIPAA Lock Gateway Screen
@Composable
fun SecureGatewayScreen(viewModel: HospitalViewModel) {
    var code by remember { mutableStateOf("") }
    val error by viewModel.pinError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App logo or clinical icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.shagun_logo_icon),
                contentDescription = "Shagun Hospital",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SHAGUN MULTI-SPECIALITY HOSPITAL",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = viewModel.t("tagline"),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(30.dp))

        // HIPAA Compliance Badge
        Row(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "HIPAA Seal",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = viewModel.t("hipaa_compliance") + " - SECURED ACCESS",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(0.95f),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = viewModel.t("secure_lock"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 4) code = it },
                    label = { Text(viewModel.t("enter_pin")) },
                    placeholder = { Text("••••") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_field"),
                    isError = error != null,
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) }
                )

                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .testTag("pin_error")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        viewModel.submitPin(code)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pin_submit"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(viewModel.t("submit"), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { viewModel.showOnboarding.value = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboard_new_patient_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Patient? Onboard & Register", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = viewModel.t("pin_hint"),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = viewModel.t("locked_status"),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
        )
    }
}

// In-App Alert Toast Simulation
@Composable
fun InAppNotificationToast(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 60.dp)
                .clickable { onDismiss() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Alert",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Medication / Appt Reminder",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// Backup Successful dialog
@Composable
fun BackupCompletedDialog(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Cloud backup success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Backup Succeeded",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK, Close")
                }
            }
        }
    }
}

// Unlocked Main Portal Container (Handles role tabs + display)
@Composable
fun MainPortalContainer(viewModel: HospitalViewModel) {
    val activeRole by viewModel.activePortal.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper banner with role switcher
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = viewModel.t("patient_portal"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val profile by viewModel.patientProfile.collectAsState()
                    Text(
                        text = if (activeRole == "PATIENT") {
                            if (profile != null) "Welcome, ${profile?.fullName}" else "Welcome, Patient"
                        } else if (activeRole == "ADMIN") {
                            "Administrator Panel"
                        } else {
                            "Dr. Access Dashboard"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Toggle roles dynamically for user demo ease
                Row {
                    Button(
                        onClick = { viewModel.setPortal(if (activeRole == "PATIENT") "ADMIN" else "PATIENT") },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("role_switch_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (activeRole == "PATIENT") Icons.Default.SupervisorAccount else Icons.Default.AccountCircle,
                            contentDescription = "Switch",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (activeRole == "PATIENT") "Go Admin" else "Go Patient",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (activeRole == "PATIENT") {
            PatientPortalDashboard(viewModel)
        } else {
            AdminDashboardView(viewModel)
        }
    }
}

// Patient View Portal Dashboard
@Composable
fun PatientPortalDashboard(viewModel: HospitalViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        viewModel.t("appointments") to Icons.Default.Event,
        viewModel.t("reports") to Icons.Default.Description,
        viewModel.t("messaging") to Icons.Default.Forum,
        viewModel.t("billing") to Icons.Default.Payment,
        viewModel.t("med_history") to Icons.Default.HistoryToggleOff
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("patient_tabs")
        ) {
            tabTitles.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("tab_$index"),
                    icon = { Icon(icon, contentDescription = title, modifier = Modifier.size(20.dp)) },
                    text = {
                        Text(
                            text = title,
                            fontSize = 10.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> AppointmentsTab(viewModel)
                1 -> ReportsAndPrescriptionsTab(viewModel)
                2 -> SecureMessagingTab(viewModel)
                3 -> BillingPaymentsTab(viewModel)
                4 -> MedicationHistoryTab(viewModel)
            }
        }
    }
}

// TAB 0: Appointments & Scheduling (Patient)
@Composable
fun AppointmentsTab(viewModel: HospitalViewModel) {
    val appointmentsList by viewModel.appointments.collectAsState()
    val doctorsList by viewModel.doctors.collectAsState()
    var showBookingDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        // App banner image (Generated visual asset)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.shagun_hero_banner),
                    contentDescription = "Shagun Hospital Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Your Wellness is Our Speciality",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "World-class healthcare across 5 comprehensive departments",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Scheduled Visits",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = { showBookingDialog = true },
                modifier = Modifier
                    .height(36.dp)
                    .testTag("book_appt_btn"),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Book", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(viewModel.t("book_appt"), fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (appointmentsList.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "No appointments",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No scheduled appointments found.",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Tap 'Book Appointment' to schedule a consultation with our specialist doctors.",
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            appointmentsList.forEach { appt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("appt_item_${appt.id}"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = appt.doctorName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = appt.speciality,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = appt.status,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${appt.date} at ${appt.time}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        if (appt.reason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reason: ${appt.reason}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { viewModel.cancelAppointment(appt.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("cancel_appt_${appt.id}"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(viewModel.t("cancel"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Section: Doctor Profiles
        Text(
            text = "Featured Multi-Speciality Doctors",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        doctorsList.forEach { doc ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("doctor_profile_card_${doc.id}"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = doc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(text = "${doc.speciality} • ${doc.experience} Exp", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = doc.rating.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = doc.bio, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Roster availability:", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            Text(text = doc.availability, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.selectedChatDoctor.value = doc
                                    // Normally we would transition tab to chat, we can just log a toast or the user can switch tabs
                                    viewModel.sendMessage("Hello, I am scheduling an consultation follow-up.")
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chat", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Booking Dialog Form
    if (showBookingDialog) {
        val profile by viewModel.patientProfile.collectAsState()
        var patientName by remember(profile) { mutableStateOf(profile?.fullName ?: "Patient (You)") }
        var selectedDocIndex by remember { mutableIntStateOf(0) }
        var apptDate by remember { mutableStateOf("2026-07-06") }
        var apptTime by remember { mutableStateOf("10:30 AM") }
        var reasonText by remember { mutableStateOf("") }
        var expandedDocs by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showBookingDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Schedule Appointment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Patient Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Doc Selector dropdown
                    Text(text = viewModel.t("select_doctor"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedDocs = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_doctor_dropdown_btn")
                        ) {
                            Text(
                                text = if (doctorsList.isNotEmpty()) doctorsList[selectedDocIndex].name else "No doctors available"
                            )
                        }

                        DropdownMenu(
                            expanded = expandedDocs,
                            onDismissRequest = { expandedDocs = false }
                        ) {
                            doctorsList.forEachIndexed { idx, doc ->
                                DropdownMenuItem(
                                    text = { Text("${doc.name} (${doc.speciality})") },
                                    onClick = {
                                        selectedDocIndex = idx
                                        expandedDocs = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = apptDate,
                            onValueChange = { apptDate = it },
                            label = { Text(viewModel.t("date")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = apptTime,
                            onValueChange = { apptTime = it },
                            label = { Text(viewModel.t("time")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text(viewModel.t("reason")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showBookingDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(viewModel.t("cancel"))
                        }
                        Button(
                            onClick = {
                                if (doctorsList.isNotEmpty()) {
                                    val doc = doctorsList[selectedDocIndex]
                                    viewModel.bookAppointment(
                                        patientName = patientName,
                                        doctorId = doc.id,
                                        doctorName = doc.name,
                                        speciality = doc.speciality,
                                        date = apptDate,
                                        time = apptTime,
                                        reason = reasonText
                                    )
                                }
                                showBookingDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("confirm_booking_btn")
                        ) {
                            Text(viewModel.t("schedule_now"), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// TAB 1: Medical Reports & Digital Prescriptions (Patient)
@Composable
fun ReportsAndPrescriptionsTab(viewModel: HospitalViewModel) {
    val reportsList by viewModel.reports.collectAsState()
    val prescriptionsList by viewModel.prescriptions.collectAsState()
    var selectedReport by remember { mutableStateOf<MedicalReportEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Clinical Banner
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("HIPAA Locked Medical Record", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Text("All reports and prescriptions are secured locally with AES-256 standard and verified against the cloud backup system.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Section: Medical Reports
        item {
            Text("Your Lab & Medical Reports", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        }

        if (reportsList.isEmpty()) {
            item {
                Text("No medical reports on file.", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            items(reportsList) { rep ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedReport = rep }
                        .testTag("report_card_${rep.id}"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFCDE8D1), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF00210B)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = rep.reportName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "By ${rep.doctorName} • ${rep.department}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "Released: ${rep.reportDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            if (rep.isSecureCloudBackedUp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Backed Up", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Local Only", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("View", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Prescriptions
        item {
            Text("Active Digital Prescriptions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        }

        if (prescriptionsList.isEmpty()) {
            item {
                Text("No digital prescriptions on file.", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            items(prescriptionsList) { pres ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFFFDAD6), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Medication,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color(0xFF410002)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Rx Digital Prescription", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(text = pres.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(text = "Diagnosis: ${pres.diagnosis}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(text = "Medicines & Intake:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = pres.medicationList,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Doctor Advice: ${pres.instructions}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Issued by: ${pres.doctorName}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    // Detail Dialog for Report
    selectedReport?.let { rep ->
        Dialog(onDismissRequest = { selectedReport = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lab Report Analysis",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { selectedReport = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = rep.reportName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Physician: ${rep.doctorName} • Date: ${rep.reportDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Diagnostic Summary:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = rep.summary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Laboratory Test Metrics:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Key Metrics parsed and listed
                    rep.keyMetrics.split(",").forEach { metric ->
                        if (metric.contains(":")) {
                            val parts = metric.split(":")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = parts[0].trim(), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(text = parts[1].trim(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Doctor's Advice & Plan:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = rep.doctorNotes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "HIPAA Compliant Electronic Lock ID: #${rep.hashCode()}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { selectedReport = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

// TAB 2: Secure Messaging Portal (Patient <-> Doctor)
@Composable
fun SecureMessagingTab(viewModel: HospitalViewModel) {
    val doctorsList by viewModel.doctors.collectAsState()
    val selectedDoctor by viewModel.selectedChatDoctor.collectAsState()
    val allMessagesList by viewModel.messages.collectAsState()

    var textInput by remember { mutableStateOf("") }

    // Filter messages for current selected doctor
    val chatMessages = allMessagesList.filter {
        (it.senderId == "patient" && it.receiverId == (selectedDoctor?.id ?: "")) ||
                (it.senderId == (selectedDoctor?.id ?: "") && it.receiverId == "patient")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Selected Doctor profile head
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = selectedDoctor?.name ?: "No Doctor Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = selectedDoctor?.speciality ?: "Click below to select",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // HIPAA Secure Session Indicator
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0E1FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF21005D))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("HIPAA Encrypted", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Select other doctor list row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    doctorsList.forEach { doc ->
                        AssistChip(
                            onClick = { viewModel.selectedChatDoctor.value = doc },
                            label = { Text(doc.name, fontSize = 11.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (doc.id == selectedDoctor?.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }

        // Message body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            if (chatMessages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "No chats",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start secure consultation chat.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Messages are encrypted locally prior to transmission.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = false,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatMessages) { msg ->
                        val isPatient = msg.senderId == "patient"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isPatient) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(0.82f),
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isPatient) 12.dp else 0.dp,
                                    bottomEnd = if (isPatient) 0.dp else 12.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPatient) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (!isPatient) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = msg.senderName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPatient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.content,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Encrypted",
                                            modifier = Modifier.size(10.dp),
                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Encrypted",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Message input row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask your doctor securely...") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("chat_send_btn"),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// TAB 3: Billing & Seamless Payments (Patient)
@Composable
fun BillingPaymentsTab(viewModel: HospitalViewModel) {
    val billingList by viewModel.bills.collectAsState()
    var activePayBill by remember { mutableStateOf<BillingEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Text(
            text = "Pending & Historic Billing",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (billingList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bills generated for your profile.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(billingList) { bill ->
                    val isPaid = bill.status == "Paid"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bill_card_${bill.id}"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Invoice ${bill.invoiceNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPaid) Color(0xFFCDE8D1) else Color(0xFFFFDAD6)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = bill.status.uppercase(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPaid) Color(0xFF00210B) else Color(0xFF410002)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = bill.description,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(text = "Amount: $${"%.2f".format(bill.amount)}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                    Text(text = "Date: ${bill.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }

                                if (!isPaid) {
                                    Button(
                                        onClick = { activePayBill = bill },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .testTag("pay_btn_${bill.id}")
                                    ) {
                                        Text(viewModel.t("pay_now"), fontSize = 12.sp)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "Receipt ID:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = bill.transactionId,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Payment Gateway Simulation Dialog
    activePayBill?.let { bill ->
        var isUpi by remember { mutableStateOf(false) }
        var cardNumber by remember { mutableStateOf("") }
        var expiry by remember { mutableStateOf("") }
        var cvv by remember { mutableStateOf("") }
        var upiAddress by remember { mutableStateOf("shagunhospital@upi") }

        Dialog(onDismissRequest = { activePayBill = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Secure Payment Gateway",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Invoice ${bill.invoiceNumber} - total amount: $${"%.2f".format(bill.amount)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    TabRow(
                        selectedTabIndex = if (isUpi) 1 else 0,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Tab(
                            selected = !isUpi,
                            onClick = { isUpi = false },
                            text = { Text("Credit/Debit Card", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = isUpi,
                            onClick = { isUpi = true },
                            text = { Text("UPI Scan / Address", fontSize = 12.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isUpi) {
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            label = { Text("Card Number") },
                            placeholder = { Text("4111 2222 3333 4444") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_number_field")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = expiry,
                                onValueChange = { expiry = it },
                                label = { Text("MM/YY") },
                                placeholder = { Text("12/28") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cvv,
                                onValueChange = { cvv = it },
                                label = { Text("CVV") },
                                placeholder = { Text("123") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = upiAddress,
                            onValueChange = { upiAddress = it },
                            label = { Text("Enter UPI ID") },
                            placeholder = { Text("username@okhdfc") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upi_field")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Generates automatic local UPI dynamic QR code to settle hospital accounts instantly.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { activePayBill = null },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.payBill(bill.id, cardNumber, upiAddress, isUpi)
                                activePayBill = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("process_pay_btn")
                        ) {
                            Text("Authorize Pay")
                        }
                    }
                }
            }
        }
    }
}

// TAB 4: Medication History & Reminders (Patient)
@Composable
fun MedicationHistoryTab(viewModel: HospitalViewModel) {
    val remindersList by viewModel.activeReminders.collectAsState()
    val profile by viewModel.patientProfile.collectAsState()
    var medName by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Twice Daily") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        profile?.let { prof ->
            Text(
                text = "Secure Patient Medical Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = prof.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "DOB: ${prof.dob} • Gender: ${prof.gender}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Contact Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Phone: ${prof.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "Email: ${prof.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(text = "Emergency Contact: ${prof.emergencyContact}", fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurface)

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Essential Medical Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDAD6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "BLOOD GROUP: ${prof.bloodGroup}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF410002)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Allergies: ${prof.allergies.ifBlank { "None" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Chronic Conditions: ${prof.chronicConditions.ifBlank { "None" }}", fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp), color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Current Medications: ${prof.currentMedications.ifBlank { "None" }}", fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Text(
            text = "Medication Intake Scheduling",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Configure custom reminders below to receive secure mock push notifications when dosage intervals occur.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Set reminder form card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = viewModel.t("add_med_reminder"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = medName,
                    onValueChange = { medName = it },
                    label = { Text(viewModel.t("med_name")) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Atorvastatin 10mg") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("med_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text(viewModel.t("frequency")) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Once daily at bedtime") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (medName.isNotBlank()) {
                            viewModel.addMedicationReminder(medName, frequency)
                            medName = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_reminder_btn")
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Active Reminder")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = viewModel.t("active_reminders"),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (remindersList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("No active dosage reminders configured.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                remindersList.forEach { rem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            Color(0xFFFFDAD6),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AlarmOn,
                                        contentDescription = null,
                                        tint = Color(0xFF410002),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = rem,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.removeMedicationReminder(rem) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Administrator View Panel
// -------------------------------------------------------------
@Composable
fun AdminDashboardView(viewModel: HospitalViewModel) {
    val doctorsList by viewModel.doctors.collectAsState()
    val resourcesList by viewModel.resources.collectAsState()
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val lastBackupTimeStr by viewModel.lastBackupTime.collectAsState()

    var showAddStaffDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("admin_panel"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HIPAA Cloud Backup simulator control card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BackupTable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HIPAA Secure Cloud Ledger",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = viewModel.t("backup_status"),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Synchronize patient medical reports, billing registers, and messaging logs to AES-256 encrypted remote backup nodes in full HIPAA security compliance.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${viewModel.t("last_backup")}: $lastBackupTimeStr",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isBackingUp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Encrypting and syncing logs...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.performCloudBackup() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sync_backup_btn")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(viewModel.t("sync_records"))
                        }
                    }
                }
            }
        }

        // Section: Clinic Facility Resources Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Clinic Resource Monitors",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        items(resourcesList) { res ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = res.resourceName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        val pct = if (res.totalCount > 0) (res.occupiedCount.toFloat() / res.totalCount * 100).toInt() else 0
                        Text(text = "Capacity: $pct% Utilized", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress indicators
                    val progress = if (res.totalCount > 0) res.occupiedCount.toFloat() / res.totalCount else 0f
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (progress > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Total Count: ${res.totalCount}  |  Active: ${res.occupiedCount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                        // Increments/decrements for Admin simulation
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    if (res.occupiedCount > 0) {
                                        viewModel.updateFacilityResource(res.id, res.totalCount, res.occupiedCount - 1)
                                    }
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            IconButton(
                                onClick = {
                                    if (res.occupiedCount < res.totalCount) {
                                        viewModel.updateFacilityResource(res.id, res.totalCount, res.occupiedCount + 1)
                                    }
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // Section: Staff Manager list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Medical Staff Roster",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = { showAddStaffDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_staff_btn")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Staff")
                }
            }
        }

        items(doctorsList) { doc ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = doc.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "${doc.speciality} • Experience: ${doc.experience}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = { /* Detail expand */ }) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Roster Availability: ${doc.availability}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = doc.bio, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    // Add Staff Profile Dialog Form
    if (showAddStaffDialog) {
        var name by remember { mutableStateOf("") }
        var spec by remember { mutableStateOf("General Medicine") }
        var exp by remember { mutableStateOf("5 Years") }
        var avail by remember { mutableStateOf("Mon-Fri, 9:00 AM - 5:00 PM") }
        var bioText by remember { mutableStateOf("") }
        var langs by remember { mutableStateOf("English, Hindi") }

        Dialog(onDismissRequest = { showAddStaffDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = viewModel.t("add_staff"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(viewModel.t("doctor_name")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Dr. John Doe") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_doc_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = spec,
                        onValueChange = { spec = it },
                        label = { Text(viewModel.t("speciality")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = exp,
                            onValueChange = { exp = it },
                            label = { Text(viewModel.t("experience")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = avail,
                            onValueChange = { avail = it },
                            label = { Text(viewModel.t("availability")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = langs,
                        onValueChange = { langs = it },
                        label = { Text(viewModel.t("languages")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = bioText,
                        onValueChange = { bioText = it },
                        label = { Text(viewModel.t("bio")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddStaffDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(viewModel.t("cancel"))
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    viewModel.addDoctor(
                                        name = name,
                                        speciality = spec,
                                        experience = exp,
                                        availability = avail,
                                        bio = bioText,
                                        languages = langs
                                    )
                                }
                                showAddStaffDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("new_doc_submit_btn")
                        ) {
                            Text(viewModel.t("save_profile"))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientOnboardingScreen(viewModel: HospitalViewModel) {
    var step by remember { mutableIntStateOf(1) }

    // Step 1: Personal Details
    var fullName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Male") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }

    // Step 2: Medical Details
    var selectedBloodGroup by remember { mutableStateOf("O+") }
    var allergies by remember { mutableStateOf("") }
    var chronicConditions by remember { mutableStateOf("") }
    var currentMedications by remember { mutableStateOf("") }

    // Step 3: Account Security
    var securePin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var securityError by remember { mutableStateOf<String?>(null) }

    val genders = listOf("Male", "Female", "Other")
    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App logo
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.shagun_logo_icon),
                contentDescription = "Shagun Hospital",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SHAGUN PATIENT ONBOARDING",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Text(
            text = "Please complete registration to activate your HIPAA-secured medical record.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Progress bar indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 1..3) {
                val isActive = step >= i
                val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = i.toString(),
                        color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                if (i < 3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(if (step > i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                when (step) {
                    1 -> {
                        Text(
                            text = "Step 1: Personal Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("e.g. Rahul Sharma") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_fullname"),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            label = { Text("Date of Birth") },
                            placeholder = { Text("e.g. 1994-08-23") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "Gender", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genders.forEach { gender ->
                                val isSelected = selectedGender == gender
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedGender = gender },
                                    label = { Text(gender) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("e.g. +91 9876543210") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            placeholder = { Text("e.g. rahul@example.com") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = emergencyContact,
                            onValueChange = { emergencyContact = it },
                            label = { Text("Emergency Contact Name & Phone") },
                            placeholder = { Text("e.g. Priya Sharma (Wife) - 9876543211") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) }
                        )
                    }

                    2 -> {
                        Text(
                            text = "Step 2: Medical History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Blood Group", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            var expandedGroup by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { expandedGroup = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Selected Blood Group: $selectedBloodGroup", fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(
                                expanded = expandedGroup,
                                onDismissRequest = { expandedGroup = false }
                            ) {
                                bloodGroups.forEach { bg ->
                                    DropdownMenuItem(
                                        text = { Text(bg) },
                                        onClick = { selectedBloodGroup = bg; expandedGroup = false }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = allergies,
                            onValueChange = { allergies = it },
                            label = { Text("Allergies (if any)") },
                            placeholder = { Text("e.g. Penicillin, Peanuts, Gluten") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = chronicConditions,
                            onValueChange = { chronicConditions = it },
                            label = { Text("Chronic Conditions") },
                            placeholder = { Text("e.g. Asthma, Hypertension, Diabetes") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Healing, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = currentMedications,
                            onValueChange = { currentMedications = it },
                            label = { Text("Current Medications") },
                            placeholder = { Text("e.g. Metformin 500mg daily") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) }
                        )
                    }

                    3 -> {
                        Text(
                            text = "Step 3: Account Security PIN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Set up a 4-digit passcode PIN. This passcode is required to decrypt your HIPAA-secured medical portal.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = securePin,
                            onValueChange = { if (it.length <= 4) securePin = it },
                            label = { Text("Choose 4-Digit PIN") },
                            placeholder = { Text("••••") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_pin"),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 4) confirmPin = it },
                            label = { Text("Confirm 4-Digit PIN") },
                            placeholder = { Text("••••") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                        )

                        securityError?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = { step-- },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.showOnboarding.value = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Button(
                        onClick = {
                            if (step == 1) {
                                if (fullName.isBlank()) {
                                    // Name is required
                                } else {
                                    step = 2
                                }
                            } else if (step == 2) {
                                step = 3
                            } else if (step == 3) {
                                if (securePin.length != 4) {
                                    securityError = "PIN must be exactly 4 digits."
                                } else if (securePin != confirmPin) {
                                    securityError = "PINs do not match."
                                } else {
                                    securityError = null
                                    val profile = PatientProfile(
                                        fullName = fullName,
                                        dob = dob,
                                        gender = selectedGender,
                                        phone = phone,
                                        email = email,
                                        emergencyContact = emergencyContact,
                                        bloodGroup = selectedBloodGroup,
                                        allergies = allergies,
                                        chronicConditions = chronicConditions,
                                        currentMedications = currentMedications,
                                        securePin = securePin
                                    )
                                    viewModel.registerPatientProfile(profile)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("onboarding_next_btn")
                    ) {
                        Text(if (step == 3) "Finish & Register" else "Continue")
                        Spacer(modifier = Modifier.width(4.dp))
                        if (step < 3) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
