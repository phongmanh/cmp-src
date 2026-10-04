---
name: move-resource
description: Moves a Compose string or drawable into core:ui when a second module needs it, fixing imports and clearing the stale Android asset copy. Use when a module needs a resource that lives in another feature module.
argument-hint: <resource key> from <module>
---

# Move a resource into core:ui

A resource lives in the module whose code uses it. When a second module needs it, it moves to `core:ui` — never imported sideways from another feature. It keeps its key.

## Steps

1. **Move it.**
   - Drawable: `git mv <module>/src/commonMain/composeResources/drawable/<key>.xml core/ui/src/commonMain/composeResources/drawable/`
   - String or plural: cut the entry from `<module>/.../values/strings.xml` into `core/ui/src/commonMain/composeResources/values/strings.xml` under a matching comment group. Do the same in any `values-<locale>/strings.xml`, once there are translations.

2. **Fix the imports** in every file that uses it (`grep -rnw "<key>" feature core shared --include=*.kt`):
   - The import changes from `cmpsrc.<module path>.generated.resources.<key>` to `cmpsrc.core.ui.generated.resources.<key>`.
   - A file that also uses its own module's resources imports `core:ui`'s under an alias:
     ```kotlin
     import cmpsrc.core.ui.generated.resources.Res as UiRes
     import cmpsrc.core.ui.generated.resources.<key>
     // UiRes.drawable.<key> / UiRes.string.<key>
     ```

3. **Clear the stale copy** — `./gradlew clean`. The Android asset copy task never deletes, so without this the old module's copy stays in the APK.

## Verify

```
grep -rn "<key>" <old module>/src/commonMain/composeResources   # no match
./gradlew :core:ui:compileKotlinIosSimulatorArm64 :<old module>:compileKotlinIosSimulatorArm64 :<new module>:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug
```
