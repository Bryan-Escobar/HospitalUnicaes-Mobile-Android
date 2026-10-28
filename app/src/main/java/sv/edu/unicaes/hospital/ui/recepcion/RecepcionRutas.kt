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
data object RegistroPacienteRuta

@Serializable
data class FichaPacienteRuta(
    val pacienteId: String,
)

@Serializable
data class NuevaCitaRuta(
    val solicitudId: String? = null,
)

@Serializable
data object SolicitudesCitasRuta

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
