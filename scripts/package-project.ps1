param(
    [string]$OutputPath = "build/youth-center-api-lab-share.zip"
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path ".").Path
$output = Join-Path $root $OutputPath
$outputDir = Split-Path -Parent $output
New-Item -ItemType Directory -Force $outputDir | Out-Null
if (Test-Path $output) {
    Remove-Item -LiteralPath $output -Force
}

$excludePatterns = @(
    "\\.git(\\|$)",
    "\\.idea(\\|$)",
    "\\.gradle(\\|$)",
    "\\build(\\|$)",
    "\\out(\\|$)",
    "\\.env$",
    "\\.env\\.",
    "application-secret\\.yml$",
    "\\.log$"
)

$files = Get-ChildItem -Path $root -Recurse -File | Where-Object {
    $relative = $_.FullName.Substring($root.Length)
    foreach ($pattern in $excludePatterns) {
        if ($relative -match $pattern) {
            return $false
        }
    }
    return $true
}

$temp = Join-Path $env:TEMP ("youth-center-api-lab-" + [guid]::NewGuid())
New-Item -ItemType Directory -Force $temp | Out-Null
try {
    foreach ($file in $files) {
        $relative = $file.FullName.Substring($root.Length).TrimStart("\\")
        $target = Join-Path $temp $relative
        New-Item -ItemType Directory -Force (Split-Path -Parent $target) | Out-Null
        Copy-Item -LiteralPath $file.FullName -Destination $target
    }
    Compress-Archive -Path (Join-Path $temp "*") -DestinationPath $output -Force
    Write-Output "Created $OutputPath"
} finally {
    Remove-Item -LiteralPath $temp -Recurse -Force
}
