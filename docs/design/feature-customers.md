# feature:customers

The signed-in user's customers: a searchable list (the Home shell's Customers tab) and an editor,
offline-first. Every write lands in the local `customers` table as `PENDING` and a sync pushes it,
then pulls the server's pages; WorkManager (Android) or `BGTaskScheduler` (iOS) retries when
offline. Depends on `core:domain`, `core:ui`, `core:utils`, `core:network` and `core:database`
(whose `Customer` entity is imported here as `CustomerEntity`).

## Classes

### Presentation

```mermaid
classDiagram
    direction TB
    class CustomersRoute {
        <<composable>>
        +CustomersRoute(ownerId: String, onOpenEditor: (customerId: String?) -> Unit, modifier, viewModel: CustomersViewModel)
    }
    class CustomersScreen {
        <<composable>>
        +CustomersScreen(state: CustomersUiState, search: TextFieldState, onAction, snackbarHostState, modifier)
    }
    class CustomersViewModel {
        -ownerId: String
        +search: FormField
        +state: StateFlow~CustomersUiState~
        +events: SharedFlow~CustomersEvent~
        -syncStatus: MutableStateFlow~SyncStatus~
        -deleteCandidate: MutableStateFlow~Customer?~
        +onAction(action: CustomersAction)
        -sync(showIndicator: Boolean)
        -confirmDelete()
        -bannerFor(pendingCount: Int, outcome: SyncOutcome?) SyncBanner?
    }
    class CustomersUiState {
        <<sealed>>
        Loading
        Success(customers: List~Customer~, query: String, isRefreshing: Boolean, banner: SyncBanner?, deleteCandidate: Customer?)
    }
    class SyncBanner {
        <<data>>
        +pendingCount: Int
        +problem: SyncProblem?
    }
    class SyncProblem {
        <<enumeration>>
        OFFLINE
        FAILED
        SESSION_MISMATCH
    }
    class CustomersAction {
        <<sealed>>
        Refresh
        Add
        Open(customerId: String)
        Delete(customer: Customer)
        ConfirmDelete
        DismissDelete
    }
    class CustomersEvent {
        <<sealed>>
        OpenEditor(customerId: String?)
        Deleted
    }
    class CustomerEditorRoute {
        <<composable>>
        +CustomerEditorRoute(ownerId: String, customerId: String?, onDone: () -> Unit, modifier, viewModel: CustomerEditorViewModel)
    }
    class CustomerEditorScreen {
        <<composable>>
        +CustomerEditorScreen(state: CustomerEditorUiState, fieldState: (CustomerField) -> TextFieldState, onAction, modifier)
    }
    class CustomerEditorViewModel {
        -ownerId: String
        -customerId: String?
        +state: StateFlow~CustomerEditorUiState~
        +events: SharedFlow~CustomerEditorEvent~
        +fields: Map~CustomerField, FormField~
        +field(field: CustomerField) FormField
        +onAction(action: CustomerEditorAction)
        -load(id: String)
        -save()
        -delete()
    }
    class CustomerEditorUiState {
        <<sealed>>
        Loading
        NotFound
        Editing(isNew, status: CustomerStatus?, errors: Map~CustomerField, CustomerFieldError~, isRejected, isSaving, isConfirmingDelete)
    }
    class CustomerEditorAction {
        <<sealed>>
        StatusSelected(status: CustomerStatus)
        Save
        Delete
        ConfirmDelete
        DismissDelete
        Close
    }
    class CustomerEditorEvent {
        <<sealed>>
        Done
    }
    class FormField {
        <<core:ui>>
    }
    class ObserveCustomersUseCase
    class ObservePendingChangesUseCase
    class SyncCustomersUseCase
    class DeleteCustomerUseCase
    class GetCustomerUseCase
    class SaveCustomerUseCase
    class ValidateCustomerUseCase

    CustomersRoute --> CustomersViewModel : koinViewModel(parametersOf(ownerId))
    CustomersRoute --> CustomersScreen : renders
    CustomersScreen ..> CustomersAction : sends
    CustomersViewModel *-- CustomersUiState : combines into
    CustomersUiState *-- SyncBanner
    SyncBanner --> SyncProblem
    CustomersViewModel ..> CustomersEvent : emits
    CustomersViewModel *-- FormField : search
    CustomersViewModel --> ObserveCustomersUseCase : search results
    CustomersViewModel --> ObservePendingChangesUseCase : pending count
    CustomersViewModel --> SyncCustomersUseCase : on start and Refresh
    CustomersViewModel --> DeleteCustomerUseCase : ConfirmDelete
    CustomerEditorRoute --> CustomerEditorViewModel : koinViewModel(parametersOf(ownerId, customerId))
    CustomerEditorRoute --> CustomerEditorScreen : renders
    CustomerEditorScreen ..> CustomerEditorAction : sends
    CustomerEditorViewModel *-- CustomerEditorUiState : owns
    CustomerEditorViewModel ..> CustomerEditorEvent : emits
    CustomerEditorViewModel *-- FormField : one per text CustomerField
    CustomerEditorViewModel --> GetCustomerUseCase : load
    CustomerEditorViewModel --> ValidateCustomerUseCase : save
    CustomerEditorViewModel --> SaveCustomerUseCase : save
    CustomerEditorViewModel --> DeleteCustomerUseCase : ConfirmDelete
```

