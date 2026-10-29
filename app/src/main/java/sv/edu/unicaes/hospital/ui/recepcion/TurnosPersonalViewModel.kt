package sv.edu.unicaes.hospital.ui.recepcion

import dagger.hilt.android.lifecycle.HiltViewModel
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.DiaTurnosUi
import sv.edu.unicaes.hospital.ui.modelo.MuestraViewModel
import javax.inject.Inject

/** AND-R12 · Turnos del personal. */
@HiltViewModel
class TurnosPersonalViewModel
    @Inject
    constructor() :
    MuestraViewModel<List<DiaTurnosUi>>({ DatosMuestra.turnosPersonal })
