package sv.edu.unicaes.hospital.ui.doctor

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import sv.edu.unicaes.hospital.ui.modelo.AgendaUi
import sv.edu.unicaes.hospital.ui.modelo.CitaUi
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.DetalleCitaUi
import sv.edu.unicaes.hospital.ui.modelo.MuestraViewModel
import javax.inject.Inject

/** AND-D1 · Agenda del doctor. */
@HiltViewModel
class AgendaViewModel
    @Inject
    constructor() : MuestraViewModel<AgendaUi>({ DatosMuestra.agendaDoctor })

/** AND-D2 · Detalle de cita. */
@HiltViewModel
class DetalleCitaViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
    ) : MuestraViewModel<DetalleCitaUi>({
            // Mismo nombre que la propiedad de DetalleCitaRuta.
            val id: String = checkNotNull(savedStateHandle["citaId"])
            citaDeMuestra(id)
                ?.let { DatosMuestra.detalleCita.copy(cita = it.copy(fecha = "Mié 21 oct · ${it.hora}")) }
                ?: DatosMuestra.detalleCita
        })

internal fun citaDeMuestra(id: String): CitaUi? = DatosMuestra.agendaDoctor.citas.firstOrNull { it.id == id }
