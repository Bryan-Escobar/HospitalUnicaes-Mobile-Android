package sv.edu.unicaes.hospital.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.BuildConfig
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.data.repository.AuthRepository
import sv.edu.unicaes.hospital.data.repository.EstadoSesion
import sv.edu.unicaes.hospital.domain.model.Rol
import sv.edu.unicaes.hospital.ui.navigation.DoctorRuta
import sv.edu.unicaes.hospital.ui.navigation.LoginRuta
import sv.edu.unicaes.hospital.ui.navigation.RecepcionRuta
import sv.edu.unicaes.hospital.ui.navigation.SinAccesoRuta
import javax.inject.Inject

sealed interface SplashUiState {
    data object Cargando : SplashUiState

    /** Variante dev sin GET /auth/me: el rol se elige a mano para revisar la interfaz. */
    data object ElegirRol : SplashUiState

    data class Error(
        val error: AppError,
    ) : SplashUiState

    data class Listo(
        val destino: Any,
    ) : SplashUiState
}

/** Restaura la sesión guardada y consulta GET /me para decidir el inicio según el rol. */
@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Cargando)
        val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

        init {
            resolverDestino()
        }

        fun resolverDestino() {
            _uiState.value = SplashUiState.Cargando
            viewModelScope.launch {
                val sesion = authRepository.estadoSesion.first { it != EstadoSesion.CARGANDO }
                if (sesion == EstadoSesion.INACTIVA) {
                    _uiState.value = SplashUiState.Listo(LoginRuta)
                    return@launch
                }
                if (!BuildConfig.ROL_DESDE_API) {
                    _uiState.value = SplashUiState.ElegirRol
                    return@launch
                }
                _uiState.value =
                    when (val resultado = authRepository.usuarioActual()) {
                        is Resultado.Exito -> {
                            SplashUiState.Listo(destinoPara(resultado.datos.rol))
                        }

                        is Resultado.Fallo -> {
                            if (resultado.error == AppError.NoAutorizado) {
                                authRepository.cerrarSesion()
                                SplashUiState.Listo(LoginRuta)
                            } else {
                                SplashUiState.Error(resultado.error)
                            }
                        }
                    }
            }
        }

        private fun destinoPara(rol: Rol): Any =
            when {
                rol.usaAppRecepcion -> RecepcionRuta
                rol == Rol.DOCTOR -> DoctorRuta
                else -> SinAccesoRuta
            }
    }
