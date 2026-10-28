package sv.edu.unicaes.hospital.ui.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.data.repository.AuthRepository
import sv.edu.unicaes.hospital.data.repository.EstadoSesion
import javax.inject.Inject

@HiltViewModel
class SesionViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        val estadoSesion: StateFlow<EstadoSesion> =
            authRepository.estadoSesion
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoSesion.CARGANDO)

        fun cerrarSesion() {
            viewModelScope.launch { authRepository.cerrarSesion() }
        }
    }
