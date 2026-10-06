# core:database

Room 3 on SQLite (bundled driver): the encrypted session row behind `RoomTokenStore`, and the
`customers` table with its offline-sync bookkeeping. Exposes `core:network` and `core:security`
through `api`. Schema version 4, exported to `core/database/schemas/` and migrated, never wiped,
past version 2. `shared` builds the database per platform and binds the DAOs in Koin.

## Classes

```mermaid
classDiagram
    direction TB
    class AppDatabase {
        <<abstract>>
        +tokenStoreDto: TokenStoreDto
        +customerDAO: CustomerDao
    }
    class Database {
        <<file>>
        ~DATABASE_NAME = "app.db"
        ~DATABASE_VERSION = 4
        +getRoomDatabase(builder: RoomDatabase.Builder~AppDatabase~) AppDatabase
        +getDatabaseBuilder(context: Context) Builder — androidMain
        +getDatabaseBuilder() Builder — iosMain
    }
    class Migrations {
        <<file>>
        ~MIGRATION_2_3 : create customers
        ~MIGRATION_3_4 : add remoteId, syncState
        ~APP_MIGRATIONS: Array~Migration~
    }
    class TokenStoreDto {
        <<dao>>
        +tokens() Flow~EncryptedAuthTokens?~
        +save(tokens: EncryptedAuthTokens)
        +clear()
    }
    class CustomerDao {
        <<dao>>
        +customers(ownerId: String, search: String) Flow~List~Customer~~
        +customersMatching(ownerId: String, prefix: String) Flow~List~Customer~~
        +customerOf(id: String) Customer?
        +rowOf(id: String) Customer?
        +rowWithRemoteId(ownerId: String, remoteId: String) Customer?
        +pending(ownerId: String) List~Customer~
        +pendingCount(ownerId: String) Flow~Int~
        +syncedRemoteIds(ownerId: String) List~String~
        +save(customer: Customer) Long
        +updateDetails(id, firstName, ..., status: String, updatedAt: String) Int
        +softDelete(id: String, deletedAt: String)
        +purge(id: String)
        +setRemoteId(id: String, remoteId: String)
        +markRejected(id: String, expectedUpdatedAt: String)
        +acceptServerCopy(id: String, expectedUpdatedAt: String, server: Customer) — transaction
        +applyServerPage(ownerId: String, page: List~Customer~) — transaction
        +purgeSyncedMissing(ownerId: String, serverIds: Set~String~) — transaction
    }
    class EncryptedAuthTokens {
        <<entity authToken>>
        +payload: String
        +id: Int = ROW_ID
        +ROW_ID = 1$
    }
    class Customer {
        <<entity customers>>
        +id: String
        +ownerId: String
        +firstName: String?
        +lastName: String?
        +companyName: String?
        +email: String?
        +phone: String?
        +addressLine1: String?
        +addressLine2: String?
        +city: String?
        +region: String?
        +postalCode: String?
        +countryCode: String?
        +notes: String?
        +status: String
        +createdAt: String
        +updatedAt: String
        +deletedAt: String?
        +remoteId: String?
        +syncState: String = SYNCED
    }
    class CustomerSyncState {
        <<object>>
        +SYNCED$
        +PENDING$
        +REJECTED$
    }
    class RoomTokenStore {
        -dao: TokenStoreDto
        -cipher: TokenCipher
        +tokens: Flow~AuthTokens?~
        +current() AuthTokens?
        +save(tokens: AuthTokens)
        +clear()
    }
    class TokenMapping {
        <<file>>
        ~AuthTokens.encryptWith(cipher: TokenCipher) EncryptedAuthTokens
        ~EncryptedAuthTokens.decryptWith(cipher: TokenCipher) AuthTokens?
    }
    class TokenStore {
        <<core:network>>
    }
    class TokenCipher {
        <<core:security>>
    }

    AppDatabase *-- TokenStoreDto : provides
    AppDatabase *-- CustomerDao : provides
    Database ..> AppDatabase : builds
    Database --> Migrations : addMigrations
    TokenStoreDto ..> EncryptedAuthTokens : reads/writes
    CustomerDao ..> Customer : reads/writes
    Customer --> CustomerSyncState : syncState values
    RoomTokenStore ..|> TokenStore : implements
    RoomTokenStore --> TokenStoreDto : persists through
    RoomTokenStore --> TokenMapping : encrypts/decrypts with
    TokenMapping --> TokenCipher : uses
```

## Sequences

### Read the session

What the HTTP client's `loadTokens` calls. An unreadable row (lost key, older build) is cleared, so
the user is signed out rather than stuck.

```mermaid
sequenceDiagram
    participant C as Caller (HttpClient Auth plugin)
    participant TS as RoomTokenStore
    participant D as TokenStoreDto
    participant CI as TokenCipher
    C->>TS: current()
    TS->>D: tokens().first()
    alt no row
        D-->>TS: null
        TS-->>C: null
    else row
        D-->>TS: EncryptedAuthTokens(payload)
        TS->>CI: decrypt(payload)
        alt readable "access + newline + refresh"
            CI-->>TS: plaintext
            TS-->>C: AuthTokens
        else unreadable
            CI-->>TS: null
            TS->>D: clear()
            TS-->>C: null
        end
    end
```

### Save the session

```mermaid
sequenceDiagram
    participant C as Caller (AuthApi or refresh)
    participant TS as RoomTokenStore
    participant CI as TokenCipher
    participant D as TokenStoreDto
    C->>TS: save(AuthTokens)
    TS->>CI: encrypt(access + newline + refresh)
    CI-->>TS: ciphertext
    TS->>D: save(EncryptedAuthTokens(payload, id = 1))
    Note over D: @Upsert, one row only
```

### Accept a pushed customer

`acceptServerCopy` only overwrites the local row if nobody edited it while the push was in flight.

```mermaid
sequenceDiagram
    participant R as CustomerRepositoryImpl
    participant D as CustomerDao
    R->>D: acceptServerCopy(id, expectedUpdatedAt, server)
    D->>D: rowOf(id)
    alt row gone
        Note over D: nothing to do
    else unchanged since push (updatedAt matches, not deleted)
        D->>D: save(server.copy(id, ownerId, syncState = SYNCED))
    else edited or deleted meanwhile
        D->>D: setRemoteId(id, server.remoteId)
        Note over D: stays PENDING, pushed again next sync
    end
```

### Apply a pulled page

```mermaid
sequenceDiagram
    participant R as CustomerRepositoryImpl
    participant D as CustomerDao
    R->>D: applyServerPage(ownerId, page)
    loop each server row
        D->>D: rowWithRemoteId(ownerId, remoteId)
        alt not stored
            D->>D: save(server.copy(id = remoteId))
        else stored with local changes (PENDING, REJECTED or deleted)
            Note over D: keep the local row
        else stored and SYNCED
            D->>D: save(server.copy(id = local.id))
        end
    end
    R->>D: purgeSyncedMissing(ownerId, serverIds)
    D->>D: syncedRemoteIds(ownerId), purge each not in serverIds
```
