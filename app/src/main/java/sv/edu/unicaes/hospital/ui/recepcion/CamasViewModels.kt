package sv.edu.unicaes.hospital.ui.recepcion

import dagger.hilt.android.lifecycle.HiltViewModel
import sv.edu.unicaes.hospital.ui.modelo.AsignacionCamaUi
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.FichaPacienteUi
import sv.edu.unicaes.hospital.ui.modelo.HabitacionUi
import sv.edu.unicaes.hospital.ui.modelo.MuestraViewModel
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import javax.inject.Inject

// Habitaciones (AND-R8 a AND-R11) con datos de ejemplo: 11 habitaciones y 17 camas.

data class IngresoCamaUi(
    val paciente: PacienteUi,
    val habitaciones: List<HabitacionUi>,
)

data class TrasladoUi(
    val paciente: String,
    val asignacion: AsignacionCamaUi,
    val habitaciones: List<HabitacionUi>,
)

private val habitaciones get() = DatosMuestra.planta2 + DatosMuestra.planta3

/** AND-R8 · Mapa de camas. */
@HiltViewModel
class MapaCamasViewModel
    @Inject
    constructor() : MuestraViewModel<List<HabitacionUi>>({ habitaciones })

/** AND-R9 · Ingreso y asignación de cama. */
@HiltViewModel
class IngresoCamaViewModel
    @Inject
    constructor() :
    MuestraViewModel<IngresoCamaUi>({ IngresoCamaUi(DatosMuestra.pacienteIngreso, habitaciones) })

/** AND-R10 · Traslado de cama. */
@HiltViewModel
class TrasladoViewModel
    @Inject
    constructor() :
    MuestraViewModel<TrasladoUi>({
            TrasladoUi(DatosMuestra.fichaLuis.paciente.nombre, DatosMuestra.asignacionLuis, habitaciones)
        })

/** AND-R11 · Alta. */
@HiltViewModel
class AltaViewModel
    @Inject
    constructor() : MuestraViewModel<FichaPacienteUi>({ DatosMuestra.fichaLuis })
