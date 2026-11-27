#!/bin/bash
# Скрипт для генерации keystore для release-сборки
# Запуск: ./scripts/generate-keystore.sh

set -e

KEYSTORE_FILE="release.keystore"
ALIAS="release"
VALIDITY=10000
KEYSIZE=2048

echo "=== Генерация Release Keystore ==="
echo ""

# Проверяем, не существует ли уже
if [ -f "$KEYSTORE_FILE" ]; then
    echo "⚠️  Файл $KEYSTORE_FILE уже существует!"
    read -p "Перезаписать? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Отмена."
        exit 1
    fi
fi

# Запрашиваем пароли
read -s -p "Введите пароль для keystore (min 6 символов): " STOREPASS
echo
read -s -p "Подтвердите пароль: " STOREPASS_CONFIRM
echo

if [ "$STOREPASS" != "$STOREPASS_CONFIRM" ]; then
    echo "❌ Пароли не совпадают!"
    exit 1
fi

if [ ${#STOREPASS} -lt 6 ]; then
    echo "❌ Пароль слишком короткий (минимум 6 символов)!"
    exit 1
fi

read -s -p "Введите пароль для ключа (Enter = тот же что keystore): " KEYPRESS
echo

if [ -z "$KEYPASS" ]; then
    KEYPASS="$STOREPASS"
fi

# Данные для сертификата
read -p "Ваше имя/организация (CN): " CN
read -p "Организационная единица (OU): " OU
read -p "Организация (O): " O
read -p "Город (L): " L
read -p "Штат/Регион (ST): " ST
read -p "Код страны (C) [RU]: " C
C=${C:-RU}

DNAME="CN=$CN, OU=$OU, O=$O, L=$L, ST=$ST, C=$C"

echo ""
echo "=== Генерация keystore ==="
echo "Файл: $KEYSTORE_FILE"
echo "Alias: $ALIAS"
echo "DNAME: $DNAME"
echo ""

keytool -genkeypair -v \
  -keystore "$KEYSTORE_FILE" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize $KEYSIZE \
  -validity $VALIDITY \
  -storepass "$STOREPASS" \
  -keypass "$KEYPASS" \
  -dname "$DNAME"

echo ""
echo "✅ Keystore создан: $KEYSTORE_FILE"
echo ""
echo "=== Для GitHub Secrets (скопируйте значения) ==="
echo ""
echo "KEYSTORE_BASE64:"
base64 -w0 "$KEYSTORE_FILE"
echo ""
echo ""
echo "KEYSTORE_PASSWORD: $STOREPASS"
echo "KEY_ALIAS: $ALIAS"
echo "KEY_PASSWORD: $KEYPASS"
echo ""
echo "=== Добавьте в GitHub: ==="
echo "gh secret set KEYSTORE_BASE64   # вставьте base64 строку выше"
echo "gh secret set KEYSTORE_PASSWORD # вставьте пароль"
echo "gh secret set KEY_ALIAS         # вставьте alias"
echo "gh secret set KEY_PASSWORD      # вставьте пароль ключа"
echo ""
echo "⚠️  ВАЖНО: Сохраните $KEYSTORE_FILE и пароли в безопасном месте!"
echo "   Без них нельзя будет обновлять приложение в Play Store."
echo "   Файл $KEYSTORE_FILE уже в .gitignore и НЕ попадёт в git."