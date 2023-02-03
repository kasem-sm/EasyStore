# EasyStore
Auto generate boilerplate code for Androidx Preference Datastore with the help of KSP ([Kotlin Symbol Processor](https://kotlinlang.org/docs/ksp-overview.html)

## Features
✅ Supports `Int`, `String` `Double`, `Boolean`, `Float`, `Long`, `Set<String>`.

✅ Automatic mapping for Kotlin enum and Data class.


###  ✍️ You write
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

### 🧑‍💻 EasyStore generates
```kotlin
public class AppPreferencesImpl(
  private val dataStore: DataStore<Preferences>,
) : AppPreferences {
  public override suspend fun updateTheme(theme: Theme): Unit {
    dataStore.edit { preferences ->
        preferences[APP_THEME_KEY] = theme.name
    }
  }
    
  public override fun getAppTheme(default: Theme): Flow<Theme> = dataStore.data
  .catch { exception ->
      if (exception is IOException) {
          emit(emptyPreferences())
      } else {
          throw exception
      }
  }.map { preference ->
      Theme.valueOf(preference[APP_THEME_KEY] ?: default.name)
  }
    
  private companion object {
    private val APP_THEME_KEY: Preferences.Key<String> = stringPreferencesKey("app_theme")
  }
}
```

### 🔥 Usage
```kotlin
// Single Instance of DataStore
private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "my_prefs")

class MainActivity : AppCompatActivity() {

    private val appPreferences: AppPreferences by lazy {
        // Impl generated by EasyStore
        AppPreferencesImpl(application.dataStore)
    }

    private lateinit var currentTheme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        withRepeatOnLifecycle {
            appPreferences.getAppTheme(default = Theme.SYSTEM).collectLatest { theme ->
                currentTheme = theme
            }
        }
        
        private fun applyTheme(theme: Theme) {
            lifecycleScope.launch {
                appPreferences.updateTheme(theme)
            }
        }
    }
}
```

## Guidelines
1. Create an interface and annotate it with `@EasyStore`.
2. Create two functions, one to store and other to retrieve, annotate it with `@Store` and `@Retrieve` respectively.
3. Make sure to have a return type of `Flow<T>` to the retrieve function.
4. Add a parameter/argument to the retrieve function with the same type you want to return (for default).
```kotlin
    @Retrieve(preferenceKeyName = "app_theme")
    fun getAppTheme(default: Theme): Flow<Theme>
```
5. Last but not the least, make sure the `preferenceKeyName` is same for both the retrieve and store function.
 
Checkout the official [sample](https://github.com/kasem-sm/EasyStore/blob/master/sample/src/main/java/kasem/sm/easystore/sample/MainActivity.kt).

## Gradle Setup

To use [KSP (Kotlin Symbol Processing)](https://kotlinlang.org/docs/ksp-quickstart.html) and EasyStore library in your project, you need to follow steps below.

### 1. Enable KSP in your module

Add the KSP plugin below into your **module**'s `build.gradle` file:

<details open>
  <summary>Kotlin (KTS)</summary>

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}
```
</details>

<details>
  <summary>Groovy</summary>

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}
```
</details>

> **Note**: Make sure your current Kotlin version and [KSP version](https://github.com/google/ksp/releases) is the same.
### 2. Add EasyStore dependencies

[![Maven Central](https://img.shields.io/badge/Maven%20Central-v0.0.4-green?style=for-the-badge)](https://central.sonatype.com/namespace/com.kasem-sm.easystore)

Add the dependency below into your **module**'s `build.gradle` file:

```gradle
dependencies {
    // DataStore (Use the same version to avoid any conflicts)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    implementation("com.kasem-sm.easystore:core:0.0.4-alpha")
    ksp("com.kasem-sm.easystore:processor:0.0.4-alpha")
}
```

### 3. Add KSP source path

To access generated codes from KSP, you need to set up the source path like the below into your **module**'s `build.gradle` file:

<details open>
  <summary>Android Kotlin (KTS)</summary>

```kotlin
kotlin {
  sourceSets.configureEach {
    kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
  }
}
```
</details>

<details>
  <summary>Android Groovy</summary>

```gradle
android {
    applicationVariants.all { variant ->
        kotlin.sourceSets {
            def name = variant.name
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
}
```
</details>

## Find this project useful? 💖

Support it by starring this repository. Join our [Stargazers](https://github.com/kasem-sm/SlimeKT/stargazers) team!

## Contact 🤙

Direct Messages on [My Twitter](https://twitter.com/KasemSM_) are always open. If you have any
questions related to EasyStore or Android development, ping me anytime!

## 💎 Credits 
* Gradle setup from [skydoves](https://github.com/skydoves)'s repo.
* Márton B's super-helpful article, [Publishing Android libraries to MavenCentral in 2021](https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/).
