package sv.edu.unicaes.hospital.ui.recepcion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import javax.inject.Inject

data class RegistroPacienteUiState(
    val edicion: Boolean = false,
    /** Carga del paciente en modo edición; en registro nuevo arranca en Exito. */
    val carga: UiState<Unit> = UiState.Exito(Unit),
    val form: FormularioPaciente = FormularioPaciente(),
    val errores: Map<CampoPaciente, Int> = emptyMap(),
    val guardando: Boolean = false,
    val errorGeneral: AppError? = null,
    /** 409 REGISTRO_DUPLICADO: el documento que ya existe y, si se encontró, el id de su ficha. */
    val documentoDuplicado: String? = null,
    val duplicadoId: String? = null,
    val guardado: Boolean = false,
) {
    val puedeGuardar: Boolean get() = !guardando && carga is UiState.Exito
}

/** AND-R3 · Registro (sin `pacienteId`) o edición (con `pacienteId`) de un paciente. */
@HiltViewModel
class RegistroPacienteViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val pacientesRepository: PacientesRepository,
    ) : ViewModel() {
        // Mismo nombre que la propiedad de RegistroPacienteRuta.
        private val pacienteId: String? = savedStateHandle["pacienteId"]

        private val _uiState = MutableStateFlow(RegistroPacienteUiState(edicion = pacienteId != null))
        val uiState: StateFlow<RegistroPacienteUiState> = _uiState.asStateFlow()

        /** Datos con los que se abrió la edición, para enviar en el PATCH solo lo que cambió. */
        private var original = FormularioPaciente()

        init {
            if (pacienteId != null) cargar()
        }

        fun cargar() {
            val id = pacienteId ?: return
            _uiState.update { it.copy(carga = UiState.Cargando) }
            viewModelScope.launch {
                when (val resultado = pacientesRepository.obtener(id)) {
                    is Resultado.Exito -> {
                        original = resultado.datos.aFormulario()
                        _uiState.update { it.copy(carga = UiState.Exito(Unit), form = original) }
                    }

                    is Resultado.Fallo -> {
                        _uiState.update { it.copy(carga = UiState.Error(resultado.error)) }
                    }
                }
            }
        }

        /** Cambia el formulario y borra el error de los campos que se tocaron. */
        fun onFormChange(cambio: (FormularioPaciente) -> FormularioPaciente) =
            _uiState.update { estado ->
                val nuevo = cambio(estado.form)
                val tocados = CampoPaciente.entries.filter { nuevo.valor(it) != estado.form.valor(it) }
                estado.copy(
                    form = nuevo,
                    errores = estado.errores - tocados.toSet(),
                    errorGeneral = null,
                    documentoDuplicado = estado.documentoDuplicado.takeUnless { CampoPaciente.DOCUMENTO in tocados },
                    duplicadoId = estado.duplicadoId.takeUnless { CampoPaciente.DOCUMENTO in tocados },
                )
            }

        fun guardar() {
            val estado = _uiState.value
            if (!estado.puedeGuardar) return
            val errores = validarPaciente(estado.form)
            if (errores.isNotEmpty()) {
                _uiState.update { it.copy(errores = errores) }
                return
            }
            val id = pacienteId
            val cambios = if (id == null) estado.form.aDatos() else estado.form.cambiosRespectoA(original)
            if (cambios == null) {
                // Edición sin cambios: no hay nada que enviar.
                _uiState.update { it.copy(guardado = true) }
                return
            }
            _uiState.update { it.copy(guardando = true, errorGeneral = null) }
            viewModelScope.launch {
                val resultado =
                    if (id == null) pacientesRepository.crear(cambios) else pacientesRepository.actualizar(id, cambios)
                when (resultado) {
                    is Resultado.Exito -> _uiState.update { it.copy(guardando = false, guardado = true) }
                    is Resultado.Fallo -> mostrarError(resultado.error, estado.form)
                }
            }
        }

        private suspend fun mostrarError(
            error: AppError,
            form: FormularioPaciente,
        ) {
            when (error) {
                AppError.Duplicado -> {
                    val documento = form.documento.trim()
                    _uiState.update { it.copy(guardando = false, documentoDuplicado = documento) }
                    val existente = buscarPorDocumento(documento)
                    _uiState.update { it.copy(duplicadoId = existente.takeIf { it != pacienteId }) }
                }

                is AppError.CamposInvalidos -> {
                    val porCampo =
                        error.campos.keys
                            .mapNotNull { CampoPaciente.desdeApi(it) }
                            .associateWith { R.string.error_campo_invalido }
                    _uiState.update {
                        it.copy(
                            guardando = false,
                            errores = porCampo,
                            errorGeneral = error.takeIf { porCampo.isEmpty() },
                        )
                    }
                }

                else -> {
                    _uiState.update { it.copy(guardando = false, errorGeneral = error) }
                }
            }
        }

        /** La API no devuelve el id del duplicado: se busca por documento para ofrecer "Abrir ficha". */
        private suspend fun buscarPorDocumento(documento: String): String? =
            (pacientesRepository.listar(documento, 1) as? Resultado.Exito)
                ?.datos
                ?.items
                ?.firstOrNull { it.documento == documento }
                ?.id
    }

private fun FormularioPaciente.valor(campo: CampoPaciente): Any? =
    when (campo) {
        CampoPaciente.NOMBRES -> nombres
        CampoPaciente.APELLIDOS -> apellidos
        CampoPaciente.DOCUMENTO -> documento
        CampoPaciente.NACIMIENTO -> nacimiento
        CampoPaciente.SEXO -> sexo
        CampoPaciente.TELEFONO -> telefono
        CampoPaciente.DIRECCION -> direccion
        CampoPaciente.CONTACTO_NOMBRE -> contactoNombre
        CampoPaciente.CONTACTO_TELEFONO -> contactoTelefono
    }
