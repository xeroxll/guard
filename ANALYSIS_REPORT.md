# 📱 Полный анализ приложения Guardian v1.0.0

---

## 1. Общая информация

| Параметр | Значение |
|----------|----------|
| **Название** | Guardian |
| **Версия** | 1.0.0 |
| **Тип** | Android-приложение для обеспечения безопасности |
| **Min SDK** | 26 (Android 8.0 Oreo) |
| **Target SDK** | 34 (Android 14) |
| **Лицензия** | MIT |
| **Репозиторий** | https://github.com/xeroxll/guard |

---

## 2. Технический стек

| Компонент | Технология | Версия |
|-----------|------------|--------|
| **Язык** | Kotlin | 1.9.21 |
| **UI Framework** | Jetpack Compose | BOM 2024.02.00 |
| **Архитектура** | MVVM + Clean Architecture | - |
| **Навигация** | Navigation Compose | 2.7.7 |
| **Стейт-менеджмент** | Kotlin Flows + StateFlow | - |
| **Локальное хранилище** | DataStore Preferences | 1.0.0 |
| **Сеть** | Retrofit + OkHttp | 2.9.0 / 4.12.0 |
| **Сборка** | Gradle | 8.2.0 |
| **JDK** | Java | 17 |

---

## 3. Архитектура приложения

### 3.1 Структура проекта

```
com.guardian.app/
├── GuardianApp.kt              # Application class (создание каналов уведомлений)
├── admin/
│   └── GuardianDeviceAdminReceiver.kt  # Device Admin (не используется)
├── data/
│   ├── api/
│   │   ├── VirusTotalApi.kt   # Retrofit API интерфейс
│   │   └── VirusTotalService.kt # Сервис для VT проверок
│   ├── model/
│   │   ├── Models.kt          # Data classes
│   │   └── VirusTotalModels.kt # VT модели ответа
│   ├── notification/
│   │   └── NotificationHelper.kt # Утилиты для уведомлений
│   └── repository/
│       └── GuardianRepository.kt # DataStore repository
├── receiver/
│   └── Receivers.kt           # BroadcastReceivers
├── service/
│   └── GuardianMonitorService.kt # Foreground Service
└── ui/
    ├── MainActivity.kt        # Главная Activity
    ├── navigation/
    │   └── Navigation.kt     # Навигация
    ├── screens/
    │   ├── HomeScreen.kt      # Главный экран
    │   ├── ScanScreen.kt      # Экран сканирования
    │   ├── SettingsScreen.kt  # Настройки
    │   ├── EventsScreen.kt    # Журнал событий
    │   └── TrustedAppsScreen.kt # Доверенные приложения
    ├── theme/
    │   ├── Color.kt          # Цветовая палитра
    │   └── Type.kt           # Типографика
    └── viewmodel/
        └── GuardianViewModel.kt # ViewModel
```

### 3.2 Паттерн MVVM

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│      View      │────▶│   ViewModel     │────▶│     Model       │
│  (Compose UI)  │◀────│ (StateFlow/Flow)│◀────│ (Repository)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
   HomeScreen            GuardianViewModel       DataStore
   SettingsScreen       (UI State)          GuardianRepository
   ScanScreen                                    (Data)