### Domain

```mermaid
classDiagram
    direction TB
    class CustomerRepository {
        <<interface>>
        +customers(ownerId: String, search: String) Flow~List~Customer~~
        +pendingChanges(ownerId: String) Flow~Int~
        +customer(id: String) Customer?
        +create(ownerId: String, draft: CustomerDraft) String
        +update(id: String, draft: CustomerDraft) Boolean
        +delete(id: String)
        +sync(ownerId: String) SyncOutcome
    }
    class CustomerSyncScheduler {
        <<fun interface>>
        +requestSync(ownerId: String)
    }
    class ObserveCustomersUseCase {
        +invoke(ownerId: String, search: String) Flow~List~Customer~~
    }
    class ObservePendingChangesUseCase {
        +invoke(ownerId: String) Flow~Int~
    }
    class GetCustomerUseCase {
        +invoke(id: String) Customer?
    }
    class SaveCustomerUseCase {
        +invoke(ownerId: String, id: String?, draft: CustomerDraft) Boolean
    }
    class DeleteCustomerUseCase {
        +invoke(ownerId: String, id: String)
    }
    class SyncCustomersUseCase {
        +invoke(ownerId: String) SyncOutcome
    }
    class ValidateCustomerUseCase {
        +invoke(input: CustomerInput) CustomerValidation
        -validateAddress(input, errors) CustomerAddress?
    }
    class Customer {
        <<data>>
        +id: String
        +details: CustomerDraft
        +syncState: SyncState
        +updatedAt: String
    }
    class CustomerDraft {
        <<data>>
        +firstName: String
        +lastName: String?
        +companyName: String?
        +email: String?
        +phone: String?
        +address: CustomerAddress?
        +notes: String?
        +status: CustomerStatus?
    }
    class SyncState {
        <<enumeration>>
        SYNCED
        PENDING
        REJECTED
    }
    class SyncOutcome {
        <<sealed>>
        Synced
        Offline
        SessionMismatch
        Failed
    }
    class CustomerInput {
        <<data>>
        raw text of every field + status: CustomerStatus?
    }
    class CustomerField {
        <<enumeration>>
        FIRST_NAME ... NOTES
        STATUS
    }
    class CustomerFieldError {
        <<sealed>>
        Required
        TooLong(maxLength: Int)
        InvalidEmail
        InvalidPhone
        InvalidCountryCode
    }
    class CustomerValidation {
        <<sealed>>
        Valid(draft: CustomerDraft)
        Invalid(errors: Map~CustomerField, CustomerFieldError~)
    }
    class CustomerAddress {
        <<api-contract>>
    }
    class CustomerStatus {
        <<api-contract>>
    }

    ObserveCustomersUseCase --> CustomerRepository : customers(ownerId, search.trim())
    ObservePendingChangesUseCase --> CustomerRepository
    GetCustomerUseCase --> CustomerRepository
    SyncCustomersUseCase --> CustomerRepository
    SaveCustomerUseCase --> CustomerRepository : create or update
    SaveCustomerUseCase --> CustomerSyncScheduler : requestSync if saved
    DeleteCustomerUseCase --> CustomerRepository : delete
    DeleteCustomerUseCase --> CustomerSyncScheduler : requestSync
    ValidateCustomerUseCase ..> CustomerValidation : returns
    ValidateCustomerUseCase --> CustomerInput : reads
    CustomerValidation --> CustomerField
    CustomerValidation --> CustomerFieldError
    Customer *-- CustomerDraft
    Customer --> SyncState
    CustomerDraft --> CustomerAddress
    CustomerDraft --> CustomerStatus
    CustomerRepository ..> SyncOutcome : sync returns
```

