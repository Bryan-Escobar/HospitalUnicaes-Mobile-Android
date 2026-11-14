package sv.edu.unicaes.hospital.ui.navigation

import sv.edu.unicaes.hospital.domain.model.Rol

/** Inicio de cada rol. Paciente y Administrador no usan la app Android (AND-03). */
fun destinoPara(rol: Rol): Any =
    when (rol) {
        Rol.RECEPCIONISTA -> RecepcionRuta
        Rol.DOCTOR -> DoctorRuta
        else -> SinAccesoRuta
    }
