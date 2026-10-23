package sv.edu.unicaes.hospital.core

/**
 * Estado base de una pantalla con datos (ver tablero "Android · Estados"):
 * cargando, vacío, error con reintentar, o con datos.
 */
sealed interface UiState<out T> {
    data object Cargando : UiState<Nothing>

    data object Vacio : UiState<Nothing>

    data class Error(
        val error: AppError,
    ) : UiState<Nothing>

    data class Exito<T>(
        val datos: T,
    ) : UiState<T>
}
