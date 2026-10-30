# NeoStore In-App Review SDK

SDK позволяет приложению запросить отзыв в NeoStore. `appId` задаётся один раз при инициализации; передавать его при каждом вызове больше не нужно.

## Как это работает

SDK открывает публичный экран отзыва NeoStore через Android `Intent`. Сам NeoStore проверяет авторизацию и наличие существующего отзыва, показывает экран и отправляет отзыв. SDK не получает и не хранит токен пользователя.

## Требования

- Android 7.0 (API 24) или новее;
- вызывающая Activity должна наследоваться от `ComponentActivity`, например быть `AppCompatActivity`;
- NeoStore должен быть установлен на устройстве;
- `appId` — положительный числовой ID приложения в каталоге NeoStore, а не Android package name.

## 1. Подключение Gradle-зависимости

В `settings.gradle.kts` проекта добавьте JitPack в репозитории зависимостей, если его там ещё нет:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Для Groovy DSL (`settings.gradle`) синтаксис репозитория:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

В `build.gradle.kts` модуля приложения:

```kotlin
dependencies {
    implementation("test.test.test:neostore-iar:1.0.0")
}
```

Указанные координаты — временная заглушка. Когда будет опубликован SDK, замените их на предоставленные Maven-координаты JitPack. Если проект использует Groovy DSL:

```groovy
dependencies {
    implementation 'test.test.test:neostore-iar:1.0.0'
}
```

Затем синхронизируйте проект с Gradle.

## 2. Инициализация SDK

Инициализируйте SDK один раз при запуске процесса приложения — предпочтительно в классе `Application`. Передавайте именно application context, а `appId` — строкой:

```kotlin
class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        NeoStoreReview.initialize(
            context = applicationContext,
            appId = "123456"
        )
    }
}
```

Зарегистрируйте класс приложения в `AndroidManifest.xml`:

```xml
<application
    android:name=".ExampleApplication"
    ... >
</application>
```

Для Java:

```java
public final class ExampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NeoStoreReview.initialize(getApplicationContext(), "123456");
    }
}
```

`initialize` проверяет формат и положительность ID. При некорректном значении метод выбрасывает `IllegalArgumentException`. Значение действует в процессе приложения; вызовите `initialize` при каждом запуске процесса.

## 3. Запуск экрана отзыва

После инициализации вызывайте метод без `appId`:

### Kotlin

```kotlin
NeoStoreReview.requestReview(this) { result ->
    when (result) {
        is ReviewResult.Submitted -> {
            val rating = result.rating
            val comment = result.comment
            // Отзыв принят NeoStore.
        }
        is ReviewResult.Cancelled -> {
            // Пользователь закрыл экран.
        }
        is ReviewResult.AlreadyReviewed -> {
            // Отзыв уже существует; обычно ничего показывать не нужно.
        }
        is ReviewResult.NotAuthenticated -> {
            // Пользователь не вошёл в NeoStore.
        }
        is ReviewResult.Error -> {
            val message = result.message
            // Обработайте ошибку.
        }
    }
}
```

### Java

```java
NeoStoreReview.requestReview(this, result -> {
    if (result instanceof ReviewResult.Submitted) {
        ReviewResult.Submitted submitted = (ReviewResult.Submitted) result;
        int rating = submitted.getRating();
        String comment = submitted.getComment();
    } else if (result instanceof ReviewResult.Cancelled) {
        // Пользователь закрыл экран.
    } else if (result instanceof ReviewResult.AlreadyReviewed) {
        // Отзыв уже существует.
    } else if (result instanceof ReviewResult.NotAuthenticated) {
        // Пользователь не вошёл в NeoStore.
    } else if (result instanceof ReviewResult.Error) {
        String message = ((ReviewResult.Error) result).getMessage();
        // Обработайте ошибку.
    }
});
```

`showReview(activity, callback)` — синоним `requestReview(activity, callback)`.
Вызывайте метод из UI-потока. Callback результата также приходит в UI-потоке.

## Результаты

| Тип | Значение |
|---|---|
| `ReviewResult.Submitted` | NeoStore успешно принял отзыв. Содержит `rating` и `comment`. |
| `ReviewResult.Cancelled` | Пользователь закрыл экран, не отправив отзыв. |
| `ReviewResult.AlreadyReviewed` | У пользователя уже есть отзыв для указанного `appId`. |
| `ReviewResult.NotAuthenticated` | Пользователь не авторизован в NeoStore. |
| `ReviewResult.Error` | Не удалось запустить NeoStore или произошла ошибка. Содержит `message`. |

Успех сообщается только после того, как NeoStore подтвердил отправку отзыва.

## Проверка установки NeoStore

Перед запуском можно проверить, установлено ли приложение:

```kotlin
if (NeoStoreReview.isNeoStoreInstalled(applicationContext)) {
    NeoStoreReview.requestReview(this) { result ->
        // Обработка результата.
    }
} else {
    // Предложите установить NeoStore или пропустите запрос отзыва.
}
```

## Важные замечания

- Инициализируйте SDK в `Application.onCreate()`, до создания Activity. SDK заранее регистрирует обработчик результата для Activity, чтобы его можно было безопасно вызвать, например, после нажатия кнопки.
- Передавайте в `initialize` корректный ID приложения из NeoStore. В тестовом профиле из проекта используется `2`.
- Не передавайте NeoStore токены или данные аккаунта: SDK их не использует.
- Не запускайте несколько запросов отзыва одновременно из одной Activity.
- Проверка наличия NeoStore — вспомогательная; между проверкой и запуском приложение теоретически может быть удалено. Ошибка запуска в этом случае придёт как `ReviewResult.Error`.

## Контракт с NeoStore

SDK запускает Activity NeoStore с action `com.neostudio.neostore.action.REQUEST_REVIEW`, package `com.neostudio.neostore` и extra `in_app_review_app_id`. Это публичный контракт NeoStore. Для интеграции достаточно публичного API SDK — вручную создавать Intent не требуется.
