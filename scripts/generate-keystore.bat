@echo off
REM Скрипт для генерации keystore для release-сборки (Windows)
REM Запуск: scripts\generate-keystore.bat

chcp 65001 >nul
setlocal enabledelayedexpansion

set KEYSTORE_FILE=release.keystore
set ALIAS=release
set VALIDITY=10000
set KEYSIZE=2048

echo === Генерация Release Keystore ===
echo.

REM Проверяем, не существует ли уже
if exist "%KEYSTORE_FILE%" (
    echo ⚠️  Файл %KEYSTORE_FILE% уже существует!
    set /p REPLY="Перезаписать? (y/N): "
    if /i not "!REPLY!"=="y" (
        echo Отмена.
        exit /b 1
    )
)

REM Запрашиваем пароли
set /p STOREPASS="Введите пароль для keystore (min 6 символов): "
set /p STOREPASS_CONFIRM="Подтвердите пароль: "

if not "!STOREPASS!"=="!STOREPASS_CONFIRM!" (
    echo ❌ Пароли не совпадают!
    exit /b 1
)

if "!STOREPASS!"=="" (
    echo ❌ Пароль не может быть пустым!
    exit /b 1
)

set /p KEYPRESS="Введите пароль для ключа (Enter = тот же что keystore): "

if "!KEYPASS!"=="" set KEYPASS=!STOREPASS!

REM Данные для сертификата
set /p CN="Ваше имя/организация (CN): "
set /p OU="Организационная единица (OU): "
set /p O="Организация (O): "
set /p L="Город (L): "
set /p ST="Штат/Регион (ST): "
set /p C="Код страны (C) [RU]: "
if "!C!"=="" set C=RU

set DNAME=CN=!CN!, OU=!OU!, O=!O!, L=!L!, ST=!ST!, C=!C!

echo.
echo === Генерация keystore ===
echo Файл: %KEYSTORE_FILE%
echo Alias: %ALIAS%
echo DNAME: %DNAME%
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
    echo ❌ Ошибка при генерации keystore
    exit /b 1
)

echo.
echo ✅ Keystore создан: %KEYSTORE_FILE%
echo.
echo === Для GitHub Secrets (скопируйте значения) ===
echo.

REM Base64 encoding для Windows
certutil -encode "%KEYSTORE_FILE%" "%KEYSTORE_FILE%.b64" >nul
type "%KEYSTORE_FILE%.b64" | findstr /v /c:"BEGIN CERTIFICATE" /c:"END CERTIFICATE" > "%KEYSTORE_FILE%.b64.clean"
set /p BASE64_VAL=<"%KEYSTORE_FILE%.b64.clean"
del "%KEYSTORE_FILE%.b64" "%KEYSTORE_FILE%.b64.clean"

echo KEYSTORE_BASE64:
echo %BASE64_VAL%
echo.
echo KEYSTORE_PASSWORD: %STOREPASS%
echo KEY_ALIAS: %ALIAS%
echo KEY_PASSWORD: %KEYPASS%
echo.
echo === Добавьте в GitHub: ===
echo gh secret set KEYSTORE_BASE64   ^< вставьте base64 строку выше
echo gh secret set KEYSTORE_PASSWORD ^< вставьте пароль
echo gh secret set KEY_ALIAS         ^< вставьте alias
echo gh secret set KEY_PASSWORD      ^< вставьте пароль ключа
echo.
echo ⚠️  ВАЖНО: Сохраните %KEYSTORE_FILE% и пароли в безопасном месте!
echo    Без них нельзя будет обновлять приложение в Play Store.
echo    Файл %KEYSTORE_FILE% уже в .gitignore и НЕ попадёт в git.

pause