package sv.edu.unicaes.hospital.ui.recepcion

import dagger.hilt.android.lifecycle.HiltViewModel
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.InicioRecepcionUi
import sv.edu.unicaes.hospital.ui.modelo.MuestraViewModel
import javax.inject.Inject

/** AND-R1 · Inicio (bandeja). Datos de ejemplo hasta tener el resumen del turno en la API. */
@HiltViewModel
class InicioRecepcionViewModel
    @Inject
    constructor() :
    MuestraViewModel<InicioRecepcionUi>({ DatosMuestra.inicioRecepcion })
