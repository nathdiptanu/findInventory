param(
    [string]$CacheRoot = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1",
    [string]$RepoRoot = "$PSScriptRoot\plugin-repo"
)

function Copy-Artifact {
    param(
        [string]$Group,
        [string]$Artifact,
        [string]$Version
    )

    $sourceDir = Join-Path $CacheRoot $Group
    $sourceDir = Join-Path $sourceDir $Artifact
    $sourceDir = Join-Path $sourceDir $Version
    if (-not (Test-Path $sourceDir)) {
        Write-Warning "Missing cache entry: $Group`:$Artifact`:$Version"
        return
    }

    $groupPath = ($Group -replace '\.', '/')
    $targetDir = Join-Path $RepoRoot "$groupPath/$Artifact/$Version"
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

    $files = Get-ChildItem $sourceDir -Recurse -File
    $jar = $files | Where-Object { $_.Extension -eq '.jar' -and $_.Name -notmatch 'sources|javadoc' } | Select-Object -First 1
    $pom = $files | Where-Object { $_.Extension -eq '.pom' } | Select-Object -First 1

    if ($pom) {
        Copy-Item $pom.FullName (Join-Path $targetDir "$Artifact-$Version.pom") -Force
    }
    if ($jar) {
        $targetJar = Join-Path $targetDir "$Artifact-$Version$($jar.Extension)"
        Copy-Item $jar.FullName $targetJar -Force
        if ($jar.Name -ne "$Artifact-$Version.jar") {
            Copy-Item $jar.FullName (Join-Path $targetDir "$Artifact-$Version.jar") -Force
        }
    }
}

Remove-Item $RepoRoot -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $RepoRoot | Out-Null

$artifacts = @(
    @{ Group = 'com.android.tools.build'; Artifact = 'gradle'; Version = '8.7.3' },
    @{ Group = 'org.jetbrains.kotlin'; Artifact = 'kotlin-gradle-plugin'; Version = '2.0.21' },
    @{ Group = 'org.jetbrains.kotlin'; Artifact = 'compose-compiler-gradle-plugin'; Version = '2.0.21' },
    @{ Group = 'com.google.dagger'; Artifact = 'hilt-android-gradle-plugin'; Version = '2.52' },
    @{ Group = 'com.google.devtools.ksp'; Artifact = 'symbol-processing-gradle-plugin'; Version = '2.0.21-1.0.28' },
    @{ Group = 'com.android.application'; Artifact = 'com.android.application.gradle.plugin'; Version = '8.7.3' },
    @{ Group = 'org.jetbrains.kotlin.android'; Artifact = 'org.jetbrains.kotlin.android.gradle.plugin'; Version = '2.0.21' },
    @{ Group = 'org.jetbrains.kotlin.plugin.compose'; Artifact = 'org.jetbrains.kotlin.plugin.compose.gradle.plugin'; Version = '2.0.21' },
    @{ Group = 'com.google.dagger.hilt.android'; Artifact = 'com.google.dagger.hilt.android.gradle.plugin'; Version = '2.52' },
    @{ Group = 'com.google.devtools.ksp'; Artifact = 'com.google.devtools.ksp.gradle.plugin'; Version = '2.0.21-1.0.28' }
)

foreach ($item in $artifacts) {
    Copy-Artifact -Group $item.Group -Artifact $item.Artifact -Version $item.Version
}

Write-Host "Local plugin repo ready at $RepoRoot"
