param(
    [string]$CacheRoot = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1",
    [string]$RepoRoot = "$PSScriptRoot\offline-repo"
)

function Copy-CacheEntry {
    param([string]$GroupDir)

    $groupName = Split-Path $GroupDir -Leaf
    $groupPath = ($groupName -replace '\.', '/')
    foreach ($artifactDir in Get-ChildItem $GroupDir -Directory -ErrorAction SilentlyContinue) {
        foreach ($versionDir in Get-ChildItem $artifactDir.FullName -Directory -ErrorAction SilentlyContinue) {
            $artifact = $artifactDir.Name
            $version = $versionDir.Name
            $files = Get-ChildItem $versionDir.FullName -Recurse -File
            if (-not $files) { continue }

            $targetDir = Join-Path $RepoRoot "$groupPath/$artifact/$version"
            New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

            foreach ($file in $files) {
                switch ($file.Extension) {
                    '.pom' {
                        Copy-Item $file.FullName (Join-Path $targetDir "$artifact-$version.pom") -Force
                    }
                    '.aar' {
                        Copy-Item $file.FullName (Join-Path $targetDir "$artifact-$version.aar") -Force
                    }
                    '.jar' {
                        if ($file.Name -match 'sources|javadoc') { continue }
                        Copy-Item $file.FullName (Join-Path $targetDir "$artifact-$version.jar") -Force
                    }
                }
            }
        }
    }
}

Write-Host "Syncing Gradle cache to offline maven repo..."
Remove-Item $RepoRoot -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $RepoRoot | Out-Null

$groups = Get-ChildItem $CacheRoot -Directory
$count = 0
foreach ($group in $groups) {
    Copy-CacheEntry -GroupDir $group.FullName
    $count++
    if ($count % 100 -eq 0) { Write-Host "Processed $count groups..." }
}

Write-Host "Offline repo ready at $RepoRoot ($count groups)"
