param(
    [string]$RepoRoot = "$PSScriptRoot\offline-repo",
    [string]$Google = "https://dl.google.com/dl/android/maven2"
)

function Fetch-Artifact {
    param([string]$Group, [string]$Artifact, [string]$Version)
    $groupPath = ($Group -replace '\.', '/')
    $targetDir = Join-Path $RepoRoot "$groupPath/$Artifact/$Version"
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    foreach ($ext in @('pom', 'jar', 'aar')) {
        $fileName = "$Artifact-$Version.$ext"
        $target = Join-Path $targetDir $fileName
        if (Test-Path $target) { continue }
        $remote = "$Google/$groupPath/$Artifact/$Version/$fileName"
        curl.exe --ssl-no-revoke -fsSL -o $target $remote 2>$null
    }
}

$version = "1.7.6"
$composeGroups = @(
    @{ Group = "androidx.compose.runtime"; Artifacts = @("runtime", "runtime-android", "runtime-saveable", "runtime-saveable-android") },
    @{ Group = "androidx.compose.ui"; Artifacts = @(
        "ui", "ui-android", "ui-text", "ui-text-android", "ui-graphics", "ui-graphics-android",
        "ui-unit", "ui-unit-android", "ui-geometry", "ui-geometry-android", "ui-util", "ui-util-android",
        "ui-tooling", "ui-tooling-android", "ui-tooling-preview", "ui-tooling-preview-android",
        "ui-tooling-data", "ui-tooling-data-android", "ui-test-manifest"
    ) },
    @{ Group = "androidx.compose.animation"; Artifacts = @("animation", "animation-android", "animation-core", "animation-core-android") },
    @{ Group = "androidx.compose.foundation"; Artifacts = @("foundation", "foundation-android", "foundation-layout", "foundation-layout-android") },
    @{ Group = "androidx.compose.material"; Artifacts = @(
        "material", "material-android", "material-ripple", "material-ripple-android",
        "material-icons-core", "material-icons-core-android",
        "material-icons-extended", "material-icons-extended-android"
    ) },
    @{ Group = "androidx.compose.material3"; Artifacts = @("material3", "material3-android") }
)

foreach ($entry in $composeGroups) {
    foreach ($artifact in $entry.Artifacts) {
        Fetch-Artifact -Group $entry.Group -Artifact $artifact -Version $version
    }
}

Fetch-Artifact -Group "androidx.compose.material3" -Artifact "material3" -Version "1.3.1"
Fetch-Artifact -Group "androidx.compose.material3" -Artifact "material3-android" -Version "1.3.1"

$datastoreVersion = "1.1.1"
$datastoreArtifacts = @(
    @{ Group = "androidx.datastore"; Artifacts = @(
        "datastore", "datastore-android", "datastore-preferences", "datastore-preferences-android",
        "datastore-preferences-core", "datastore-preferences-core-jvm",
        "datastore-core", "datastore-core-android", "datastore-core-jvm",
        "datastore-core-okio", "datastore-core-okio-jvm"
    ) }
)
foreach ($entry in $datastoreArtifacts) {
    foreach ($artifact in $entry.Artifacts) {
        Fetch-Artifact -Group $entry.Group -Artifact $artifact -Version $datastoreVersion
    }
}

Write-Host "Compose/datastore artifacts fetched."
