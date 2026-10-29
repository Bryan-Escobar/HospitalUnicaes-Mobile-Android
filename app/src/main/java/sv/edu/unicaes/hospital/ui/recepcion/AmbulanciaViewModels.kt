package sv.edu.unicaes.hospital.ui.recepcion

import dagger.hilt.android.lifecycle.HiltViewModel
import sv.edu.unicaes.hospital.ui.modelo.AmbulanciaUi
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.MuestraViewModel
import sv.edu.unicaes.hospital.ui.modelo.ServicioBitacoraUi
import javax.inject.Inject

/** AND-R13 · Ambulancia. Datos de ejemplo hasta el canal en tiempo real (`context.md` §3.5). */
@HiltViewModel
class AmbulanciaViewModel
    @Inject
    constructor() : MuestraViewModel<AmbulanciaUi>({ DatosMuestra.ambulancia })

/** AND-R14 · Bitácora de ambulancia (reporte de servicios atendidos). */
@HiltViewModel
class BitacoraViewModel
    @Inject
    constructor() :
    MuestraViewModel<List<ServicioBitacoraUi>>({ DatosMuestra.bitacora }) {
        val detalle = DatosMuestra.detalleServicio
    }
