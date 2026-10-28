package sv.edu.unicaes.hospital.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.AuthRepository
import sv.edu.unicaes.hospital.domain.model.Usuario
import javax.inject.Inject

/** Datos de la cuenta de la sesión, desde GET /auth/me. */
@HiltViewModel
class PerfilViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<UiState<Usuario>>(UiState.Cargando)
        val uiState: StateFlow<UiState<Usuario>> = _uiState.asStateFlow()

        init {
            cargar()
        }

        fun cargar() {
            _uiState.value = UiState.Cargando
            viewModelScope.launch {
                _uiState.value =
                    when (val resultado = authRepository.usuarioActual()) {
                        is Resultado.Exito -> UiState.Exito(resultado.datos)
                        is Resultado.Fallo -> UiState.Error(resultado.error)
                    }
            }
        }
    }
