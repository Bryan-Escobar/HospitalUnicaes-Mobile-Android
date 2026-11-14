package sv.edu.unicaes.hospital.core

import androidx.annotation.StringRes
import sv.edu.unicaes.hospital.R

/**
 * Errores que la UI sabe mostrar. Los códigos HTTP de la API se traducen aquí
 * para que ningún ViewModel dependa de Retrofit ni de Supabase.
 */
sealed interface AppError {
    data object SinConexion : AppError

    data object CredencialesInvalidas : AppError

    /** 403 CUENTA_INACTIVA: la administración desactivó la cuenta. */
    data object CuentaInactiva : AppError

    /** 403 PERFIL_INCOMPLETO: la cuenta no tiene registro de personal ni de paciente. */
    data object PerfilIncompleto : AppError

    /** 401: token ausente o vencido. */
    data object NoAutorizado : AppError

    /** 403: el rol o la relación (médico tratante) no permite el acceso. */
    data object SinPermiso : AppError

    data object NoEncontrado : AppError

    /** 409: concurrencia (cama ya asignada, ambulancia ya despachada…). */
    data object Conflicto : AppError

    /** 409 REGISTRO_DUPLICADO: ya existe un registro con ese dato único (p. ej. el documento). */
    data object Duplicado : AppError

    /** 422: regla de negocio o transición de estado inválida. */
    data object Validacion : AppError

    /**
     * Regla de la base de datos con mensaje para el usuario (`CITA_DUPLICADA: el doctor ya tiene…`).
     * [detalle] ya viene sin el código y se muestra tal cual.
     */
    data class Rechazada(
        val detalle: String,
    ) : AppError

    /** 400 VALIDACION: campos rechazados por la API, `campo → mensaje`. */
    data class CamposInvalidos(
        val campos: Map<String, String>,
    ) : AppError

    data class Servidor(
        val codigo: Int,
    ) : AppError

    data class Desconocido(
        val causa: Throwable? = null,
    ) : AppError
}

@StringRes
fun AppError.mensaje(): Int =
    when (this) {
        AppError.SinConexion -> {
            R.string.error_sin_conexion
        }

        AppError.CredencialesInvalidas -> {
            R.string.error_credenciales
        }

        AppError.NoAutorizado -> {
            R.string.error_sesion_expirada
        }

        AppError.CuentaInactiva -> {
            R.string.error_cuenta_inactiva
        }

        AppError.PerfilIncompleto -> {
            R.string.error_perfil_incompleto
        }

        AppError.SinPermiso -> {
            R.string.error_sin_permiso
        }

        AppError.Conflicto -> {
            R.string.error_conflicto
        }

        AppError.Duplicado -> {
            R.string.error_duplicado
        }

        AppError.NoEncontrado -> {
            R.string.error_no_encontrado
        }

        AppError.Validacion, is AppError.CamposInvalidos, is AppError.Rechazada -> {
            R.string.error_validacion
        }

        is AppError.Servidor, is AppError.Desconocido -> {
            R.string.error_desconocido
        }
    }
