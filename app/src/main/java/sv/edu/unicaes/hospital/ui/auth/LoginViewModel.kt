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
import sv.edu.unicaes.hospital.ui.navigation.destinoPara
import javax.inject.Inject

data class LoginUiState(
    val correo: String = "",
    val sugerenciasCorreo: List<String> = emptyList(),
    val contrasena: String = "",
    val cargando: Boolean = false,
    val error: AppError? = null,
    /** Inicio del rol, resuelto con GET /auth/me tras ingresar. */
    val destino: Any? = null,
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

        fun onCorreoChange(valor: String) =
            _uiState.update { it.copy(correo = valor, sugerenciasCorreo = sugerenciasCorreo(valor), error = null) }

        fun onContrasenaChange(valor: String) = _uiState.update { it.copy(contrasena = valor, error = null) }

        fun ingresar() {
            val estado = _uiState.value
            if (!estado.puedeIngresar) return
            _uiState.update { it.copy(cargando = true, error = null) }
            viewModelScope.launch {
                val sesion = authRepository.iniciarSesion(estado.correo.trim(), estado.contrasena)
                if (sesion is Resultado.Fallo) {
                    _uiState.update { it.copy(cargando = false, error = sesion.error) }
                    return@launch
                }
                when (val usuario = authRepository.usuarioActual()) {
                    is Resultado.Exito -> {
                        _uiState.update { it.copy(cargando = false, destino = destinoPara(usuario.datos.rol)) }
                    }

                    is Resultado.Fallo -> {
                        // Sin perfil válido no se deja una sesión a medias (cuenta inactiva, sin red…).
                        authRepository.cerrarSesion()
                        _uiState.update { it.copy(cargando = false, error = usuario.error) }
                    }
                }
            }
        }
    }
