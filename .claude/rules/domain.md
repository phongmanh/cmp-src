---
paths:
  - "core/domain/**"
  - "feature/**/domain/**"
  - "feature/**/*UseCase*Test.kt"
---

# Domain layer

`core:domain` and the `domain` package of each `feature:*` module.

- Pure Kotlin: the standard library, coroutines and the `api-contract` types. No Compose, Ktor or Room — those belong to the layers on either side.
- One use case per business action, named as a verb phrase (`SignInWithEmailUseCase`).
- Use cases depend on repository interfaces declared in the feature's `domain` package, or in `core:domain` once a second feature needs them — never on an implementation.
- Domain models are separate from DTOs and mapped at the data boundary. The one exception is the contract's `UserResponse`, which is the signed-in user end to end (`CLAUDE.md` → Networking).
- Take a `CoroutineDispatcher` as a parameter rather than naming `Dispatchers.*`, so a test controls it.
- Every public use case has a unit test in `commonTest`, built on fakes. A domain test never needs an Android runtime; one that does is testing the wrong layer.
