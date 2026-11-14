package sv.edu.unicaes.hospital.ui.modelo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sv.edu.unicaes.hospital.core.UiState

/**
 * ViewModel de una pantalla que todavía muestra [DatosMuestra]. Expone el mismo
 * `StateFlow<UiState<T>>` que tendrá al conectarse a su repositorio: al reemplazar [datos] por
 * la llamada a la API, la pantalla y el grafo de navegación no cambian.
 */
abstract class MuestraViewModel<T>(
    private val datos: () -> T,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<T>>(UiState.Exito(datos()))
    val uiState: StateFlow<UiState<T>> = _uiState.asStateFlow()

    fun cargar() {
        _uiState.value = UiState.Exito(datos())
    }
}

/** Para pantallas que reciben los datos ya listos (formularios): las dibuja cuando hay datos. */
@Composable
fun <T> ConDatos(
    viewModel: MuestraViewModel<T>,
    contenido: @Composable (T) -> Unit,
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    (estado as? UiState.Exito)?.let { contenido(it.datos) }
}
