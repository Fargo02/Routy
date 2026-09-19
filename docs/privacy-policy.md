---
title: Privacy Policy — Routy
---

# Privacy Policy — Routy

**App:** Routy (Batumi public transport)
**Package name:** `ge.routy.transport`
**Developer:** Fargo02 ([github.com/Fargo02/Routy](https://github.com/Fargo02/Routy))
**Contact:** `trenin.work@gmail.compf`
**Effective date:** 19 September 2026
**Last updated:** 19 September 2026

[Русская версия ниже](#политика-конфиденциальности--routy)

## Short version

Routy has no accounts, no ads and no tracking. It does not ask for your name,
phone number or e-mail. Your location never leaves your device. The only data
sent off the device is anonymous crash diagnostics on Android and the ordinary
network requests needed to load transport data and map tiles.

## What data the app collects

| Data | Collected | Leaves the device | Purpose |
| --- | --- | --- | --- |
| Account or profile data (name, e-mail, phone) | No | — | The app has no sign-in |
| Precise / approximate location | Only while the app is open, and only after you tap **My location** | No | Centring the map on your position and showing nearby stops |
| Favourite routes and stops | Yes, on the device | No | Your saved list |
| Language, theme and map appearance | Yes, on the device | No | Remembering your settings |
| Cached transport data (routes, stops, timetables, geometry) | Yes, on the device | No | Offline start-up and faster loading |
| Crash diagnostics (Android) | Yes | Yes, to Google | Fixing crashes |
| Advertising identifiers | No | — | The app shows no ads |

### Location

Location is optional. The app works fully without it — you can browse routes,
stops, timetables and live vehicles with the permission denied.

The permission (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` on Android;
`NSLocationWhenInUseUsageDescription` on iOS) is requested only when you tap
**My location**, and only while the app is in the foreground. The coordinates
are used by the map on your device and are **not** transmitted to us, not stored
in any file and not shared with any third party. There is no background location
collection and no location history.

### Crash diagnostics

The Android build includes **Firebase Crashlytics** (Google). When the app
crashes, Crashlytics sends Google a crash report containing the stack trace, the
device model, the operating-system and app version, the amount of free memory
and storage at the time of the crash, and a randomly generated Crashlytics
installation identifier. This report contains no name, no e-mail and no
location. It is used only to find and fix defects.

Google acts as our processor for this data. See the
[Firebase privacy documentation](https://firebase.google.com/support/privacy)
and the [Google Privacy Policy](https://policies.google.com/privacy).

The iOS build contains no crash-reporting SDK.

Independently of the app, Google Play collects its own diagnostics (Android
Vitals) for apps installed from the store.

## Third-party services the app contacts

Using the app means your device makes direct HTTPS requests to the services
below. Each of them necessarily receives your IP address and standard request
metadata; none of them receives your location or an identifier created by us.

| Service | What is requested | Provider policy |
| --- | --- | --- |
| ThetaMaps API (`thetamaps.site`) | Routes, stops, timetables, live vehicle positions | Operated by the transport data provider |
| OpenFreeMap (`tiles.openfreemap.org`) | Map tiles and styles | [openfreemap.org](https://openfreemap.org/) |
| Google Translate endpoint (`clients5.google.com`) | Transport names sent for translation into the interface language | [policies.google.com/privacy](https://policies.google.com/privacy) |
| Firebase Crashlytics (Android only) | Crash reports, as described above | [firebase.google.com/support/privacy](https://firebase.google.com/support/privacy) |

Only public transport names — stop and route labels coming from the transport
database — are sent to the translation endpoint. Nothing you type and nothing
about you is sent there.

## What the app does not do

- No user accounts, registration or sign-in.
- No advertising, no advertising ID, no marketing profiling.
- No analytics of your behaviour inside the app.
- No sale or rental of data to anyone.
- No access to contacts, photos, files, microphone, camera or call history.
- No collection of data from children specifically; the app is not directed at
  children and collects nothing that would identify anyone.

## Storage and retention

Favourites, settings and the cached transport snapshot are stored only in the
app's private storage on your device. Deleting the app removes them. Clearing
the app's data in system settings has the same effect.

Crash reports are retained by Google under Firebase's retention policy
(currently 90 days for crash data).

## Your rights

Because the app stores no personal data on any server we control, there is
nothing on our side to export or correct. You can:

- revoke the location permission at any time in system settings;
- delete all local data by clearing the app's storage or uninstalling it;
- write to the contact address above with questions about crash diagnostics,
  including a request to delete a report.

## Permissions used

| Permission | Why |
| --- | --- |
| `INTERNET` | Loading transport data and map tiles |
| `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` | Optional; showing your position on the map |

## Changes to this policy

If the app starts collecting anything not described here, this document is
updated before the change ships, and the effective date at the top changes with
it. The revision history of this file is public in the repository.

## Contact

Questions about this policy: `TODO: contact@example.com`

---

# Политика конфиденциальности — Routy

**Приложение:** Routy (общественный транспорт Батуми)
**Имя пакета:** `ge.routy.transport`
**Разработчик:** Fargo02 ([github.com/Fargo02/Routy](https://github.com/Fargo02/Routy))
**Контакт:** `TODO: contact@example.com`
**Дата вступления в силу:** 19 сентября 2026 г.
**Последнее обновление:** 19 сентября 2026 г.

## Коротко

В Routy нет аккаунтов, рекламы и слежки. Приложение не спрашивает имя, телефон
или e-mail. Ваша геопозиция не покидает устройство. За пределы устройства
уходят только анонимные отчёты о сбоях на Android и обычные сетевые запросы,
нужные для загрузки данных о транспорте и карты.

## Какие данные собирает приложение

| Данные | Собираются | Покидают устройство | Зачем |
| --- | --- | --- | --- |
| Учётная запись (имя, e-mail, телефон) | Нет | — | В приложении нет входа |
| Точная / приблизительная геопозиция | Только при открытом приложении и только после нажатия **Моё местоположение** | Нет | Центрирование карты и показ ближайших остановок |
| Избранные маршруты и остановки | Да, на устройстве | Нет | Ваш сохранённый список |
| Язык, тема и оформление карты | Да, на устройстве | Нет | Сохранение настроек |
| Кэш данных транспорта (маршруты, остановки, расписания, геометрия) | Да, на устройстве | Нет | Офлайн-запуск и быстрая загрузка |
| Диагностика сбоев (Android) | Да | Да, в Google | Исправление сбоев |
| Рекламные идентификаторы | Нет | — | Рекламы в приложении нет |

### Геопозиция

Геопозиция необязательна. Без неё приложение работает полностью: маршруты,
остановки, расписания и движение транспорта доступны при отклонённом разрешении.

Разрешение (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` на Android;
`NSLocationWhenInUseUsageDescription` на iOS) запрашивается только при нажатии
**Моё местоположение** и только пока приложение на переднем плане. Координаты
используются картой на вашем устройстве, **не** передаются нам, не сохраняются
в файлы и не передаются третьим лицам. Фонового сбора геопозиции и истории
перемещений нет.

### Диагностика сбоев

В Android-сборку встроен **Firebase Crashlytics** (Google). При аварийном
завершении приложения Crashlytics отправляет в Google отчёт со стеком вызовов,
моделью устройства, версией ОС и приложения, объёмом свободной памяти и
хранилища на момент сбоя, а также случайно сгенерированным идентификатором
установки Crashlytics. В отчёте нет имени, e-mail и геопозиции. Отчёты
используются только для поиска и устранения дефектов.

Google выступает обработчиком этих данных: см.
[документацию Firebase о конфиденциальности](https://firebase.google.com/support/privacy)
и [Политику конфиденциальности Google](https://policies.google.com/privacy).

В iOS-сборке SDK отчётов о сбоях отсутствует.

Независимо от приложения собственную диагностику (Android Vitals) собирает
Google Play для приложений, установленных из магазина.

## Сторонние сервисы, к которым обращается приложение

При работе приложения ваше устройство напрямую делает HTTPS-запросы к сервисам
ниже. Каждый из них неизбежно получает ваш IP-адрес и обычные метаданные
запроса; ни один не получает вашу геопозицию и созданные нами идентификаторы.

| Сервис | Что запрашивается | Политика провайдера |
| --- | --- | --- |
| ThetaMaps API (`thetamaps.site`) | Маршруты, остановки, расписания, позиции транспорта | Поставщик данных о транспорте |
| OpenFreeMap (`tiles.openfreemap.org`) | Тайлы и стили карты | [openfreemap.org](https://openfreemap.org/) |
| Google Translate (`clients5.google.com`) | Названия транспорта для перевода на язык интерфейса | [policies.google.com/privacy](https://policies.google.com/privacy) |
| Firebase Crashlytics (только Android) | Отчёты о сбоях, как описано выше | [firebase.google.com/support/privacy](https://firebase.google.com/support/privacy) |

На сервис перевода отправляются только публичные названия из транспортной базы
— имена остановок и маршрутов. Введённый вами текст и данные о вас туда не
попадают.

## Чего приложение не делает

- Нет учётных записей, регистрации и входа.
- Нет рекламы, рекламного идентификатора и маркетингового профилирования.
- Нет аналитики вашего поведения внутри приложения.
- Данные никому не продаются и не сдаются в аренду.
- Нет доступа к контактам, фотографиям, файлам, микрофону, камере и звонкам.
- Приложение не предназначено для детей и не собирает данных, позволяющих
  идентифицировать человека.

## Хранение и сроки

Избранное, настройки и кэш транспорта хранятся только в приватном хранилище
приложения на вашем устройстве. Удаление приложения удаляет их; очистка данных
приложения в системных настройках даёт тот же результат.

Отчёты о сбоях хранятся Google по правилам Firebase (сейчас — 90 дней для
данных о сбоях).

## Ваши права

Поскольку персональные данные не хранятся ни на одном подконтрольном нам
сервере, экспортировать или исправлять на нашей стороне нечего. Вы можете:

- в любой момент отозвать разрешение на геопозицию в системных настройках;
- удалить все локальные данные, очистив хранилище приложения или удалив его;
- написать на контактный адрес выше по вопросам диагностики сбоев, включая
  запрос на удаление отчёта.

## Используемые разрешения

| Разрешение | Зачем |
| --- | --- |
| `INTERNET` | Загрузка данных о транспорте и карты |
| `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` | Необязательно; показ вашей позиции на карте |

## Изменения политики

Если приложение начнёт собирать что-то, не описанное здесь, документ будет
обновлён до выхода такой версии, вместе с датой вступления в силу. История
правок файла публична в репозитории.

## Контакты

Вопросы по политике: `TODO: contact@example.com`
