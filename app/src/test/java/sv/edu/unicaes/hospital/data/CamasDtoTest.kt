package sv.edu.unicaes.hospital.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sv.edu.unicaes.hospital.data.remote.dto.ApiRespuesta
import sv.edu.unicaes.hospital.data.remote.dto.CrearHospitalizacionBody
import sv.edu.unicaes.hospital.data.remote.dto.HabitacionMapaDto
import sv.edu.unicaes.hospital.data.remote.dto.PaginaDto
import sv.edu.unicaes.hospital.data.remote.dto.PersonalDto
import sv.edu.unicaes.hospital.data.remote.dto.aDoctor
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.di.NetworkModule
import sv.edu.unicaes.hospital.domain.model.EstadoCama
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion

class CamasDtoTest {
    private val json: Json = NetworkModule.provideJson()

    @Test
    fun `lee el mapa de habitaciones con ocupantes y estados`() {
        val respuesta =
            """
            {"statusCode":200,"success":true,"message":"Habitaciones","data":[
              {"id":"r1","codigo":"HAB-204","planta":2,"tipo":"semiprivada","capacidad":2,"activa":true,"camas":[
                {"id":"c1","codigo":"HAB-204-A","estado":"ocupada","ocupante":{"hospitalizacion_id":"h1",
                  "paciente":{"id":"p1","nombres":"Luis Alberto","apellidos":"Martínez Rivas","sexo":"masculino"},
                  "fecha_ingreso":"2026-10-19T22:40:00.000Z"}},
                {"id":"c2","codigo":"HAB-204-B","estado":"limpieza","ocupante":null}]},
              {"id":"r2","codigo":"VIP-301","planta":3,"tipo":"vip","capacidad":1,"activa":true,"camas":[
                {"id":"c3","codigo":"VIP-301-A","estado":"disponible","ocupante":null}]}]}
            """.trimIndent()

        val habitaciones =
            json.decodeFromString<ApiRespuesta<List<HabitacionMapaDto>>>(respuesta).data.map { it.aDominio() }

        val (semiprivada, vip) = habitaciones
        assertEquals(TipoHabitacion.SEMIPRIVADA, semiprivada.tipo)
        assertEquals(listOf("A", "B"), semiprivada.camas.map { it.letra })
        val ocupada = semiprivada.camas[0]
        assertEquals(EstadoCama.OCUPADA, ocupada.estado)
        assertEquals("h1", ocupada.ocupante?.hospitalizacionId)
        assertEquals("p1", ocupada.ocupante?.pacienteId)
        assertEquals("L. Martínez", ocupada.ocupante?.nombreCorto)
        assertEquals(EstadoCama.LIMPIEZA, semiprivada.camas[1].estado)
        assertNull(semiprivada.camas[1].ocupante)
        // VIP: una sola cama, sin letra.
        assertEquals(TipoHabitacion.VIP, vip.tipo)
        assertEquals("", vip.camas.single().letra)
        assertEquals(EstadoCama.DISPONIBLE, vip.camas.single().estado)
    }

    @Test
    fun `un estado de cama desconocido nunca se ofrece como disponible`() {
        assertEquals(EstadoCama.MANTENIMIENTO, EstadoCama.desdeApi("otro"))
    }

    @Test
    fun `el body de ingreso omite lo que no se eligio`() {
        val body =
            json.encodeToString(
                CrearHospitalizacionBody.serializer(),
                CrearHospitalizacionBody("p1", "d1", "c1"),
            )

        assertEquals("""{"paciente_id":"p1","doctor_tratante_id":"d1","cama_id":"c1"}""", body)
    }

    @Test
    fun `lee el personal para elegir medico tratante`() {
        val respuesta =
            """
            {"statusCode":200,"success":true,"message":"x","data":{"items":[
              {"id":"d1","perfil_id":"u1","nombres":"Ana","apellidos":"Rivas","email":null,"telefono":null,
               "rol":"doctor","tipo":{"id":"t1","nombre":"Doctor"},"especialidad":{"id":"e1","nombre":"Pediatría"},
               "codigo_empleado":"EMP-1","num_licencia":null,"fecha_contratacion":null,"activo":true}],
              "paginacion":{"pagina":1,"por_pagina":100,"total":1,"total_paginas":1}}}
            """.trimIndent()

        val personal =
            json
                .decodeFromString<ApiRespuesta<PaginaDto<PersonalDto>>>(respuesta)
                .data.items
                .single()

        assertEquals("doctor", personal.rol)
        val doctor = personal.aDoctor()
        assertEquals("Ana Rivas", doctor.nombre)
        assertEquals("Pediatría", doctor.especialidad)
    }
}
