package sv.edu.unicaes.hospital.ui.modelo

import sv.edu.unicaes.hospital.domain.model.EstadoAmbulancia
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.Sexo
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion

// Modelos de presentación: lo que cada pantalla necesita ya formateado para mostrar.
// Los ViewModels los construirán a partir de los DTO de la API; por ahora salen de DatosMuestra.

data class TurnoUi(
    val dia: String,
    val horario: String,
    val area: String,
    val tipo: String? = null,
    val enCurso: Boolean = false,
    val terminado: Boolean = false,
    val sinTurno: Boolean = false,
)

data class TurnoPersonalUi(
    val persona: String,
    val area: String,
    val horario: String,
    val tipo: String,
)

data class DiaTurnosUi(
    val dia: String,
    val turnos: List<TurnoPersonalUi>,
)

data class OcupacionUi(
    val tipo: TipoHabitacion,
    val ocupadas: Int,
    val total: Int,
) {
    val libres: Int get() = total - ocupadas
}

data class CitaUi(
    val id: String,
    val hora: String,
    val paciente: String,
    val doctor: String,
    val motivo: String,
    val estado: EstadoCita,
    val fecha: String = "",
)

data class AgendaDoctorUi(
    val doctor: String,
    val especialidad: String,
    val horario: String,
    val citas: List<CitaUi>,
    val horasLibres: List<String> = emptyList(),
)

data class PacienteUi(
    val id: String,
    val nombre: String,
    val dui: String,
    val etiqueta: String? = null,
    val hospitalizado: Boolean = false,
    val ultimaVisita: String? = null,
)

data class AsignacionCamaUi(
    val habitacion: String,
    val cama: String,
    val tipo: TipoHabitacion,
    val desde: String,
    val ingreso: String,
    val diasHospitalizado: Int,
    val medicoTratante: String,
) {
    val codigo: String get() = if (cama.isEmpty()) habitacion else "$habitacion · $cama"
}

data class FichaPacienteUi(
    val paciente: PacienteUi,
    val edad: Int?,
    val telefono: String,
    val contactoEmergencia: String,
    val asignacion: AsignacionCamaUi?,
    val citas: List<CitaUi>,
    val numeroExpediente: String? = null,
    /** `dd/mm/aaaa`. */
    val nacimiento: String? = null,
    val sexo: Sexo? = null,
    val direccion: String? = null,
)

data class SolicitudCitaUi(
    val id: String,
    val paciente: String,
    val hace: String,
    val motivo: String,
    val especialidad: String,
    val preferencia: String,
)

data class CamaUi(
    val id: String,
    val etiqueta: String,
    val ocupante: String? = null,
) {
    val libre: Boolean get() = ocupante == null
}

data class HabitacionUi(
    val codigo: String,
    val tipo: TipoHabitacion,
    val camas: List<CamaUi>,
)

data class SolicitudAmbulanciaUi(
    val id: String,
    val paciente: String,
    val hora: String,
    val hace: String,
    val tipo: String,
    val ubicacion: String,
    val descripcion: String,
)

data class AmbulanciaUi(
    val estado: EstadoAmbulancia,
    val solicitudes: List<SolicitudAmbulanciaUi>,
    val servicioActual: String? = null,
)

enum class EstadoServicio { FINALIZADO, RECHAZADA, EN_MANTENIMIENTO }

data class ServicioBitacoraUi(
    val id: String,
    val fecha: String,
    val horario: String,
    val tipo: String,
    val paciente: String? = null,
    val duracion: String? = null,
    val estado: EstadoServicio,
)

data class DetalleServicioUi(
    val fecha: String,
    val despacho: String,
    val destino: String,
    val kilometraje: Int,
    val observaciones: String,
)

data class OrdenLabUi(
    val id: String,
    val numero: Int,
    val examen: String,
    val paciente: String,
    val dui: String,
    val doctor: String,
    val hora: String,
    val estado: EstadoLaboratorio,
    val indicaciones: String = "",
    val ordenada: String = "",
    val enProcesoDesde: String? = null,
    val resultado: String? = null,
    val nueva: Boolean = false,
)

data class InicioRecepcionUi(
    val turno: TurnoUi,
    val solicitudAmbulancia: SolicitudAmbulanciaUi?,
    val solicitudesCitas: Int,
    val laboratorioPendiente: Int,
    val ocupacion: List<OcupacionUi>,
    val citasHoyTotal: Int,
    val citasHoy: List<CitaUi>,
)

data class AgendaUi(
    val turno: TurnoUi?,
    val dias: List<String>,
    val diaSeleccionado: Int,
    val citas: List<CitaUi>,
)

data class DetalleCitaUi(
    val cita: CitaUi,
    val edad: Int,
    val dui: String,
    val alergias: String?,
)

data class ConsultaUi(
    val fecha: String,
    val diagnostico: String,
    val doctor: String,
    val recetas: Int,
    val ordenes: Int,
)

data class ExpedienteUi(
    val paciente: PacienteUi,
    val edad: Int,
    val alergias: String?,
    val hospitalizacion: String?,
    val consultas: List<ConsultaUi>,
    val laboratorio: List<OrdenLabUi>,
    val telefono: String,
    val contactoEmergencia: String,
)

data class PerfilUi(
    val nombre: String,
    val rolYTipo: String,
    val correo: String?,
    val telefono: String?,
)
