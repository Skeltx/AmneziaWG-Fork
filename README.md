# AmneziaWG Fork — WireGuard Client with Per-App & Per-Domain Routing

Полнофункциональный Android-клиент WireGuard с продвинутым контролем маршрутизации (split tunneling).

## 🎯 Ключевые особенности

### Полный контроль маршрутизации
- **1 приложение = 1 конфиг** — назначьте отдельный WireGuard-конфиг конкретному приложению
- **1 сайт/домен = 1 конфиг** — маршрутизируйте трафик конкретных доменов через выбранный конфиг
- **Гибкие правила** — комбинируйте правила по пакетам, доменам, IP/CIDR, UID процесса
- **Приоритеты** — правила с более высоким приоритетом переопределяют низкоприоритетные

### Действия для каждого правила
- 🔒 **Через VPN** — весь трафик идет через WireGuard туннель
- 🌐 **Напрямую** — трафик обходит VPN (split tunneling)
- 🚫 **Блокировать** — полная блокировка доступа
- 🔄 **Через другой конфиг** — перенаправление на альтернативный WireGuard сервер

### Управление конфигурациями
- Неограниченное количество профилей WireGuard
- Быстрое переключение между конфигами
- Импорт/экспорт конфигураций
- Авто-подключение при загрузке системы

### Безопасность и приватность
- Биометрическая блокировка приложения
- Always-on VPN (системный)
- Блокировка трафика при отключенном VPN
- Локальное хранение ключей (без облака)

## 📱 Скриншоты

*Скриншоты будут добавлены после первой сборки*

## 🛠 Технологический стек

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Hilt** для Dependency Injection
- **Room** для локальной БД
- **DataStore** для настроек
- **WireGuard Android Library** (Go backend)
- **Coroutines/Flow** для асинхронности
- **KSP** для кодогенерации

## 📋 Требования

- Android 7.0 (API 24) или выше
- Root не требуется
- Разрешение на создание VPN-соединения

## 🚀 Сборка

```bash
# Клонирование
git clone https://github.com/yourusername/AmneziaWG-Fork.git
cd AmneziaWG-Fork

# Сборка debug APK
./gradlew assembleDebug

# Сборка release APK (требует настроенного keystore)
./gradlew assembleRelease
```

### Переменные окружения для Release сборки
```bash
export KEYSTORE_PATH=/path/to/keystore.jks
export KEYSTORE_PASSWORD=your_keystore_password
export KEY_ALIAS=your_key_alias
export KEY_PASSWORD=your_key_password
```

## 📂 Структура проекта

```
app/
├── src/main/
│   ├── java/com/amneziawg/fork/
│   │   ├── model/           # Модели данных (RoutingRule, RoutingConfig, WireGuardConfig)
│   │   ├── db/              # Room Database (Entity, Dao, Database)
│   │   ├── vpn/             # VPN Service, RoutingEngine
│   │   ├── di/              # Hilt Modules
│   │   ├── ui/
│   │   │   ├── main/        # MainActivity, MainViewModel
│   │   │   ├── config/      # ConfigEditorActivity, ViewModel
│   │   │   ├── settings/    # SettingsActivity, ViewModel
│   │   │   └── theme/       # Material3 Theme
│   │   └── receiver/        # Broadcast Receivers
│   ├── res/
│   │   ├── values/          # Strings, Colors, Themes
│   │   └── xml/             # Backup rules, Data extraction
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## 🔧 Как работает маршрутизация

### RoutingEngine
Центральный компонент, принимающий решения о маршрутизации каждого пакета/соединения:

1. **Проверка пакета приложения** — если приложение имеет назначенный конфиг или правила
2. **Проверка UID процесса** — правила для конкретных UID
3. **Проверка домена** — точные совпадения, regex, наследование поддоменов
4. **Проверка IP/CIDR** — совпадение с диапазонами адресов
5. **Действие по умолчанию** — из настроек активного конфига

### Кэширование решений
Для производительности решения кэшируются с TTL и очисткой старых записей.

## 📄 Лицензия

Этот проект является форком AmneziaWG и распространяется под лицензией GPL-3.0.

## 🤝 Вклад в проект

1. Fork репозитория
2. Создайте feature branch (`git checkout -b feature/amazing-feature`)
3. Commit изменения (`git commit -m 'Add amazing feature'`)
4. Push в branch (`git push origin feature/amazing-feature`)
5. Откройте Pull Request

## 🙏 Благодарности

- [WireGuard](https://www.wireguard.com/) — современный VPN протокол
- [AmneziaVPN](https://amnezia.org/) — вдохновение для проекта
- [WireGuard Android](https://gitlab.com/wireguard-android/wireguard-android) — библиотека для Android

---

**Сделано с ❤️ для свободного интернета**