package com.docufind.app.ui.screens.add



import androidx.compose.runtime.Composable



@Composable

fun AddScreen(

    onDocumentSaved: (String) -> Unit = {}

) {

    NewDocumentScreen(onDocumentSaved = onDocumentSaved)

}


