package sv.edu.unicaes.hospital.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sv.edu.unicaes.hospital.data.remote.dto.ApiRespuesta
import sv.edu.unicaes.hospital.data.remote.dto.CrearDiagnosticoBody
import sv.edu.unicaes.hospital.data.remote.dto.ExamenCatalogoDto
import sv.edu.unicaes.hospital.data.remote.dto.HistorialClinicoDto
import sv.edu.unicaes.hospital.data.remote.dto.TIPO_PRINCIPAL
import sv.edu.unicaes.hospital.data.remote.dto.aBody
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.di.NetworkModule
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.NuevaConsulta
import sv.edu.unicaes.hospital.domain.model.NuevaOrden
import sv.edu.unicaes.hospital.domain.model.NuevaReceta
import sv.edu.unicaes.hospital.domain.model.PrioridadLaboratorio

/** Contrato de GET /pacientes/:id/historial, GET /laboratorio/examenes y los bodies de la consulta (HUNI-61). */
class ConsultaDtoTest {
    private val json: Json = NetworkModule.provideJson()

    private val historial =
        """
        {"statusCode":200,"success":true,"message":"x","data":{
          "paciente":{"id":"p1","perfil_id":null,"nombres":"Tomás","apellidos":"Mora",
            "documento_identidad":"06677889-3","fecha_nacimiento":"1974-03-02","sexo":"masculino",
            "telefono":"7000-4444","direccion":null,"contacto_emergencia":"Lucía Mora",
            "telefono_emergencia":"7000-5555","numero_expediente":"EXP-0006","activo":true,
            "created_at":"2026-10-01T10:00:00.000Z"},
          "expediente":{"id":"e1","paciente_id":"p1","numero_expediente":"EXP-0006","tipo_sangre":"O+",
            "alergias":"Ibuprofeno","antecedentes":"  ","enfermedades_cronicas":null,
            "created_at":"2026-10-01T10:00:00.000Z","updated_at":"2026-10-01T10:00:00.000Z"},
          "consultas":[{
            "id":"c1","expediente_id":"e1",
            "paciente":{"id":"p1","nombres":"Tomás","apellidos":"Mora"},
            "doctor":{"id":"d1","nombres":"Jorge","apellidos":"Peña","especialidad":"Emergencias"},
            "cita_id":"ci1","fecha":"2026-10-20T16:00:00.000Z","motivo_consulta":"Dolor abdominal",
            "signos_vitales":null,"examen_fisico":null,"notas":"Dolor en fosa ilíaca derecha",
            "cerrada_en":"2026-10-20T16:30:00.000Z",
            "diagnosticos":[
              {"id":"g2","consulta_id":"c1","codigo_cie10":null,"descripcion":"Deshidratación leve","tipo":"secundario",
               "registrado_por":{"id":"d1","nombres":"Jorge","apellidos":"Peña"},"created_at":"2026-10-20T16:10:00.000Z"},
              {"id":"g1","consulta_id":"c1","codigo_cie10":"K35.8","descripcion":"Apendicitis aguda","tipo":"principal",
               "registrado_por":{"id":"d1","nombres":"Jorge","apellidos":"Peña"},"created_at":"2026-10-20T16:05:00.000Z"}],
            "recetas":[{"id":"r1","consulta_id":"c1","fecha_emision":"2026-10-20T16:20:00.000Z",
              "registrado_por":{"id":"d1","nombres":"Jorge","apellidos":"Peña"},"indicaciones":"Con alimentos",
              "detalles":[{"id":"rd1","medicamento":"Amoxicilina 500 mg","dosis":"1 cápsula","frecuencia":"Cada 8 horas",
                "duracion":"7 días","cantidad":21,"item_id":null}]}],
            "ordenes_laboratorio":[{"id":"o1","fecha_solicitud":"2026-10-20T16:25:00.000Z",
              "registrado_por":{"id":"d1","nombres":"Jorge","apellidos":"Peña"},"prioridad":"urgente",
              "estado":"en_proceso","total_examenes":2}]
          },{
            "id":"c0","expediente_id":"e1",
            "paciente":{"id":"p1","nombres":"Tomás","apellidos":"Mora"},
            "doctor":{"id":"d2","nombres":"Carlos","apellidos":"Molina","especialidad":null},
            "cita_id":null,"fecha":"2026-06-03T15:00:00.000Z","motivo_consulta":"Gastritis",
            "signos_vitales":null,"examen_fisico":null,"notas":null,"cerrada_en":null,
            "diagnosticos":[],"recetas":[],"ordenes_laboratorio":[]
          }]}}
        """.trimIndent()

