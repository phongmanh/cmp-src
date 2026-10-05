# Naming

- Classes, objects and interfaces `PascalCase`; functions and variables `camelCase`; `const val` and top-level constants `SCREAMING_SNAKE_CASE`.
- Composables are noun phrases: `ProfileScreen`, not `ShowProfile`.
- Use cases are a verb phrase plus `UseCase`: `SignInWithEmailUseCase`.
- Repositories are a noun plus `Repository`; the interface has no prefix and the implementation adds `Impl`: `CustomerRepository` / `CustomerRepositoryImpl`.
- Screen files group as `<Name>Screen`, `<Name>ViewModel`, `<Name>UiState`, `<Name>Action`, `<Name>Event`.
- Test files: `<ClassUnderTest>Test.kt`.
- Resource keys: `<screen>_<element>[_<kind>]` — `login_password_label`, `customers_search_placeholder`. Content descriptions start with `cd_`; messages several screens share start with `validation_` or `error_`.
