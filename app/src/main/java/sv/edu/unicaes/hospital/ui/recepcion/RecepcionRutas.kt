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

/**
 * Ingreso y asignación de cama del paciente [pacienteId] (HUNI-67). [camaId] llega preelegida
 * cuando el ingreso empezó tocando una cama libre del mapa.
 */
@Serializable
data class IngresoCamaRuta(
    val pacienteId: String,
    val camaId: String? = null,
)

/** Elegir al paciente que se va a ingresar, desde el mapa de camas, conservando la cama tocada. */
@Serializable
data class ElegirPacienteCamaRuta(
    val camaId: String? = null,
)

/** Traslado de la hospitalización activa del paciente [pacienteId]. */
@Serializable
data class TrasladoRuta(
    val pacienteId: String,
)

/** Alta de la hospitalización activa del paciente [pacienteId]. */
@Serializable
data class AltaRuta(
    val pacienteId: String,
)

@Serializable
data object TurnosPersonalRuta

@Serializable
data object MisTurnosRecepcionRuta

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

/** Formulario de resultado de un examen En Proceso de la orden [ordenId]. */
@Serializable
data class CargarResultadoRuta(
    val ordenId: String,
    val examenId: String,
)

@Serializable
data object PerfilRecepcionRuta
