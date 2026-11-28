# Multi-tunnel routing — рабочий журнал

Ветка: `feat/multi-tunnel-routing`

## Принятые решения

- Движок: **sing-box-lx 1.14.4-lx.37** (`libbox.aar`), поддержка AmneziaWG 1.x / 2.0 / 3.x.
- Один Android `VpnService`, внутри — несколько WireGuard/AmneziaWG endpoints.
- Маршрутизация: приложение / домен / IP → выбранный туннель, `direct` или `block`.
- `minSdk` поднят до **29** (Android 10) — нужен `getConnectionOwnerUid`.
- UI: текущий XML/Material 3 + новый экран «Маршрутизация».
- Ветка `legacy-generated` удалена локально и в `origin` (tip: `50bfd56`).
- Лицензия форка — **GPL-3.0**; исходники AmneziaWG остаются Apache-2.0
  (`LICENSE-APACHE-2.0`), третий стороны — `NOTICE`.

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
- Экран «Маршрутизация»: правила, выбор приложения, домена, IP/CIDR, туннеля,
  маршрут по умолчанию.
- Главный экран переработан: заголовок, кнопка Routing, карточки туннелей с
  иконкой, статусом и switch, extended FAB «Добавить туннель».
- Экран деталей: hero-карточка с именем туннеля, статусом, switch и кнопкой Routing,
  секция «AmneziaWG parameters».
- Bottom sheet «Добавить туннель» получил заголовок и подпись.
- Проверены светлая и тёмная темы (Material You/dynamic colors).
- Старые per-app include/exclude из `.conf` автоматически превращаются в правила
  маршрутизации.
- Исправлены все вызовы `VpnService.prepare` (раньше использовался старый `GoBackend`).
- Если в конфиге нет IPv6-адреса, IPv6 отключается на TUN и в endpoint, а IPv6-трафик
  отклоняется (`ip_version=6` + `reject`), вместо ошибки
  `missing IPv6 local address`.
- `getStatistics`/`getLastHandshake` пока возвращают пустые значения.

## Проверено на эмуляторе Android 15 (API 35, x86_64)

- Приложение запускается, `libbox.so` загружается, `Libbox.setup` проходит.
- Один туннель: TUN `tun0`, DNS hijack, `sing-box started`.
- Два туннеля одновременно: `tunnel_home` и `tunnel_work`, оба endpoint подняты.
- Per-app routing:
  - `com.android.chrome` → `tunnel_home`;
  - `com.google.android.gms` → `tunnel_work`;
  - остальное → `direct`.
- Domain routing: правило `example.org` добавлено через UI, сохранилось в
  `routing.json`, применилось после reload.
- Выбор приложения через UI: `AppListDialogFragment` заполняет пакет в поле цели.
- IPv4-only профиль: TUN без IPv6, IPv6 reject, ошибок `missing IPv6 local address` нет.
- Остановка последнего туннеля: сервис останавливается, VPN-интерфейс исчезает.
- Release APK после R8: `libbox.so` загружается, VPN поднимается, per-app
  `include_package` содержит только UID Chrome/GMS (проверено через `dumpsys connectivity`).
- `sing-box check` для сгенерированного JSON — OK.
- `:ui:testDebugUnitTest`, `:ui:lintDebug`, `:ui:assembleDebug`, `:ui:assembleRelease` — OK.

## Ограничения / что проверить дальше

1. Реальный handshake с AmneziaWG-сервером не проверялся: в репозитории нет тестовых
   учётных данных. На эмуляторе endpoint указывал на TEST-NET (192.0.2.x), поэтому
   handshake ожидаемо не завершался.
2. `getConnectionOwnerUid` может кидать `SecurityException` на части OEM — сейчас
   возвращается `INVALID_UID`, правило по пакету не сработает.
3. DNS — DoH `1.1.1.1`; DNS-правила и DNS через туннель пока не настраиваются.
4. `Statistics` и `getLastHandshake` пустые; статус «подключено» выставляется после
   запуска движка, а не после handshake.
5. Android не разрешает одновременно `include_package` и `exclude_package`; билдер
   выбирает что-то одно.
6. APK пока universal (~111 MB debug, ~88 MB release); ABI-splits не включены.
7. При включении одного туннеля без правил весь трафик идёт через него; при наличии
   правил по умолчанию — `direct`.

## Следующие шаги

1. Проверить реальный AmneziaWG 1.x/2.0/3.x сервер, handshake и передачу трафика.
2. Добавить реальный статус/handshake/статистику через libbox `CommandClient`.
3. Добавить DNS-правила и опцию «DNS через туннель».
4. ABI-splits и уменьшение APK.
5. Материальный редизайн главного экрана и списка туннелей.
6. Тесты на импорт конфигов и миграцию старых per-app списков.

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
