package sv.edu.unicaes.hospital.domain.model

// Registro clínico del doctor (HUNI-57, HUNI-61). Privado: solo lo ven el médico tratante, el propio
// paciente y la administración (context.md §3.4). Recepción no entra.

/** GET /pacientes/:id/historial: ficha, expediente y consultas de la más reciente a la más antigua. */
data class HistorialClinico(
    val paciente: Paciente,
    val expediente: Expediente,
    val consultas: List<ConsultaClinica>,
)

data class Expediente(
    val numeroExpediente: String,
    val tipoSangre: String? = null,
    /** Texto libre ("Penicilina, ibuprofeno"); null si no hay alergias registradas. */
    val alergias: String? = null,
    val antecedentes: String? = null,
    val enfermedadesCronicas: String? = null,
)

/** Una consulta con lo que se registró en ella. */
data class ConsultaClinica(
    val id: String,
    val citaId: String? = null,
    /** ISO 8601 con zona, como la API. */
    val fecha: String,
    val doctor: String,
    val motivo: String? = null,
    val notas: String? = null,
    /** Una consulta cerrada ya no se edita. */
    val cerrada: Boolean = false,
    val diagnosticos: List<Diagnostico> = emptyList(),
    val recetas: List<RecetaEmitida> = emptyList(),
    val ordenes: List<OrdenResumen> = emptyList(),
) {
    /** El diagnóstico principal; si no hay, el primero; si no hay ninguno, el motivo de consulta. */
    val tituloDiagnostico: String?
        get() =
            (diagnosticos.firstOrNull { it.principal } ?: diagnosticos.firstOrNull())?.descripcion
                ?: motivo?.takeIf { it.isNotBlank() }
}

data class Diagnostico(
    val descripcion: String,
    val codigoCie10: String? = null,
    val principal: Boolean = true,
)

data class RecetaEmitida(
    val id: String,
    val indicaciones: String? = null,
    val medicamentos: List<String> = emptyList(),
)

/** `OrdenLaboratorioResumen` dentro del historial: sin los nombres de los exámenes. */
data class OrdenResumen(
    val id: String,
    val fechaSolicitud: String,
    val estado: EstadoLaboratorio,
    val totalExamenes: Int,
)

/** Examen del catálogo (GET /laboratorio/examenes). La orden se pide con su [id]. */
data class ExamenCatalogo(
    val id: String,
    val nombre: String,
    val categoria: String? = null,
    val tipoMuestra: String? = null,
)

/** POST /consultas. Si atiende una cita, la API la pasa a `atendida`. */
data class NuevaConsulta(
    val pacienteId: String,
    val citaId: String? = null,
    val motivo: String? = null,
    val notas: String? = null,
)

/** Un medicamento de la receta. Solo [medicamento] es obligatorio. */
data class NuevaReceta(
    val medicamento: String,
    val dosis: String? = null,
    val frecuencia: String? = null,
    val indicaciones: String? = null,
)

/** POST /laboratorio/ordenes. La orden nace con todos sus exámenes en Pendiente. */
data class NuevaOrden(
    val examenIds: List<String>,
    val indicaciones: String? = null,
    val prioridad: PrioridadLaboratorio = PrioridadLaboratorio.NORMAL,
)
