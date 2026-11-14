package sv.edu.unicaes.hospital.ui.doctor

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import sv.edu.unicaes.hospital.ui.modelo.CitaUi
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.ExpedienteUi
import sv.edu.unicaes.hospital.ui.modelo.MuestraViewModel
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import javax.inject.Inject

/** AND-D3 · Mis pacientes. */
@HiltViewModel
class MisPacientesViewModel
    @Inject
    constructor() :
    MuestraViewModel<List<PacienteUi>>({ DatosMuestra.misPacientes })

/** AND-D4 · Expediente: el de Tomás con los datos del paciente elegido. */
@HiltViewModel
class ExpedienteViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
    ) : MuestraViewModel<ExpedienteUi>({
            // Mismo nombre que la propiedad de ExpedienteRuta.
            val id: String = checkNotNull(savedStateHandle["pacienteId"])
            val paciente =
                DatosMuestra.misPacientes.firstOrNull { it.id == id } ?: DatosMuestra.expedienteTomas.paciente
            DatosMuestra.expedienteTomas.copy(paciente = paciente)
        })

/** AND-D5 · Nueva consulta de la cita elegida. */
@HiltViewModel
class NuevaConsultaViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
    ) : MuestraViewModel<CitaUi>({
            // Mismo nombre que la propiedad de NuevaConsultaRuta.
            val id: String = checkNotNull(savedStateHandle["citaId"])
            citaDeMuestra(id) ?: DatosMuestra.detalleCita.cita
        })