```

---

## 4. Функциональные возможности

### 4.1 Мониторинг приложений (App Monitor) ✅

| Функция | Описание |
|---------|----------|
| **Отслеживание установки** | BroadcastReceiver реагирует на `PACKAGE_ADDED` |
| **Отслеживание удаления** | BroadcastReceiver реагирует на `PACKAGE_REMOVED` |
| **Сканирование при установке** | Автоматический анализ новых приложений |
| **Проверка обновлений** | Анализ изменений разрешений при обновлении |
| **Blacklist** | Ручное добавление приложений в чёрный список |
| **Whitelist** | Доверенные приложения (30+ предустановленных) |

### 4.2 USB мониторинг ✅

| Функция | Описание |
|---------|----------|
| **ADB мониторинг** | Отслеживание включения USB-отладки |
| **Периодическая проверка** | Проверка каждые 5 секунд |
| **Уведомление при подключении** | Оповещение при USB подключении с ADB |
| **Обнаружение криминалистических инструментов** | Проверка на приложения: Cellebrite, MSAB XRY, Oxygen и др. |

### 4.3 Сканирование устройства ✅

| Функция | Описание |
|---------|----------|
| **Локальное сканирование** | Анализ всех установленных приложений |
| **VirusTotal интеграция** | Проверка через VT API (требуется ключ) |
| **Сканирование APK** | Анализ APK файлов |
| **История сканирований** | Последние 50 сканирований |

### 4.4 Система анализа угроз ⚠️

**Встроенные сигнатуры вредоносного ПО (28 шт.):**
```
trojan(10), backdoor(10), rat(9), spyware(9), keylog(10),
stealer(9), ransomware(10), banker(10), bankbot(10), smsfraud(9),
miner(8), dropper(9), rootkit(10), botnet(10), worm(9),
pegasus(10), anubis(10), cerberus(10), eventbot(10), sharkbot(10)
```

**Опасные разрешения (12 шт.):**
```
BIND_ACCESSIBILITY_SERVICE(5), SEND_SMS(4), PROCESS_OUTGOING_CALLS(4),
READ_SMS(3), CALL_PHONE(3), READ_CALL_LOG(3), RECORD_AUDIO(3),
SYSTEM_ALERT_WINDOW(3), READ_CONTACTS(2), CAMERA(2), ACCESS_FINE_LOCATION(2)
```

### 4.5 Интерфейс пользователя ✅

| Экран | Описание |
|-------|----------|
| **Главный экран** | Статус защиты, щит, статистика, последние события |
| **Сканирование** | Полное сканирование устройства, результаты |
| **События** | Журнал всех событий безопасности |
| **Настройки** | Управление защитой, темой, VirusTotal API |
| **Доверенные приложения** | Управление whitelist |

---

## 5. Компоненты системы

### 5.1 Foreground Service

```
GuardianMonitorService
├── Уведомление о мониторинге (ongoing)
├── BroadcastReceiver для PACKAGE_ADDED/REMOVED
├── SecurityScanner - анализ угроз
└── Уведомления о найденных угрозах
```

### 5.2 Broadcast Receivers

| Receiver | События | Назначение |
|----------|---------|------------|
| AppMonitorReceiver | PACKAGE_ADDED, PACKAGE_REMOVED | Мониторинг приложений |
| UsbMonitorReceiver | BOOT_COMPLETED, USB_STATE, POWER_CONNECTED | USB/ADB мониторинг |

### 5.3 Уведомления

| Канал | Важность | Назначение |
|-------|----------|------------|
| alerts | HIGH | Важные уведомления безопасности |
| blocked | DEFAULT | Заблокированные приложения |
| monitoring | LOW | Фоновый мониторинг (постоянное) |

---

## 6. Разрешения приложения

### 6.1 Обязательные разрешения

| Разрешение | Назначение | Статус |
|------------|------------|--------|
| INTERNET | Для VirusTotal API | ✅ |
| POST_NOTIFICATIONS | Уведомления | ✅ |
| RECEIVE_BOOT_COMPLETED | Автозапуск | ✅ |
| QUERY_ALL_PACKAGES | Список приложений | ✅ |
| PACKAGE_USAGE_STATS | Статистика | ✅ |
| FOREGROUND_SERVICE | Фоновый сервис | ✅ |
| FOREGROUND_SERVICE_SPECIAL_USE | Special Use | ✅ |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | Непрерывная работа | ✅ |
| SYSTEM_ALERT_WINDOW | Surface overlay | ✅ |

### 6.2 Удалённые разрешения (Google Play)

| Разрешение | Причина удаления |
|------------|------------------|
| RECEIVE_SMS | Удалено |
| READ_SMS | Удалено |
| READ_PHONE_STATE | Удалено |
| READ_CALL_LOG | Удалено |
| READ_CONTACTS | Удалено |

---

## 7. Поток данных

```
┌─────────────────┐
│   App Install   │
│     Event       │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────┐
│   GuardianMonitorService        │
│   (Foreground Service)          │
└────────┬───────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│   SecurityScanner.scan()        │
│   • Whitelist check             │
│   • Malware signatures          │
│   • Permission analysis         │
│   • System app check           │
└────────┬───────────────────────┘
         │
    ┌────┴────┐
    ▼         ▼
