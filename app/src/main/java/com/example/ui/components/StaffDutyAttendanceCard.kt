package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.StaffAttendanceEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.UserRole
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.RoyalElectricBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DutyBlue = RoyalElectricBlue
private val DutyRed = BrandRedSecondary
private val DutyGreen = BrandGreenPrimary
private val DutyAmber = MetallicOrange

private fun formatDutyTime(ms: Long): String {
    if (ms <= 0L) return "--:--"
    return SimpleDateFormat("hh:mm a", Locale.US).format(Date(ms))
}

private fun formatDurationHrs(minutes: Long): String {
    val safe = minutes.coerceAtLeast(0L)
    return String.format(Locale.US, "%dh %02dm", safe / 60L, safe % 60L)
}

/**
 * Staff Daily Duty Attendance Card (Morning Punch In -> Lunch/Break Out -> After Break Punch In -> Duty Close Out).
 * Note: Daily Work Hours Report is strictly hidden from Staff and shown ONLY to Admin and Owner View.
 */
@Composable
fun StaffDutyAttendanceCard(
    attendance: StaffAttendanceEntity?,
    selectedDate: String,
    onPunchStage: (String) -> Unit
) {
    val dutyStart = attendance?.dutyStartMs ?: 0L
    val lunchOut = attendance?.lunchOutMs ?: 0L
    val lunchIn = attendance?.lunchInMs ?: 0L
    val dutyClose = attendance?.dutyCloseMs ?: 0L
    val statusText = attendance?.statusLabel() ?: "Not Punched In"

    val statusColor = when {
        dutyClose > 0L -> DutyGreen
        lunchOut > 0L && lunchIn <= 0L -> DutyAmber
        dutyStart > 0L -> DutyBlue
        else -> DutyRed
    }

    LuxuryGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("staff_duty_attendance_card"),
        shape = RoundedCornerShape(18.dp),
        accentGlow = DutyBlue
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DutyBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = DutyBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Duty Attendance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Date: $selectedDate • 4-Step Punch Log",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 4-step timestamps grid (2x2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DutyPunchTile(
                    modifier = Modifier.weight(1f),
                    label = "Morning In",
                    timeText = formatDutyTime(dutyStart),
                    completed = dutyStart > 0L,
                    accent = DutyGreen
                )
                DutyPunchTile(
                    modifier = Modifier.weight(1f),
                    label = "Lunch / Break Out",
                    timeText = formatDutyTime(lunchOut),
                    completed = lunchOut > 0L,
                    accent = DutyAmber
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DutyPunchTile(
                    modifier = Modifier.weight(1f),
                    label = "After Break In",
                    timeText = formatDutyTime(lunchIn),
                    completed = lunchIn > 0L,
                    accent = DutyBlue
                )
                DutyPunchTile(
                    modifier = Modifier.weight(1f),
                    label = "Duty Close Out",
                    timeText = formatDutyTime(dutyClose),
                    completed = dutyClose > 0L,
                    accent = DutyRed
                )
            }

            Spacer(Modifier.height(12.dp))

            // Primary action buttons depending on current stage
            when {
                dutyStart <= 0L -> {
                    Button(
                        onClick = { onPunchStage("DUTY_START") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("punch_duty_start_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = DutyGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.WbSunny, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Morning Duty Start — Punch In", fontWeight = FontWeight.Bold)
                    }
                }
                dutyClose > 0L -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DutyGreen.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = DutyGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Today's duty completed & submitted to Admin.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DutyGreen
                            )
                        }
                    }
                }
                lunchOut <= 0L -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onPunchStage("LUNCH_OUT") },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("punch_lunch_out_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = DutyAmber),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.FreeBreakfast, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Lunch / Break Out", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onPunchStage("DUTY_CLOSE") },
                            modifier = Modifier
                                .weight(0.85f)
                                .heightIn(min = 48.dp)
                                .testTag("punch_duty_close_btn"),
                            border = BorderStroke(1.dp, DutyRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Logout, contentDescription = null, tint = DutyRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Duty Close", color = DutyRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                lunchIn <= 0L -> {
                    Button(
                        onClick = { onPunchStage("LUNCH_IN") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("punch_lunch_in_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = DutyBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("After Break Rest — Punch In", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Button(
                        onClick = { onPunchStage("DUTY_CLOSE") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("punch_duty_close_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = DutyRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Duty Close — Punch Out", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DutyPunchTile(
    modifier: Modifier = Modifier,
    label: String,
    timeText: String,
    completed: Boolean,
    accent: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (completed) accent.copy(alpha = 0.09f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = if (completed) BorderStroke(1.dp, accent.copy(alpha = 0.35f)) else null
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (completed) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Admin & Owner View ONLY: Daily Work Hours Summary Card.
 */
@Composable
fun AdminDailyWorkHoursCard(
    selectedDate: String,
    users: List<UserAccountEntity>,
    vehicles: List<VehicleEntity>,
    attendanceList: List<StaffAttendanceEntity>,
    onOpenWorkHoursReport: () -> Unit
) {
    val staffUsers = users.filter { it.role == UserRole.STAFF && it.isActive }
    val vehicleByStaff = vehicles.associateBy { it.staffUserId }
    val attByStaff = attendanceList.associateBy { it.staffId }
    val punchedInCount = attendanceList.count { it.dutyStartMs > 0L }
    val totalWorkMinutes = attendanceList.sumOf { it.netWorkMinutes() }

    LuxuryGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_daily_work_hours_card"),
        shape = RoundedCornerShape(18.dp),
        accentGlow = DutyBlue
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = DutyBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Staff Daily Work Hours (Admin & Owner)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "$selectedDate • $punchedInCount/${staffUsers.size} On Duty • Total: ${formatDurationHrs(totalWorkMinutes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            val displayedStaff = staffUsers.take(6)
            displayedStaff.forEachIndexed { idx, staff ->
                val att = attByStaff[staff.id]
                val vehName = att?.vehicleName?.ifBlank { null } ?: vehicleByStaff[staff.id]?.name ?: "Staff"
                val workHrs = formatDurationHrs(att?.netWorkMinutes() ?: 0L)
                val breakHrs = formatDurationHrs(att?.breakDurationMinutes() ?: 0L)
                val status = att?.statusLabel() ?: "Not Punched In"
                val active = (att?.dutyStartMs ?: 0L) > 0L

                if (idx > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${staff.displayName} ($vehName)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "In: ${formatDutyTime(att?.dutyStartMs ?: 0L)} • Break: $breakHrs • Close: ${formatDutyTime(att?.dutyCloseMs ?: 0L)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = workHrs,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (active) DutyBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) DutyGreen else DutyRed
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenWorkHoursReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("open_work_hours_report_btn"),
                border = BorderStroke(1.dp, DutyBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Assessment, contentDescription = null, tint = DutyBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Full Daily Work Hours Report", color = DutyBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}
