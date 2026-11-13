package sv.edu.unicaes.hospital.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sv.edu.unicaes.hospital.data.remote.dto.ApiRespuesta
import sv.edu.unicaes.hospital.data.remote.dto.CambiarEstadoExamenDto
import sv.edu.unicaes.hospital.data.remote.dto.OrdenLaboratorioDto
import sv.edu.unicaes.hospital.data.remote.dto.PaginaDto
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.di.NetworkModule
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.PrioridadLaboratorio
import sv.edu.unicaes.hospital.domain.model.TipoArchivoResultado

/** Contrato de GET /laboratorio/ordenes (esquema `OrdenLaboratorio`, HUNI-62 y HUNI-63). */
class LaboratorioDtoTest {
    private val json: Json = NetworkModule.provideJson()

    private fun resultado(archivo: String) =
        """
        {"id":"r1","valor":"92","unidad":"mg/dL","rango_referencia":"70 - 100","observaciones":null,
         $archivo,
         "validado_por":{"id":"s1","nombres":"Lucía","apellidos":"Herrera"},
         "fecha_resultado":"2026-11-09T18:10:00.000Z"}
        """.trimIndent()

    private val respuesta =
        """
        {"statusCode":200,"success":true,"message":"x","data":{"items":[{
          "id":"o1","fecha_solicitud":"2026-11-09T16:41:00.000Z",
          "registrado_por":{"id":"d1","nombres":"Carlos","apellidos":"Molina"},
          "prioridad":"urgente","estado":"en_proceso","consulta_id":"c1",
          "paciente":{"id":"p1","nombres":"Luis","apellidos":"Martínez"},
          "doctor":{"id":"d1","nombres":"Carlos","apellidos":"Molina","especialidad":"Medicina interna"},
          "indicaciones":"  ",
          "examenes":[
            {"id":"e1","examen":{"id":"x1","nombre":"Hemograma","tipo_muestra":"Sangre"},"estado":"pendiente",
             "fecha_inicio":null,"fecha_completado":null,"resultado":null},
            {"id":"e2","examen":{"id":"x2","nombre":"Glucosa","tipo_muestra":null},"estado":"completado",
             "fecha_inicio":"2026-11-09T17:00:00.000Z","fecha_completado":"2026-11-09T18:10:00.000Z",
             "resultado":${resultado("\"tiene_archivo\":true,\"tipo_archivo\":\"imagen\"")}},
            {"id":"e3","examen":{"id":"x3","nombre":"Orina","tipo_muestra":"Orina"},"estado":"completado",
             "fecha_inicio":null,"fecha_completado":null,
             "resultado":${resultado("\"tiene_archivo\":true")}},
            {"id":"e4","examen":{"id":"x4","nombre":"Perfil","tipo_muestra":null},"estado":"completado",
             "fecha_inicio":null,"fecha_completado":null,
             "resultado":${resultado("\"tiene_archivo\":false,\"tipo_archivo\":null")}}
          ]}],
          "paginacion":{"pagina":1,"por_pagina":20,"total":41,"total_paginas":3}}}
        """.trimIndent()

    @Test
    fun `lee la orden con sus examenes y resultados`() {
        val pagina =
            json
                .decodeFromString<ApiRespuesta<PaginaDto<OrdenLaboratorioDto>>>(respuesta)
                .data
                .aDominio { it.aDominio() }
        val orden = pagina.items.single()

        assertEquals(41, pagina.total)
        assertEquals(true, pagina.hayMas)
        assertEquals(EstadoLaboratorio.EN_PROCESO, orden.estado)
        assertEquals(PrioridadLaboratorio.URGENTE, orden.prioridad)
        assertEquals("Luis Martínez", orden.paciente)
        assertEquals("Carlos Molina", orden.doctor)
        assertEquals("Medicina interna", orden.especialidad)
        assertNull("indicaciones en blanco no se muestran", orden.indicaciones)
        assertEquals(EstadoLaboratorio.PENDIENTE, orden.examenes[0].estado)
        assertEquals("Sangre", orden.examenes[0].tipoMuestra)

        val glucosa = orden.examenes[1].resultado!!
        assertEquals("92", glucosa.valor)
        assertEquals("Lucía Herrera", glucosa.validadoPor)
        assertEquals(TipoArchivoResultado.IMAGEN, glucosa.tipoArchivo)
    }

    @Test
    fun `sin tipo_archivo el adjunto es PDF y sin archivo no hay tipo`() {
        val orden =
            json
                .decodeFromString<ApiRespuesta<PaginaDto<OrdenLaboratorioDto>>>(respuesta)
                .data.items
                .single()
                .aDominio()

        assertEquals(TipoArchivoResultado.PDF, orden.examenes[2].resultado?.tipoArchivo)
        assertNull(orden.examenes[3].resultado?.tipoArchivo)
    }

    @Test
    fun `estados y prioridades desconocidos usan el valor por defecto`() {
        assertEquals(EstadoLaboratorio.EN_PROCESO, EstadoLaboratorio.desdeApi("en_proceso"))
        assertEquals(EstadoLaboratorio.PENDIENTE, EstadoLaboratorio.desdeApi("otro"))
        assertEquals(PrioridadLaboratorio.CRITICA, PrioridadLaboratorio.desdeApi("critica"))
        assertEquals(PrioridadLaboratorio.NORMAL, PrioridadLaboratorio.desdeApi("otra"))
    }

    @Test
    fun `el body para pasar a En Proceso lleva el estado aunque el Json omita los valores por defecto`() {
        // Antes salía `{}` y la API respondía 400 VALIDACION ("Revisa los campos marcados").
        assertEquals(
            """{"estado":"en_proceso"}""",
            json.encodeToString(CambiarEstadoExamenDto.serializer(), CambiarEstadoExamenDto.EN_PROCESO),
        )
    }
}