### Data

```mermaid
classDiagram
    direction TB
    class CustomerRepository {
        <<interface>>
    }
    class CustomerSyncScheduler {
        <<fun interface>>
    }
    class CustomerRepositoryImpl {
        -api: CustomerApi
        -dao: CustomerDao
        -dispatcher: CoroutineDispatcher
        -clock: Clock
        -newId: () -> String
        -syncLock: Mutex
        +sync(ownerId: String) SyncOutcome
        -checkSession(ownerId: String) SyncOutcome?
        -push(ownerId: String) SyncOutcome?
        -pushOne(ownerId: String, row: CustomerEntity) SyncOutcome?
        -pushDelete(row: CustomerEntity) SyncOutcome?
        -pushSave(ownerId: String, row: CustomerEntity) SyncOutcome?
        -pull(ownerId: String) SyncOutcome
    }
    class CustomerApi {
        -client: HttpClient
        +customers(cursor: String?, limit: Int) ApiResult~PageResponse~CustomerResponse~~
        +create(request: CustomerRequest) ApiResult~CustomerResponse~
        +replace(remoteId: String, request: CustomerRequest) ApiResult~CustomerResponse~
        +delete(remoteId: String) ApiResult~Unit~
        +currentUser() ApiResult~UserResponse~
    }
    class CustomerMapping {
        <<file>>
        ~CustomerEntity.toDomain() Customer
        ~CustomerDraft.toNewEntity(id: String, ownerId: String, now: String) CustomerEntity
        ~CustomerResponse.toEntity(ownerId: String) CustomerEntity
        ~CustomerEntity.toRequest() CustomerRequest?
        ~ApiError.toPushFailure() PushFailure
        ~ApiError.toSyncOutcome() SyncOutcome
    }
    class PushFailure {
        <<sealed, internal>>
        NotFound
        Rejected
        Stop(outcome: SyncOutcome)
    }
    class WorkManagerCustomerSyncScheduler {
        <<androidMain>>
        -workManager: WorkManager
        +requestSync(ownerId: String)
    }
    class CustomerSyncWorker {
        <<androidMain CoroutineWorker>>
        -syncCustomers: SyncCustomersUseCase
        +doWork() Result
        +KEY_OWNER_ID$
        +UNIQUE_NAME_PREFIX$
        -MAX_ATTEMPTS = 10$
    }
    class BackgroundTaskCustomerSyncScheduler {
        <<iosMain>>
        -syncCustomers: SyncCustomersUseCase
        -scope: CoroutineScope
        +requestSync(ownerId: String)
    }
    class BackgroundSync {
        <<iosMain file>>
        +CUSTOMER_SYNC_TASK_ID$
        +registerCustomerBackgroundSync()
        -runBackgroundSync(task: BGTask)
        -submitBackgroundSync()
    }
    class customerSyncPlatformModule {
        <<expect/actual koin module>>
        Android: WorkManagerCustomerSyncScheduler
        iOS: BackgroundTaskCustomerSyncScheduler
    }
    class customersModule {
        <<koin module>>
        includes customerSyncPlatformModule
        single CustomerApi, CustomerRepository
        factory use cases
        viewModel CustomersViewModel(ownerId), CustomerEditorViewModel(ownerId, customerId)
    }
    class CustomerDao {
        <<core:database>>
    }
    class CustomerEntity {
        <<core:database Customer>>
    }
    class HttpClient {
        <<core:network>>
    }

    CustomerRepositoryImpl ..|> CustomerRepository : implements
    CustomerRepositoryImpl --> CustomerDao : reads/writes
    CustomerRepositoryImpl --> CustomerApi : sync
    CustomerRepositoryImpl --> CustomerMapping : maps
    CustomerMapping ..> PushFailure
    CustomerMapping ..> CustomerEntity
    CustomerApi --> HttpClient : sends
    WorkManagerCustomerSyncScheduler ..|> CustomerSyncScheduler : implements
    WorkManagerCustomerSyncScheduler ..> CustomerSyncWorker : enqueues unique work
    BackgroundTaskCustomerSyncScheduler ..|> CustomerSyncScheduler : implements
    BackgroundTaskCustomerSyncScheduler --> BackgroundSync : submitBackgroundSync on retry
    customersModule --> customerSyncPlatformModule : includes
```

