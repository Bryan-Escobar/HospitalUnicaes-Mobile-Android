package sv.edu.unicaes.hospital.ui.recepcion

import kotlinx.serialization.Serializable

// Rutas internas de la app de Recepción (handoff §11.2 · Android).

@Serializable
data object InicioRuta

@Serializable
data object PacientesRuta

@Serializable
data object CitasRuta

@Serializable
data object CamasRuta

@Serializable
data object MasRuta

@Serializable
data class RegistroPacienteRuta(
    /** Null para registrar uno nuevo; con id, edita ese paciente. */
    val pacienteId: String? = null,
)

@Serializable
data class FichaPacienteRuta(
    val pacienteId: String,
)

/**
 * Nueva cita, o reprogramar la cita [citaId]. Los demás datos llegan precargados desde la ficha
 * ([pacienteId]) o desde un hueco libre de la agenda ([fecha] `2026-10-29`, [hora] `10:30`, [doctorId]).
 */
@Serializable
data class NuevaCitaRuta(
    val citaId: String? = null,
    val pacienteId: String? = null,
    val fecha: String? = null,
    val hora: String? = null,
    val doctorId: String? = null,
)

@Serializable
data object IngresoCamaRuta

@Serializable
data object TrasladoRuta

@Serializable
data object AltaRuta

@Serializable
data object TurnosPersonalRuta

@Serializable
data object AmbulanciaRuta

@Serializable
data object BitacoraRuta

@Serializable
data object LaboratorioRuta

@Serializable
data class DetalleOrdenRuta(
    val ordenId: String,
)

@Serializable
data object CargarResultadoRuta

@Serializable
data object PerfilRecepcionRuta
