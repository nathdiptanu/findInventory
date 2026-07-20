package com.docufind.app.ui.components.form

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.docufind.app.domain.model.VaultCategory

@Composable
fun VaultCategory.icon(): ImageVector = when (this) {
    VaultCategory.DOCUMENTS -> Icons.Default.Description
    VaultCategory.ID_CARDS -> Icons.Default.Badge
    VaultCategory.CARDS -> Icons.Default.CreditCard
    VaultCategory.MEDICAL -> Icons.Default.MedicalServices
    VaultCategory.PRESCRIPTIONS -> Icons.Default.LocalPharmacy
    VaultCategory.VACCINATION -> Icons.Default.Vaccines
    VaultCategory.EDUCATION -> Icons.Default.School
    VaultCategory.INSURANCE -> Icons.Default.Shield
    VaultCategory.VEHICLE -> Icons.Default.DirectionsCar
    VaultCategory.WARRANTY -> Icons.Default.VerifiedUser
    VaultCategory.PETS -> Icons.Default.Pets
    VaultCategory.FAMILY -> Icons.Default.Groups
    VaultCategory.EMERGENCY -> Icons.Default.Emergency
    VaultCategory.PROPERTY -> Icons.Default.Home
    VaultCategory.TRAVEL -> Icons.Default.Flight
    VaultCategory.FINANCE -> Icons.Default.Savings
    VaultCategory.OTHERS -> Icons.Default.MoreHoriz
}