## Sequences

### Open the list

The tab composes, the ViewModel starts a silent sync, and the list renders from Room as soon as
it has rows. Search is debounced 300 ms (0 when cleared).

```mermaid
sequenceDiagram
    participant S as CustomersRoute
    participant VM as CustomersViewModel
    participant OC as ObserveCustomersUseCase
    participant OP as ObservePendingChangesUseCase
    participant R as CustomerRepositoryImpl
    participant D as CustomerDao
    S->>VM: koinViewModel(ownerId), init
    VM->>VM: sync(showIndicator = false), see Sync
    S->>VM: collect state (WhileSubscribed 5 s)
    VM->>OC: invoke(ownerId, query) via flatMapLatest
    OC->>R: customers(ownerId, query)
    R->>D: customers(ownerId, search) Flow
    D-->>R: List(CustomerEntity)
    R-->>VM: List(Customer) via toDomain(), on dispatcher
    VM->>OP: invoke(ownerId)
    OP->>R: pendingChanges(ownerId)
    R->>D: pendingCount(ownerId) Flow
    D-->>VM: Int
    alt empty, no query, first sync running
        VM-->>S: Loading (skeleton)
    else
        VM-->>S: Success(customers, query, isRefreshing, banner, deleteCandidate)
        Note over VM: banner only when pendingCount above 0, problem from the last SyncOutcome
    end
```

### Save a customer

Local first: the row is written `PENDING` and the screen closes. The sync scheduler sends it.

```mermaid
sequenceDiagram
    actor U as User
    participant S as CustomerEditorRoute
    participant VM as CustomerEditorViewModel
    participant V as ValidateCustomerUseCase
    participant UC as SaveCustomerUseCase
    participant R as CustomerRepositoryImpl
    participant D as CustomerDao
    participant SC as CustomerSyncScheduler
    U->>S: tap Save
    S->>VM: onAction(Save)
    VM->>VM: input() from every FormField.submit() and status
    VM->>V: invoke(input)
    alt Invalid
        V-->>VM: Invalid(errors)
        VM-->>S: Editing(errors)
    else Valid
        V-->>VM: Valid(draft)
        VM-->>S: Editing(isSaving = true)
        VM->>UC: invoke(ownerId, customerId, draft)
        alt new (customerId null)
            UC->>R: create(ownerId, draft)
            R->>D: save(draft.toNewEntity(uuid, ownerId, now)) PENDING
            R-->>UC: id
        else existing
            UC->>R: update(id, draft)
            R->>D: updateDetails(..., updatedAt = now) sets PENDING
            D-->>R: rows changed
            R-->>UC: changed above 0
        end
        alt saved
            UC->>SC: requestSync(ownerId)
            UC-->>VM: true
            VM-->>S: event Done
            S->>S: onDone(), AppRoot pops the editor
        else row deleted meanwhile
            UC-->>VM: false
            VM-->>S: NotFound
        end
    end
```

### Delete a customer

