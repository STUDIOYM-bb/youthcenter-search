$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path "config")) {
    New-Item -ItemType Directory -Path "config" | Out-Null
}

$created = New-Object System.Collections.Generic.List[string]
$kept = New-Object System.Collections.Generic.List[string]

if (-not (Test-Path "config/application-secret.yml")) {
    Copy-Item "config/application-secret.example.yml" "config/application-secret.yml"
    $created.Add("config/application-secret.yml")
} else {
    $kept.Add("config/application-secret.yml")
}

if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    $created.Add(".env")
} else {
    $kept.Add(".env")
}

foreach ($file in $created) {
    Write-Host "[CREATED] $file"
}

foreach ($file in $kept) {
    Write-Host "[EXISTS] $file"
}

Write-Host ""
Write-Host "다음 파일에 실제 로컬 값을 입력하세요."
Write-Host "- config/application-secret.yml"
Write-Host ""
Write-Host "필수 항목:"
Write-Host "- YOUTH_CENTER_API_KEY"
Write-Host "- OPENAI_API_KEY"
Write-Host "- ADMIN_API_KEY"
Write-Host ""
Write-Host "RAG 사용 시 활성화:"
Write-Host "- SPRING_AI_MODEL_CHAT=openai"
Write-Host "- SPRING_AI_MODEL_EMBEDDING=openai"
Write-Host "- RAG_ENABLED=true"
