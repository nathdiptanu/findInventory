param(
    [string]$RepoRoot = "$PSScriptRoot\offline-repo"
)

function Add-Artifact {
    param(
        [string]$Group,
        [string]$Artifact,
        [string]$Version,
        [string]$RepoBase,
        [string[]]$Extensions = @("pom", "jar")
    )

    $groupPath = ($Group -replace '\.', '/')
    $remoteBase = "$RepoBase/$groupPath/$Artifact/$Version"
    $targetDir = Join-Path $RepoRoot "$groupPath/$Artifact/$Version"
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

    foreach ($ext in $Extensions) {
        $fileName = "$Artifact-$Version.$ext"
        $target = Join-Path $targetDir $fileName
        $remote = "$remoteBase/$fileName"
        Write-Host "Fetching $remote"
        curl.exe --ssl-no-revoke -fsSL -o $target $remote
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "Failed: $remote"
        }
    }
}

$google = "https://dl.google.com/dl/android/maven2"
$maven = "https://repo.maven.apache.org/maven2"

$artifacts = @(
    @{ Group = 'androidx.core'; Artifact = 'core-splashscreen'; Version = '1.0.1'; Repo = $google; Ext = @('pom', 'aar') },
    @{ Group = 'org.jetbrains.kotlinx'; Artifact = 'kotlinx-coroutines-core'; Version = '1.7.3'; Repo = $maven; Ext = @('pom', 'jar') },
    @{ Group = 'org.jetbrains.kotlinx'; Artifact = 'atomicfu'; Version = '0.17.0'; Repo = $maven; Ext = @('pom', 'jar') },
    @{ Group = 'androidx.lifecycle'; Artifact = 'lifecycle-viewmodel-ktx'; Version = '2.6.2'; Repo = $google; Ext = @('pom', 'aar') },
    @{ Group = 'org.jetbrains.kotlinx'; Artifact = 'kotlinx-serialization-protobuf'; Version = '1.6.3'; Repo = $maven; Ext = @('pom', 'jar') },
    @{ Group = 'androidx.lifecycle'; Artifact = 'lifecycle-common-java8'; Version = '2.6.1'; Repo = $google; Ext = @('pom', 'jar') },
    @{ Group = 'androidx.datastore'; Artifact = 'datastore-core-jvm'; Version = '1.1.1'; Repo = $google; Ext = @('pom', 'jar') },
    @{ Group = 'androidx.lifecycle'; Artifact = 'lifecycle-livedata'; Version = '2.0.0'; Repo = $google; Ext = @('pom', 'aar') }
)

foreach ($item in $artifacts) {
    Add-Artifact -Group $item.Group -Artifact $item.Artifact -Version $item.Version -RepoBase $item.Repo -Extensions $item.Ext
}

Write-Host "Missing dependency fetch complete."
