package sv.edu.unicaes.hospital.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import sv.edu.unicaes.hospital.data.remote.dto.ApiRespuesta
import sv.edu.unicaes.hospital.data.remote.dto.CitaDto
import sv.edu.unicaes.hospital.data.remote.dto.HospitalizacionDto
import sv.edu.unicaes.hospital.data.remote.dto.PaginaDto
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.di.NetworkModule
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion

class HospitalizacionDtoTest {
    private val json: Json = NetworkModule.provideJson()

    @Test
    fun `lee la hospitalizacion activa de la API`() {
        val respuesta =
            """
            {"statusCode":200,"success":true,"message":"x","data":{"items":[{
              "id":"h1","paciente":{"id":"p1","nombres":"Luis","apellidos":"Martínez"},
              "cama":{"id":"c1","codigo":"HAB-204-A","habitacion":{"id":"r1","codigo":"HAB-204","planta":2,"tipo":"semiprivada"}},
              "doctor_tratante":{"id":"d1","nombres":"Carlos","apellidos":"Molina","especialidad":null},
              "fecha_ingreso":"2026-10-19T22:40:00.000Z","fecha_alta":null,"motivo_ingreso":null,
              "notas_alta":null,"estado":"activa"}],
              "paginacion":{"pagina":1,"por_pagina":1,"total":1,"total_paginas":1}}}
            """.trimIndent()

        val hospitalizacion =
            json
                .decodeFromString<ApiRespuesta<PaginaDto<HospitalizacionDto>>>(respuesta)
                .data.items
                .single()
                .aDominio()

        assertEquals("HAB-204", hospitalizacion.habitacion)
        assertEquals("A", hospitalizacion.cama)
        assertEquals(TipoHabitacion.SEMIPRIVADA, hospitalizacion.tipo)
        assertEquals("Carlos Molina", hospitalizacion.medicoTratante)
    }

    @Test
    fun `cama VIP`() {
        val respuesta =
            """
            {"id":"h2","cama":{"codigo":"VIP-301-A","habitacion":{"codigo":"VIP-301","planta":3,"tipo":"vip"}},
             "doctor_tratante":{"nombres":"Ana","apellidos":"Rivas"},"fecha_ingreso":"2026-10-19T22:40:00Z"}
            """.trimIndent()
        val hospitalizacion = json.decodeFromString<HospitalizacionDto>(respuesta).aDominio()
        assertEquals(TipoHabitacion.VIP, hospitalizacion.tipo)
        assertEquals("A", hospitalizacion.cama)
    }

    @Test
    fun `lee una fila de v_citas`() {
        val fila =
            """
            {"id":"c1","paciente_id":"p1","paciente_nombres":"Luis","paciente_apellidos":"Martínez",
             "doctor_id":"d1","doctor_nombres":"Ana","doctor_apellidos":"Rivas","especialidad_id":null,
             "especialidad":"Medicina general","fecha_hora":"2026-10-28T16:30:00+00:00","duracion_minutos":30,
             "fecha_hora_fin":"2026-10-28T17:00:00+00:00","motivo":"Control","estado":"no_asistio",
             "created_at":"2026-10-20T10:00:00+00:00"}
            """.trimIndent()
        val cita = json.decodeFromString<CitaDto>(fila).aDominio()
        assertEquals("Ana Rivas", cita.doctor)
        assertEquals(EstadoCita.NO_ASISTIO, cita.estado)
    }

    @Test
    fun `estados de cita de la base de datos`() {
        assertEquals(EstadoCita.CONFIRMADA, EstadoCita.desdeApi("confirmada"))
        assertEquals(EstadoCita.ATENDIDA, EstadoCita.desdeApi("atendida"))
        assertEquals(EstadoCita.PROGRAMADA, EstadoCita.desdeApi("otro"))
    }
}
