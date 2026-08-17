# Naming Conventions

- Classes/Objects/Interfaces: `PascalCase`
- Functions/variables: `camelCase`
- Constants (`const val`, top-level): `SCREAMING_SNAKE_CASE`
- Composables: `PascalCase`, noun phrases (`ProfileScreen`, not `ShowProfile`)
- UseCases: verb phrase + `UseCase` suffix (`GetUserProfileUseCase`)
- Repositories: noun + `Repository` suffix, interface has no prefix, impl has `Impl` suffix
  (`UserRepository` / `UserRepositoryImpl`)
- Test files: `<ClassUnderTest>Test.kt`
- Resource IDs: `<screen>_<component>_<type>` (e.g. `login_email_input`)