From the list (the row's delete button, then confirm) or the editor (Delete, confirm). A soft delete is a tombstone the
next sync pushes, then purges.

```mermaid
sequenceDiagram
    actor U as User
    participant VM as CustomersViewModel or CustomerEditorViewModel
    participant UC as DeleteCustomerUseCase
    participant R as CustomerRepositoryImpl
    participant D as CustomerDao
    participant SC as CustomerSyncScheduler
    U->>VM: Delete, then ConfirmDelete
    VM->>UC: invoke(ownerId, id)
    UC->>R: delete(id)
    R->>D: softDelete(id, now)
    Note over D: the list Flow drops the row at once, pendingCount rises
    UC->>SC: requestSync(ownerId)
    alt list
        VM-->>U: snackbar "deleted" (event Deleted)
    else editor
        VM-->>U: event Done, editor closes
    end
```

### Sync

One run at a time (`syncLock`). Session check, then push every pending row in `updatedAt` order,
then pull every page. Whatever stops it is the `SyncOutcome`.

```mermaid
sequenceDiagram
    participant C as Caller (ViewModel, worker, BGTask)
    participant UC as SyncCustomersUseCase
    participant R as CustomerRepositoryImpl
    participant API as CustomerApi
    participant D as CustomerDao
    participant SV as Server
    C->>UC: invoke(ownerId)
    UC->>R: sync(ownerId)
    Note over R: syncLock.withLock, withContext(dispatcher)
    R->>API: currentUser()
    API->>SV: GET /api/v1/users/me
    alt different user
        R-->>C: SessionMismatch
    else offline, timeout, 401, 5xx
        R-->>C: Offline, SessionMismatch or Failed
    else same user
        R->>D: pending(ownerId)
        loop each pending row
            alt tombstone (deletedAt set)
                opt has remoteId
                    R->>API: delete(remoteId)
                    API->>SV: DELETE /api/v1/customers/remoteId
                end
                Note over R: offline or 5xx stops the run, keeping the tombstone. 404 or refusal does not.
                R->>D: purge(id)
            else no valid request (toRequest null)
                R->>D: markRejected(id, updatedAt)
            else save
                alt no remoteId
                    R->>API: create(request)
                    API->>SV: POST /api/v1/customers
                else
                    R->>API: replace(remoteId, request)
                    API->>SV: PUT /api/v1/customers/remoteId
                end
                alt 2xx
                    R->>D: acceptServerCopy(id, updatedAt, response.toEntity())
                else 404 on replace
                    R->>D: purge(id)
                    Note over R: deleted on another device, the deletion wins
                else 4xx
                    R->>D: markRejected(id, updatedAt)
                else offline, 401, 408, 429, 5xx
                    R-->>C: Offline, SessionMismatch or Failed
                end
            end
        end
        loop pages until nextCursor is null
            R->>API: customers(cursor, MAX_PAGE_SIZE)
            API->>SV: GET /api/v1/customers?limit&cursor
            alt page
                SV-->>API: PageResponse(items, nextCursor)
                R->>D: applyServerPage(ownerId, items.toEntity())
            else failure
                R-->>C: toSyncOutcome()
            end
        end
        R->>D: purgeSyncedMissing(ownerId, serverIds)
        R-->>UC: Synced
        UC-->>C: Synced
    end
```

### Background retry (Android)

```mermaid
sequenceDiagram
    participant UC as Save or Delete use case
    participant SC as WorkManagerCustomerSyncScheduler
    participant WM as WorkManager
    participant W as CustomerSyncWorker
    participant SY as SyncCustomersUseCase
    UC->>SC: requestSync(ownerId)
    SC->>WM: enqueueUniqueWork("customer-sync-ownerId", APPEND_OR_REPLACE, network CONNECTED, exponential 30 s)
    WM->>W: doWork() once connected
    W->>SY: invoke(ownerId) (Koin inject)
    alt Synced or SessionMismatch
        W-->>WM: success
    else Offline or Failed, under 10 attempts
        W-->>WM: retry
    else 10 attempts
        W-->>WM: failure
    end
```

### Background retry (iOS)

```mermaid
sequenceDiagram
    participant APP as iOSApp init (shared setUpApp)
    participant UC as Save or Delete use case
    participant SC as BackgroundTaskCustomerSyncScheduler
    participant SY as SyncCustomersUseCase
    participant BG as BGTaskScheduler
    APP->>BG: registerCustomerBackgroundSync() before launch finishes
    UC->>SC: requestSync(ownerId)
    SC->>SC: NSUserDefaults ownerId saved
    SC->>SY: invoke(ownerId) in its own scope
    alt Offline or Failed
        SC->>BG: submitTaskRequest(BGProcessingTaskRequest, network required)
    end
    BG->>BG: later, runBackgroundSync(task)
    BG->>SY: invoke(stored ownerId)
    alt still Offline or Failed
        BG->>BG: submit again, task completed with failure
    else
        BG->>BG: task completed with success
    end
    Note over BG: on expiration the job is cancelled and resubmitted
```
