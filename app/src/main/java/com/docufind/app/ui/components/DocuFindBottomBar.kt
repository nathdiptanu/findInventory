package com.docufind.app.ui.components



import androidx.compose.foundation.layout.size

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.Home

import androidx.compose.material.icons.filled.Lock

import androidx.compose.material.icons.filled.Schedule

import androidx.compose.material.icons.filled.Settings

import androidx.compose.material3.FloatingActionButton

import androidx.compose.material3.Icon

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.NavigationBar

import androidx.compose.material3.NavigationBarItem

import androidx.compose.material3.NavigationBarItemDefaults

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import com.docufind.app.R

import com.docufind.app.ui.navigation.DocuFindRoutes



private data class SideNavItem(

    val route: String,

    val labelRes: Int,

    val icon: ImageVector

)



@Composable

fun DocuFindBottomBar(

    currentRoute: String?,

    onNavigate: (String) -> Unit

) {

    val leftItems = listOf(

        SideNavItem(DocuFindRoutes.HOME, R.string.nav_home, Icons.Default.Home),

        SideNavItem(DocuFindRoutes.VAULT, R.string.nav_vault, Icons.Default.Lock)

    )

    val rightItems = listOf(

        SideNavItem(DocuFindRoutes.REMINDERS, R.string.nav_reminders, Icons.Default.Schedule),

        SideNavItem(DocuFindRoutes.SETTINGS, R.string.nav_settings, Icons.Default.Settings)

    )



    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {

        leftItems.forEach { item ->

            NavigationBarItem(

                selected = currentRoute == item.route,

                onClick = { onNavigate(item.route) },

                icon = { Icon(item.icon, contentDescription = null) },

                label = {
                    Text(
                        text = stringResource(item.labelRes),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall
                    )
                },

                colors = NavigationBarItemDefaults.colors(

                    selectedIconColor = MaterialTheme.colorScheme.primary,

                    selectedTextColor = MaterialTheme.colorScheme.primary,

                    indicatorColor = MaterialTheme.colorScheme.primaryContainer

                )

            )

        }



        NavigationBarItem(

            selected = currentRoute == DocuFindRoutes.ADD,

            onClick = { onNavigate(DocuFindRoutes.ADD) },

            icon = {

                FloatingActionButton(

                    onClick = { onNavigate(DocuFindRoutes.ADD) },

                    shape = CircleShape,

                    containerColor = MaterialTheme.colorScheme.primary,

                    contentColor = MaterialTheme.colorScheme.onPrimary,

                    modifier = Modifier.size(48.dp)

                ) {

                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nav_add))

                }

            },

            label = {
                Text(
                    text = stringResource(R.string.nav_add),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall
                )
            },

            alwaysShowLabel = true

        )



        rightItems.forEach { item ->

            NavigationBarItem(

                selected = currentRoute == item.route,

                onClick = { onNavigate(item.route) },

                icon = { Icon(item.icon, contentDescription = null) },

                label = {
                    Text(
                        text = stringResource(item.labelRes),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall
                    )
                },

                colors = NavigationBarItemDefaults.colors(

                    selectedIconColor = MaterialTheme.colorScheme.primary,

                    selectedTextColor = MaterialTheme.colorScheme.primary,

                    indicatorColor = MaterialTheme.colorScheme.primaryContainer

                )

            )

        }

    }

}


