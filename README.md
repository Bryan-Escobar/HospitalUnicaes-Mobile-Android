# HospitalUnicaes-Mobile-Android

App Android del sistema de gestión del Hospital Universitario Central (HospitalUnicaes), para Recepcionistas, Asistentes/Enfermeros y Doctores: registro e ingreso de pacientes, citas, turnos, asignación de camas y despacho de ambulancia.

Stack: Kotlin + Jetpack Compose (Material 3), Gradle con version catalog (`gradle/libs.versions.toml`). Arquitectura MVVM con Coroutines/Flow; los datos se consumen del backend unificado ([HospitalUnicaes-Api](https://github.com/Bryan-Escobar/HospitalUnicaes-Api)) y la autenticación usa Supabase Auth.

- Reglas de negocio y privacidad: [`context.md`](./context.md)
- Ramas, commits y Pull Requests: [`CONTRIBUTING.md`](./CONTRIBUTING.md)

## Requisitos

- Android Studio (versión reciente, con soporte para AGP 9)
- JDK 17+
- SDK de Android 36

## Ejecutar

1. `git clone https://github.com/Bryan-Escobar/HospitalUnicaes-Mobile-Android.git && git checkout development`
2. Abrir la carpeta en Android Studio y esperar el *Gradle sync*.
3. Ejecutar la configuración `app` en un emulador o dispositivo.

Desde terminal: `./gradlew assembleDebug`

## Ramas

| Rama | Uso |
|---|---|
| `prod` | Producción |
| `staging` | Pruebas e integración |
| `development` | Integración diaria; todo PR de tarea apunta aquí |

Ramas de tarea: `HUNI-<n>-<titulo-en-kebab>` (ej. `HUNI-15-repositorios-github`). Detalle en `CONTRIBUTING.md`.
