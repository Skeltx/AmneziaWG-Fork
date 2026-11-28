@echo off
REM Generate Release Keystore for Android
REM Run: scripts\generate-keystore.bat

chcp 65001 >nul
setlocal enabledelayedexpansion

set KEYSTORE_FILE=release.keystore
set ALIAS=release
set VALIDITY=10000
set KEYSIZE=2048

echo =========================================
echo Generate Release Keystore
echo =========================================
echo.

REM Check if file exists
if exist "%KEYSTORE_FILE%" (
    echo WARNING: File %KEYSTORE_FILE% already exists!
    set /p REPLY="Overwrite? (y/N): "
    if /i not "!REPLY!"=="y" (
        echo Cancelled.
        exit /b 1
    )
)

echo.
echo Enter passwords (min 6 chars):
echo.

set /p STOREPASS="Keystore password: "
set /p STOREPASS2="Confirm password:  "

if not "!STOREPASS!"=="!STOREPASS2!" (
    echo ERROR: Passwords do not match!
    exit /b 1
)

if "!STOREPASS!"=="" (
    echo ERROR: Password cannot be empty!
    exit /b 1
)

set /p KEYPRESS="Key password (Enter = same as keystore): "
if "!KEYPASS!"=="" set KEYPASS=!STOREPASS!

echo.
echo Certificate details:
echo.

set /p CN="Your name/org (CN): "
set /p OU="Organizational unit (OU): "
set /p O="Organization (O): "
set /p L="City (L): "
set /p ST="State/Region (ST): "
set /p C="Country code (C) [RU]: "
if "!C!"=="" set C=RU

set DNAME=CN=!CN!, OU=!OU!, O=!O!, L=!L!, ST=!ST!, C=!C!

echo.
echo Generating keystore...
echo File: %KEYSTORE_FILE%
echo Alias: %ALIAS%
echo DN: %DNAME%
echo.

keytool -genkeypair -v ^
  -keystore "%KEYSTORE_FILE%" ^
  -alias "%ALIAS%" ^
  -keyalg RSA ^
  -keysize %KEYSIZE% ^
  -validity %VALIDITY% ^
  -storepass "%STOREPASS%" ^
  -keypass "%KEYPASS%" ^
  -dname "%DNAME%"

if errorlevel 1 (
    echo ERROR: Failed to generate keystore
    exit /b 1
)

echo.
echo SUCCESS: Keystore created: %KEYSTORE_FILE%
echo.

REM Base64 encode for GitHub Secrets
certutil -encode "%KEYSTORE_FILE%" "%KEYSTORE_FILE%.b64" >nul
type "%KEYSTORE_FILE%.b64" | findstr /v "BEGIN CERTIFICATE" | findstr /v "END CERTIFICATE" > "%KEYSTORE_FILE%.b64.clean"
set /p BASE64_VAL=<"%KEYSTORE_FILE%.b64.clean"
del "%KEYSTORE_FILE%.b64" "%KEYSTORE_FILE%.b64.clean"

echo =========================================
echo GitHub Secrets (copy these values):
echo =========================================
echo.
echo KEYSTORE_BASE64:
echo %BASE64_VAL%
echo.
echo KEYSTORE_PASSWORD: %STOREPASS%
echo KEY_ALIAS: %ALIAS%
echo KEY_PASSWORD: %KEYPASS%
echo.
echo =========================================
echo Add to GitHub: Settings -> Secrets -> Actions
echo =========================================
echo gh secret set KEYSTORE_BASE64
echo gh secret set KEYSTORE_PASSWORD
echo gh secret set KEY_ALIAS
echo gh secret set KEY_PASSWORD
echo.
echo IMPORTANT: Save %KEYSTORE_FILE% and passwords securely!
echo Without them you cannot update app in Play Store.
echo File %KEYSTORE_FILE% is in .gitignore and will NOT be committed.
echo.
pause