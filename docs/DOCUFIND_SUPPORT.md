# DocuFind — Support & Feedback

In-app support lives under **Settings → Help & Support**. Reports are sent via the user's email app — nothing is uploaded automatically.

## Support email

**ask.artha.support@gmail.com**

Contact Support subject: **DocuFind Support**

---

## Help & Support menu

| Item | Action |
|------|--------|
| FAQ | Dialog with common questions |
| Report a Bug | `ReportBugScreen` form → email |
| Send Feedback | `SendFeedbackScreen` form → email |
| Contact Support | Email intent with prefilled diagnostics |
| Privacy Policy | `PrivacyScreen` |
| App Version | Display only (version name + code) |
| Device Info | Dialog with diagnostics; copy to clipboard |

Report a Bug and Send Feedback are **not** on the main Settings list.

---

## Contact Support email

Prefilled fields:

| Field | Value |
|-------|-------|
| To | ask.artha.support@gmail.com |
| Subject | DocuFind Support |
| Body | Support request + device diagnostics |

Opens Gmail if installed, otherwise system email chooser. If no email app: copy dialog.

---

## Device diagnostics (non-sensitive only)

| Field | Source |
|-------|--------|
| App name | `R.string.app_name` |
| App version name | `BuildConfig.VERSION_NAME` |
| App version code | `BuildConfig.VERSION_CODE` |
| Android version | `Build.VERSION.RELEASE` + API level |
| Device manufacturer | `Build.MANUFACTURER` |
| Device model | `Build.MODEL` |
| Timestamp | ISO-8601 local timezone |

## Never included

- Document names or file contents
- Vault data, PIN, or biometric state
- Medical or personal records
- Database dumps or logs

---

## Report a Bug

Form: issue title, description, module, steps, optional screenshot (max 10 MB).

Uses `DocuFindFormScaffold` with fixed bottom submit and correct IME insets.

**Subject:** `DocuFind Support - Bug Report`

---

## Send Feedback

Simple message form with diagnostics appended. Same keyboard-safe scaffold as bug report.

**Subject:** `DocuFind Support - Feedback`

---

## Architecture

```
app/src/main/java/com/docufind/app/
├── support/
│   ├── SupportConstants.kt
│   ├── DeviceDiagnosticsProvider.kt
│   └── SupportEmailSender.kt
└── ui/screens/support/
    ├── HelpSupportScreen.kt / HelpSupportViewModel.kt
    ├── ReportBugScreen.kt / ReportBugViewModel.kt
    ├── SendFeedbackScreen.kt / SendFeedbackViewModel.kt
    ├── AboutScreen.kt
    └── CopyReportDialog (HelpSupportScreen.kt)
```

---

## Build

```powershell
Set-Location C:\Users\MSUSERSL123\Documents\DocuFind
.\gradle-dist\gradle-8.9\bin\gradle.bat assembleDebug test
```
