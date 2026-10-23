package sv.edu.unicaes.hospital.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.data.repository.AuthRepository
import javax.inject.Inject

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val error: AppError? = null,
    val ingresado: Boolean = false,
) {
    val puedeIngresar: Boolean get() = correo.isNotBlank() && contrasena.isNotBlank() && !cargando
}

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        fun onCorreoChange(valor: String) = _uiState.update { it.copy(correo = valor, error = null) }

        fun onContrasenaChange(valor: String) = _uiState.update { it.copy(contrasena = valor, error = null) }

        fun ingresar() {
            val estado = _uiState.value
            if (!estado.puedeIngresar) return
            _uiState.update { it.copy(cargando = true, error = null) }
            viewModelScope.launch {
                when (val resultado = authRepository.iniciarSesion(estado.correo.trim(), estado.contrasena)) {
                    is Resultado.Exito -> _uiState.update { it.copy(cargando = false, ingresado = true) }
                    is Resultado.Fallo -> _uiState.update { it.copy(cargando = false, error = resultado.error) }
                }
            }
        }
    }
