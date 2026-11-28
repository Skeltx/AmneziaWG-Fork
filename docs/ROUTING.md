# Multi-tunnel routing — рабочий журнал

Ветка: `feat/multi-tunnel-routing`

## Принятые решения

- Движок: **sing-box-lx 1.14.4-lx.37** (`libbox.aar`), поддержка AmneziaWG 1.x / 2.0 / 3.x.
- Один Android `VpnService`, внутри — несколько WireGuard/AmneziaWG endpoints.
- Маршрутизация: приложение / домен / IP → выбранный туннель, `direct` или `block`.
- `minSdk` поднят до **29** (Android 10) — нужен `getConnectionOwnerUid`.
- UI: пока остаётся текущий XML; редизайн на Material You — следующий этап.
- Ветка `legacy-generated` удалена локально и в `origin` (tip: `50bfd56`).
- Лицензия проекта станет **GPL-3.0** из-за sing-box-lx; `COPYING`/README ещё не обновлены.

## Что уже сделано

- `ui/libs/libbox.aar` скачивается скриптами `scripts/fetch-libbox.{sh,ps1}` (AAR не в git).
- `ui/build.gradle.kts`: зависимость libbox, ABI-фильтр, proguard, `useLegacyPackaging`.
- `tunnel` больше не собирает старые wg-go/wg-quick JNI-библиотеки.
- Новый слой `org.amnezia.awg.engine`:
  - `SingBoxService` — `VpnService` + `PlatformInterface` + `CommandServerHandler`.
  - `SingBoxBackend` — реализация `Backend`, держит несколько активных туннелей.
  - `SingBoxConfigBuilder` — генерация JSON sing-box из `Config` + правил.
  - `RoutingRules` — модель и JSON-хранилище правил (`filesDir/routing.json`).
  - `EngineController` — запуск/остановка/перезагрузка сервиса.
- `Application` инициализирует `Libbox.setup(...)` и использует `SingBoxBackend`.
- В манифест добавлены сервис, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`,
  `CHANGE_NETWORK_STATE`.
- Старые per-app include/exclude из `.conf` автоматически превращаются в правила
  маршрутизации, поэтому текущий редактор туннеля продолжает работать.
- Юнит-тест `SingBoxConfigBuilderTest` проходит.
- Сгенерированный JSON проверен `sing-box check` (1.14.4-lx.37) — OK.
- `:ui:assembleDebug` локально собирается (APK ~109 MB, все ABI).

## Важные ограничения / что проверить на устройстве

1. Устройства/эмулятора в сессии не было — runtime не проверен. В первую очередь
   проверить: `Libbox.setup`, `CommandServer.start()`, `openTun()`, `getConnectionOwnerUid`,
   запуск двух туннелей, per-app правила.
2. `getConnectionOwnerUid` может кидать `SecurityException` на части OEM — сейчас
   возвращается `INVALID_UID`, правило по пакету просто не сработает.
3. DNS сейчас — DoH `1.1.1.1` через `direct`; при необходимости позже добавить
   DNS-правила и DNS через туннель.
4. `Statistics` и `getLastHandshake` пока пустые (0), UI показывает нули/connecting.
5. При включении одного туннеля без правил весь трафик идёт через него (совместимость
   со старым поведением). При наличии правил по умолчанию — `direct`.
6. Android не разрешает одновременно `include_package` и `exclude_package`; билдер
   выбирает что-то одно.
7. Release-подпись/CI: `build.yml` и `ci.yml` уже качают libbox; ветка `feat/**`
   добавлена в триггеры CI.

## Следующие шаги

1. Подключить устройство/эмулятор, поставить debug APK, проверить запуск 1 и 2 туннелей.
2. Починить runtime-ошибки (логи `adb logcat -s AmneziaWG/SingBox AmneziaWG/Engine`).
3. Экран «Маршрутизация»: список правил, выбор приложения/домена, выбор туннеля.
4. Material You редизайн главного экрана и списка туннелей.
5. Обновить лицензию на GPL-3.0 и README.
6. Добавить per-tunnel статистику/хендшейк из libbox `CommandClient`.

## Локальная сборка

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.21.10002-hotspot'
$env:ANDROID_HOME='D:\Android\Sdk'
.\scripts\fetch-libbox.ps1
.\gradlew.bat :ui:assembleDebug
```

Проверка конфига:

```powershell
sing-box.exe check -c ui\build\sample-singbox.json
```
