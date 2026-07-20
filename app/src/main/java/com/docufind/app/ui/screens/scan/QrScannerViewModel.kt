package com.docufind.app.ui.screens.scan

import androidx.lifecycle.ViewModel
import com.docufind.app.scan.QrContent
import com.docufind.app.scan.QrContentParser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class QrScannerPermissionState {
    NotRequested,
    Rationale,
    Granted,
    Denied
}

data class QrScannerUiState(
    val permissionState: QrScannerPermissionState = QrScannerPermissionState.NotRequested,
    val scannedContent: QrContent? = null,
    val showConfirmation: Boolean = false,
    val scanningEnabled: Boolean = true,
    val statusMessage: String? = null
)

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val qrContentParser: QrContentParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrScannerUiState())
    val uiState: StateFlow<QrScannerUiState> = _uiState.asStateFlow()

    private var lastScanRaw: String? = null
    private var lastScanAtMs: Long = 0L

    fun onPermissionGranted() {
        _uiState.update {
            it.copy(
                permissionState = QrScannerPermissionState.Granted,
                scanningEnabled = it.scannedContent == null
            )
        }
    }

    fun onPermissionDenied() {
        _uiState.update {
            it.copy(
                permissionState = QrScannerPermissionState.Denied,
                scanningEnabled = false
            )
        }
    }

    fun showPermissionRationale() {
        _uiState.update { it.copy(permissionState = QrScannerPermissionState.Rationale) }
    }

    fun dismissPermissionRationale() {
        _uiState.update {
            if (it.permissionState == QrScannerPermissionState.Rationale) {
                it.copy(permissionState = QrScannerPermissionState.NotRequested)
            } else {
                it
            }
        }
    }

    fun onBarcodeDetected(rawValue: String) {
        val state = _uiState.value
        if (!state.scanningEnabled || state.showConfirmation) return

        val now = System.currentTimeMillis()
        if (rawValue == lastScanRaw && now - lastScanAtMs < DEBOUNCE_MS) return

        lastScanRaw = rawValue
        lastScanAtMs = now

        val parsed = qrContentParser.parse(rawValue)
        _uiState.update {
            it.copy(
                scannedContent = parsed,
                showConfirmation = true,
                scanningEnabled = false,
                statusMessage = null
            )
        }
    }

    fun dismissConfirmation() {
        _uiState.update {
            it.copy(
                scannedContent = null,
                showConfirmation = false,
                scanningEnabled = it.permissionState == QrScannerPermissionState.Granted
            )
        }
        lastScanRaw = null
        lastScanAtMs = 0L
    }

    fun onGalleryScanFailed() {
        _uiState.update { it.copy(statusMessage = "Could not read a QR code from that image.") }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    companion object {
        const val DEBOUNCE_MS = 2_000L
    }
}
