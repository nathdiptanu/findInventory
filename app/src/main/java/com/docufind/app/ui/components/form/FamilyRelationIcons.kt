package com.docufind.app.ui.components.form

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material.icons.outlined.Man
import androidx.compose.material.icons.outlined.Woman2
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.docufind.app.domain.model.family.FamilyRelation

@Composable
fun FamilyRelation.icon(emergencyContext: Boolean = false): ImageVector = when (this) {
    FamilyRelation.SELF -> Icons.Default.Person
    FamilyRelation.SPOUSE -> if (emergencyContext) Icons.Default.Phone else Icons.Default.Favorite
    FamilyRelation.SON -> Icons.Outlined.Man
    FamilyRelation.DAUGHTER -> Icons.Outlined.Woman2
    FamilyRelation.FATHER -> Icons.Outlined.Man
    FamilyRelation.MOTHER -> Icons.Default.Woman
    FamilyRelation.BROTHER -> Icons.Default.Groups
    FamilyRelation.SISTER -> Icons.Default.Groups
    FamilyRelation.FRIEND -> Icons.Default.People
    FamilyRelation.DOCTOR -> Icons.Default.LocalHospital
    FamilyRelation.VET -> Icons.Default.Pets
    FamilyRelation.NEIGHBOUR -> Icons.Default.Home
    FamilyRelation.GRANDFATHER -> Icons.Outlined.Man
    FamilyRelation.GRANDMOTHER -> Icons.Default.Woman
    FamilyRelation.GUARDIAN -> if (emergencyContext) Icons.Default.MedicalServices else Icons.Default.Shield
    FamilyRelation.OTHER -> if (emergencyContext) Icons.Default.ContactEmergency else Icons.Default.MoreHoriz
}
