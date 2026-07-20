package com.docufind.app.ui.screens.scan

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.scan.QrContent
import com.docufind.app.ui.components.DocuFindPrimaryButton
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    viewModel: QrScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val image = InputImage.fromFilePath(context, uri)
            BarcodeScanning.getClient().process(image)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull()?.rawValue
                    if (raw != null) {
                        viewModel.onBarcodeDetected(raw)
                    } else {
                        viewModel.onGalleryScanFailed()
                    }
                }
                .addOnFailureListener { viewModel.onGalleryScanFailed() }
        }.onFailure { viewModel.onGalleryScanFailed() }
    }

    LaunchedEffect(Unit) {
        when (
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        ) {
            PackageManager.PERMISSION_GRANTED -> viewModel.onPermissionGranted()
            else -> viewModel.showPermissionRationale()
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    if (uiState.permissionState == QrScannerPermissionState.Rationale) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissPermissionRationale()
                onBack()
            },
            title = { Text(stringResource(R.string.qr_camera_permission_title)) },
            text = { Text(stringResource(R.string.qr_camera_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissPermissionRationale()
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text(stringResource(R.string.qr_camera_permission_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissPermissionRationale()
                    onBack()
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.permissionState == QrScannerPermissionState.Denied) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text(stringResource(R.string.qr_camera_permission_title)) },
            text = { Text(stringResource(R.string.qr_camera_permission_denied)) },
            confirmButton = {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (uiState.showConfirmation && uiState.scannedContent != null) {
        QrScanConfirmationSheet(
            content = uiState.scannedContent!!,
            onDismiss = viewModel::dismissConfirmation,
            onActionMessage = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.qr_scan_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.qr_scan_from_gallery)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.permissionState == QrScannerPermissionState.Granted) {
                QrCameraPreview(
                    scanningEnabled = uiState.scanningEnabled,
                    onBarcodeDetected = viewModel::onBarcodeDetected,
                    modifier = Modifier.fillMaxSize()
                )
                QrScanFrame(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.permissionState != QrScannerPermissionState.Denied &&
                uiState.permissionState != QrScannerPermissionState.Rationale
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.qr_scan_waiting_camera),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QrCameraPreview(
    scanningEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            barcodeScanner.close()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier,
        update = { previewView ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    val provider = future.get()
                    cameraProvider = provider
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!scanningEnabled) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull { it.rawValue != null }?.rawValue?.let { raw ->
                                        onBarcodeDetected(raw)
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                },
                ContextCompat.getMainExecutor(context)
            )
        }
    )
}

@Composable
private fun QrScanFrame(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(260.dp)
            .border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrScanConfirmationSheet(
    content: QrContent,
    onDismiss: () -> Unit,
    onActionMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    if (pendingAction != null) {
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(stringResource(R.string.qr_confirm_action_title)) },
            text = { Text(stringResource(R.string.qr_confirm_action_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingAction?.invoke()
                    pendingAction = null
                    onDismiss()
                }) {
                    Text(stringResource(R.string.continue_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.qr_confirm_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = contentSummary(content),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            when (content) {
                is QrContent.Phone -> {
                    DocuFindPrimaryButton(
                        text = stringResource(R.string.qr_action_call),
                        onClick = {
                            pendingAction = { dialPhone(context, content.number) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is QrContent.WhatsApp -> {
                    DocuFindPrimaryButton(
                        text = stringResource(R.string.qr_action_whatsapp),
                        onClick = {
                            pendingAction = { openWhatsApp(context, content.number, onActionMessage) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is QrContent.Email -> {
                    DocuFindPrimaryButton(
                        text = stringResource(R.string.qr_action_email),
                        onClick = {
                            pendingAction = { openEmail(context, content.address) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is QrContent.Url -> {
                    if (content.isSafeScheme) {
                        DocuFindPrimaryButton(
                            text = stringResource(R.string.qr_action_open_url),
                            onClick = {
                                pendingAction = { openSafeUrl(context, content.url) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.qr_unsafe_url_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is QrContent.Contact -> {
                    DocuFindPrimaryButton(
                        text = stringResource(R.string.qr_action_save_contact),
                        onClick = {
                            pendingAction = { saveContact(context, content, onActionMessage) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is QrContent.PlainText -> Unit
            }

            DocuFindPrimaryButton(
                text = stringResource(R.string.qr_action_copy),
                onClick = {
                    copyToClipboard(context, contentSummary(content), onActionMessage)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

private fun contentSummary(content: QrContent): String = when (content) {
    is QrContent.Phone -> content.number
    is QrContent.WhatsApp -> content.number
    is QrContent.Email -> content.address
    is QrContent.Url -> content.url
    is QrContent.Contact -> content.displayName ?: content.phone ?: content.email ?: "Contact"
    is QrContent.PlainText -> content.text
}

private fun dialPhone(context: Context, number: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
    context.startActivity(intent)
}

private fun openWhatsApp(context: Context, number: String, onMessage: (String) -> Unit) {
    val digits = number.filter { it.isDigit() }
    val uri = Uri.parse("https://wa.me/$digits")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        onMessage(context.getString(R.string.qr_whatsapp_not_installed))
    }
}

private fun openEmail(context: Context, address: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address"))
    context.startActivity(intent)
}

private fun openSafeUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

private fun copyToClipboard(context: Context, text: String, onMessage: (String) -> Unit) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("qr_content", text))
    onMessage(context.getString(R.string.qr_copied))
}

private fun saveContact(context: Context, contact: QrContent.Contact, onMessage: (String) -> Unit) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        type = ContactsContract.Contacts.CONTENT_TYPE
        contact.displayName?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
        contact.phone?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
        contact.email?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
        onMessage(context.getString(R.string.qr_contact_saved))
    } else {
        onMessage(context.getString(R.string.qr_contact_save_failed))
    }
}