Threat   Safe
    │         │
    ▼         ▼
┌─────────────────────────────┐
│   Notification + Event Log  │
│   (DataStore)               │
└─────────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│   UI (Compose Screens)         │
└─────────────────────────────────┘
```

---

## 8. Оценка функциональности

| Категория | Функция | Статус |
|-----------|---------|--------|
| Мониторинг | USB/ADB мониторинг | ✅ |
| Мониторинг | Мониторинг приложений | ✅ |
| Сканирование | Локальное сканирование | ✅ |
| Сканирование | VirusTotal API | ✅ |
| Сканирование | Сканирование APK | ✅ |
| UI | Тёмная/светлая тема | ✅ |
| UI | Material Design 3 | ✅ |
| Данные | История сканирований | ✅ |
| Данные | Журнал событий | ✅ |
| Данные | Blacklist/Whitelist | ✅ |

---

## 9. Сильные стороны

1. ✅ **Современная архитектура** - MVVM, Clean Architecture, Kotlin Coroutines
2. ✅ **Jetpack Compose** - Современный UI с Material Design 3
3. ✅ **Foreground Service** - Надёжный мониторинг в фоне
4. ✅ **Встроенный сканер** - Локальный анализ без интернета
5. ✅ **VirusTotal** - Облачная проверка (опционально)
6. ✅ **ADB мониторинг** - Защита от эксплойтов
7. ✅ **Google Play совместимость** - Удалены чувствительные разрешения

---

## 10. Ограничения и рекомендации

### 10.1 Текущие ограничения

| Ограничение | Описание |
|------------|----------|
| ❌ Нет блокировки приложений | Только уведомления |
| ❌ Нет anti-theft | Нет геолокации/блокировки |
| ❌ Нет VPN/firewall | Только мониторинг |

### 10.2 Рекомендации по улучшению

1. **Безопасность хранилища** - API ключ VirusTotal хранится в открытом виде
2. **Оптимизация сканирования** - Кэширование результатов
3. **Дополнительные функции** - Anti-theft, App locking

---

## 11. Совместимость

| Параметр | Значение |
|----------|----------|
| Google Play | ✅ Совместимо |
| Android 8.0+ | ✅ minSdk 26 |
| Android 14 | ✅ targetSdk 34 |

---

## 12. Итоговая оценка

| Критерий | Оценка |
|----------|--------|
| Функциональность | ⭐⭐⭐⭐ (4/5) |
| Архитектура | ⭐⭐⭐⭐⭐ (5/5) |
| Качество кода | ⭐⭐⭐⭐ (4/5) |
| Документация | ⭐⭐⭐ (3/5) |
| Google Play | ⭐⭐⭐⭐⭐ (5/5) |

---

## Заключение

Guardian — это современное Android-приложение для обеспечения безопасности с чистой архитектурой на Kotlin и Jetpack Compose. После удаления функций SMS/Call фильтрации оно полностью соответствует политике Google Play и готово к публикации.

Основные сильные стороны: фоновый мониторинг, встроенный сканер угроз, интеграция с VirusTotal и красивый UI с поддержкой темной темы. Приложение эффективно выполняет базовые функции безопасности: мониторинг USB-отладки, отслеживание установки приложений и проверку на вредоносное ПО.

---

*Отчёт составлен: $(date)*
*Версия приложения: 1.0.0*
*Источник: https://github.com/xeroxll/guard*
