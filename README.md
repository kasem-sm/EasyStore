# EasyStore
Auto generate boilerplate code for Androidx Preference Datastore with the help of KSP ([Kotlin Symbol Processor](https://github.com/skydoves/sealedx#:~:text=based%20on%20KSP%20(-,Kotlin%20Symbol%20Processor,-).%0A%0AYou%20can))

# You write

```kotlin
@EasyStore
interface AppPreferences {

    @Store(preferenceKeyName = "app_theme")
    suspend fun updateTheme(theme: Theme)

    // Automatic Enum mapping (Also supports Data Class)
    @Retrieve(preferenceKeyName = "app_theme")
    fun getAppTheme(default: Theme): Flow<Theme>
}
```
