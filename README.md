# HospitalUnicaes-Mobile-Android

App Android del sistema de gestión del Hospital Universitario Central (HospitalUnicaes), para Recepcionistas, Asistentes/Enfermeros y Doctores: registro e ingreso de pacientes, citas, turnos, asignación de camas y despacho de ambulancia.

Stack: Kotlin + Jetpack Compose (Material 3), Navigation Compose, Hilt, Retrofit + OkHttp, supabase-kt (Auth y Realtime), Gradle con version catalog (`gradle/libs.versions.toml`) y ktlint. Arquitectura MVVM con Coroutines/Flow; los datos se consumen del backend unificado ([HospitalUnicaes-Api](https://github.com/Bryan-Escobar/HospitalUnicaes-Api)) y la autenticación usa Supabase Auth.

- Reglas de negocio y privacidad: [`context.md`](./context.md)
- Ramas, commits y Pull Requests: [`CONTRIBUTING.md`](./CONTRIBUTING.md)

## Requisitos

- Android Studio (versión reciente, con soporte para AGP 9)
- JDK 17+
- SDK de Android 36

## Configuración

Las URLs y llaves no se suben al repo. Copia `local.properties.example` como `local.properties` (Android Studio ya pone ahí `sdk.dir`) y llena los valores de cada ambiente:

| Clave | Uso |
|---|---|
| `dev.apiBaseUrl` / `prod.apiBaseUrl` | URL base de la API Express, terminada en `/` (en el emulador, `http://10.0.2.2:3000/api/` es el localhost de tu PC) |
| `dev.supabaseUrl` / `prod.supabaseUrl` | URL del proyecto de Supabase |
| `dev.supabaseKey` / `prod.supabaseKey` | Llave pública (publishable / anon). **Nunca** la `service_role` |

Si falta alguna, la app se detiene al abrir con un mensaje que dice cuál.

## Ejecutar

1. `git clone https://github.com/Bryan-Escobar/HospitalUnicaes-Mobile-Android.git && git checkout development`
2. Crear `local.properties` (ver arriba), abrir la carpeta en Android Studio y esperar el *Gradle sync*.
3. Elegir la variante `devDebug` en *Build Variants* y ejecutar `app` en un emulador o dispositivo.

Desde terminal:

```
./gradlew assembleDevDebug     # APK de desarrollo
./gradlew testDevDebugUnitTest # pruebas unitarias
./gradlew ktlintCheck          # estilo (ktlintFormat lo corrige)
```

## Arquitectura

MVVM: `Screen (Compose) → ViewModel (StateFlow) → Repository → HospitalApi (Retrofit) / Supabase`.

| Paquete | Contenido |
|---|---|
| `ui/` | Pantallas Compose, ViewModels, navegación (`ui/navigation`) y tema (`ui/theme`, tokens del sistema de diseño) |
| `data/remote` | `HospitalApi` (Retrofit), DTOs y `AuthInterceptor` (agrega el JWT de Supabase a cada llamada) |
| `data/local` | `DataStoreSessionManager`: la sesión de Supabase se guarda en DataStore y se restaura al abrir la app |
| `data/repository` | Repositorios: única puerta de los ViewModels a los datos |
| `domain/model` | Modelos de la app (`Usuario`, `Rol`) |
| `core/` | `Resultado`, `AppError` (traducción de 401/403/409/422) y `UiState` |
| `di/` | Módulos de Hilt (red, Supabase, repositorios) |

Variantes: `dev` (applicationId `sv.edu.unicaes.hospital.dev`, permite HTTP solo hacia `10.0.2.2`) y `prod` (solo HTTPS).

## Ramas

| Rama | Uso |
|---|---|
| `prod` | Producción |
| `staging` | Pruebas e integración |
| `development` | Integración diaria; todo PR de tarea apunta aquí |

Ramas de tarea: `HUNI-<n>-<titulo-en-kebab>` (ej. `HUNI-15-repositorios-github`). Detalle en `CONTRIBUTING.md`.
