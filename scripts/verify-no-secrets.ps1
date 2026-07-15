param(
    [string]$Archive = ""
)

$failed = $false

function Report([string]$Message) {
    Write-Error $Message
    $script:failed = $true
}

$trackedSecrets = git ls-files ".env" "config/application-secret.yml" "src/main/resources/application-secret.yml" "*application-secret.yml" 2>$null
if ($trackedSecrets) {
    Report "Secret files are tracked by Git:"
    $trackedSecrets | ForEach-Object { Write-Error $_ }
}

$trackedCandidates = git ls-files "src/main/java" "src/main/resources" "src/test/java" "scripts" "docs" "README.md" "build.gradle" "settings.gradle" ".gitignore" ".editorconfig" 2>$null
foreach ($file in $trackedCandidates) {
    if (Test-Path $file) {
        $text = Get-Content -Raw -ErrorAction SilentlyContinue $file
        if ($text -match "sk-[A-Za-z0-9_-]{20,}|OPENAI_API_KEY\s*=") {
            Report "Potential secret pattern found in tracked file: $file"
        }
    }
}

if ($Archive) {
    if (-not (Test-Path $Archive)) {
        Report "Archive not found: $Archive"
    } else {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $Archive))
        try {
            foreach ($entry in $zip.Entries) {
                $name = $entry.FullName
                if ($name -match "(^|/)(\.env|application-secret\.yml)$|(^|/)(build|\.gradle|\.git|\.idea|out|logs)/|\.jar$|\.zip$|__MACOSX|\.DS_Store") {
                    Report "Forbidden file found in archive: $name"
                }
            }
        } finally {
            $zip.Dispose()
        }
    }
}

if ($failed) {
    exit 1
}