    @Test
    fun `lee el historial con expediente, diagnosticos, recetas y ordenes`() {
        val datos = json.decodeFromString<ApiRespuesta<HistorialClinicoDto>>(historial).data.aDominio()

        assertEquals("Tomás Mora", datos.paciente.nombreCompleto)
        assertEquals("1974-03-02", datos.paciente.fechaNacimiento)
        assertEquals("Ibuprofeno", datos.expediente.alergias)
        assertEquals("O+", datos.expediente.tipoSangre)
        // Texto en blanco del expediente = sin dato.
        assertNull(datos.expediente.antecedentes)

        val consulta = datos.consultas.first()
        assertEquals("Jorge Peña", consulta.doctor)
        assertEquals("ci1", consulta.citaId)
        assertTrue(consulta.cerrada)
        // El principal manda aunque no sea el primero de la lista.
        assertEquals("Apendicitis aguda", consulta.tituloDiagnostico)
        assertEquals(listOf("Amoxicilina 500 mg"), consulta.recetas.single().medicamentos)
        val orden = consulta.ordenes.single()
        assertEquals(EstadoLaboratorio.EN_PROCESO, orden.estado)
        assertEquals(2, orden.totalExamenes)

        // Sin diagnóstico, la consulta se titula con el motivo.
        val sinDiagnostico = datos.consultas[1]
        assertFalse(sinDiagnostico.cerrada)
        assertEquals("Gastritis", sinDiagnostico.tituloDiagnostico)
    }

    @Test
    fun `lee el catalogo de examenes`() {
        val respuesta =
            """
            {"statusCode":200,"success":true,"message":"x","data":[
              {"id":"x1","nombre":"Hemograma completo","categoria":"Hematología","tipo_muestra":"Sangre venosa",
               "tiempo_entrega_horas":24,"activo":true}]}
            """.trimIndent()

        val examen =
            json
                .decodeFromString<ApiRespuesta<List<ExamenCatalogoDto>>>(respuesta)
                .data
                .single()
                .aDominio()

        assertEquals("x1", examen.id)
        assertEquals("Hemograma completo", examen.nombre)
        assertEquals("Sangre venosa", examen.tipoMuestra)
    }

    private inline fun <reified T> cuerpo(body: T): JsonObject =
        json.parseToJsonElement(json.encodeToString(body)).jsonObject

    @Test
    fun `el body de la consulta omite la cita y las notas vacias`() {
        val conCita = cuerpo(NuevaConsulta("p1", citaId = "ci1", notas = "  Dolor  ").aBody())
        val sinCita = cuerpo(NuevaConsulta("p1", notas = "   ").aBody())

        assertEquals("""{"paciente_id":"p1","cita_id":"ci1","notas":"Dolor"}""", conCita.toString())
        assertEquals("""{"paciente_id":"p1"}""", sinCita.toString())
    }

    @Test
    fun `el diagnostico se envia como principal`() {
        assertEquals(
            """{"descripcion":"Apendicitis aguda","tipo":"principal"}""",
            cuerpo(CrearDiagnosticoBody("Apendicitis aguda", TIPO_PRINCIPAL)).toString(),
        )
    }

    @Test
    fun `la receta lleva un medicamento con lo que se escribio`() {
        val body =
            cuerpo(NuevaReceta(" Amoxicilina ", dosis = "1 cápsula", frecuencia = "", indicaciones = null).aBody())

        assertEquals("""{"detalles":[{"medicamento":"Amoxicilina","dosis":"1 cápsula"}]}""", body.toString())
    }

    @Test
    fun `la orden siempre envia la prioridad y los examenes`() {
        val normal = cuerpo(NuevaOrden(listOf("x1"), indicaciones = "Ayuno").aBody("c1"))
        val urgente = cuerpo(NuevaOrden(listOf("x1", "x2"), prioridad = PrioridadLaboratorio.URGENTE).aBody("c1"))

        assertEquals(
            """{"consulta_id":"c1","prioridad":"normal","indicaciones":"Ayuno","examen_ids":["x1"]}""",
            normal.toString(),
        )
        assertEquals("""{"consulta_id":"c1","prioridad":"urgente","examen_ids":["x1","x2"]}""", urgente.toString())
    }
}
