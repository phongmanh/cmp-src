# core:utils

Platform helpers with no domain meaning: number, date and byte-size formatting in the device's
locale and time zone, and a runtime-permission controller. Depends on nothing in the project;
`androidx.activity.compose` on Android. A permission still has to be declared by the app (manifest
entry, `Info.plist` usage key) — none are yet.

## Classes

```mermaid
classDiagram
    direction TB
    class NumberFormat {
        <<expect/actual>>
        +DEFAULT_FRACTION_DIGITS = 2
        +formatDecimal(value: Double, maxFractionDigits: Int) String
    }
    class DateFormat {
        <<expect/actual>>
        +formatDate(epochMillis: Long, style: FormatStyle) String
        +formatDateTime(epochMillis: Long, dateStyle: FormatStyle, timeStyle: FormatStyle) String
    }
    class FormatStyle {
        <<enumeration>>
        SHORT
        MEDIUM
        LONG
    }
    class ByteSize {
        <<data>>
        +value: Double
        +unit: ByteUnit
        +Long.toByteSize()$ ByteSize
        +Long.formatByteSize(maxFractionDigits: Int)$ String
    }
    class ByteUnit {
        <<enumeration>>
        B
        KB
        MB
        GB
        TB
        +symbol: String
    }
    class PermissionController {
        <<interface>>
        +status(permission: Permission) PermissionStatus
        +request(permission: Permission) PermissionStatus
        +openAppSettings()
    }
    class rememberPermissionController {
        <<expect composable>>
        +rememberPermissionController() PermissionController
    }
    class AndroidPermissionController {
        <<androidMain, private>>
        Activity result launcher
        PendingPermissionRequest : one dialog at a time, process-wide
    }
    class IosPermissionController {
        <<iosMain, private>>
        AVCaptureDevice, CLLocationManager, UNUserNotificationCenter
        never reports DENIED
    }
    class Permission {
        <<enumeration>>
        CAMERA
        MICROPHONE
        LOCATION
        NOTIFICATIONS
    }
    class PermissionStatus {
        <<enumeration>>
        GRANTED
        NOT_DETERMINED
        DENIED
        DENIED_ALWAYS
    }

    DateFormat --> FormatStyle : uses
    ByteSize --> ByteUnit : unit
    ByteSize --> NumberFormat : formats value with
    rememberPermissionController ..> PermissionController : returns
    AndroidPermissionController ..|> PermissionController : implements
    IosPermissionController ..|> PermissionController : implements
    PermissionController --> Permission : takes
    PermissionController ..> PermissionStatus : returns
```

## Sequences

### Request a permission

```mermaid
sequenceDiagram
    participant C as Caller (a screen's coroutine)
    participant PC as PermissionController
    participant OS as System prompt
    C->>PC: status(permission)
    PC-->>C: PermissionStatus
    alt GRANTED
        Note over C: proceed
    else NOT_DETERMINED or DENIED
        C->>PC: request(permission)
        PC->>OS: show prompt (Android: one at a time, survives Activity recreation)
        OS-->>PC: answer
        PC-->>C: GRANTED, DENIED or DENIED_ALWAYS
    else DENIED_ALWAYS
        C->>PC: openAppSettings()
    end
```
