# Downloads the pinned sing-box-lx libbox AAR (AmneziaWG 1.x/2.0/3.x).
# The AAR is intentionally not stored in git (it is ~120 MB).
$ErrorActionPreference = "Stop"

$Version = "1.14.4-lx.37"
$Sha256 = "bf4d0a8f60201315c5bd8b6835c66d895cf5a4c932d5d6853f84aa919b5aa687"
$Url = "https://github.com/Leadaxe/sing-box-lx/releases/download/v$Version/libbox-$Version.aar"

$Root = Split-Path -Parent $PSScriptRoot
$Dest = Join-Path $Root "ui\libs\libbox.aar"
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Dest) | Out-Null

if (Test-Path $Dest) {
    $hash = (Get-FileHash $Dest -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($hash -eq $Sha256) {
        Write-Host "libbox $Version already present"
        exit 0
    }
}

Write-Host "Downloading libbox $Version..."
$tmp = "$Dest.tmp"
curl.exe -L --retry 3 --fail -o $tmp $Url
$hash = (Get-FileHash $tmp -Algorithm SHA256).Hash.ToLowerInvariant()
if ($hash -ne $Sha256) {
    Remove-Item $tmp -Force
    throw "libbox checksum mismatch: $hash"
}
Move-Item $tmp $Dest -Force
Write-Host "libbox $Version ready at $Dest"
