package sv.edu.unicaes.hospital.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sv.edu.unicaes.hospital.data.remote.dto.ApiRespuesta
import sv.edu.unicaes.hospital.data.remote.dto.PacienteBodyDto
import sv.edu.unicaes.hospital.data.remote.dto.PacienteDto
import sv.edu.unicaes.hospital.data.remote.dto.PaginaDto
import sv.edu.unicaes.hospital.data.remote.dto.aBody
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.di.NetworkModule
import sv.edu.unicaes.hospital.domain.model.DatosPaciente
import sv.edu.unicaes.hospital.domain.model.Sexo

class PacienteDtoTest {
    private val json: Json = NetworkModule.provideJson()

    @Test
    fun `lee una pagina de pacientes de la API`() {
        val respuesta =
            """
            {"statusCode":200,"success":true,"message":"Página de pacientes","data":{
              "items":[{"id":"a1","perfil_id":null,"nombres":"Carlos","apellidos":"Martínez Rivas",
                "documento_identidad":"01234567-8","fecha_nacimiento":"1990-05-14","sexo":"masculino",
                "telefono":"7000-0000","direccion":null,"contacto_emergencia":"María Rivas",
                "telefono_emergencia":null,"numero_expediente":"EXP-2026-00015","activo":true,
                "created_at":"2026-10-27T10:00:00.000Z"}],
              "paginacion":{"pagina":1,"por_pagina":20,"total":21,"total_paginas":2}}}
            """.trimIndent()

        val pagina =
            json
                .decodeFromString<ApiRespuesta<PaginaDto<PacienteDto>>>(respuesta)
                .data
                .aDominio { it.aDominio() }

        val paciente = pagina.items.single()
        assertEquals("Carlos Martínez Rivas", paciente.nombreCompleto)
        assertEquals("01234567-8", paciente.documento)
        assertEquals(Sexo.MASCULINO, paciente.sexo)
        assertEquals("EXP-2026-00015", paciente.numeroExpediente)
        assertTrue(pagina.hayMas)
    }

    @Test
    fun `el body no envia los campos null`() {
        val body = json.encodeToString(PacienteBodyDto.serializer(), DatosPaciente(telefono = "7000-0000").aBody())
        assertEquals("""{"telefono":"7000-0000"}""", body)
    }

    @Test
    fun `el body usa los nombres de la API`() {
        val body =
            json.encodeToString(
                PacienteBodyDto.serializer(),
                DatosPaciente(documento = "01234567-8", sexo = Sexo.OTRO, telefonoEmergencia = "7000-1111").aBody(),
            )
        assertTrue(body.contains(""""documento_identidad":"01234567-8""""))
        assertTrue(body.contains(""""sexo":"otro""""))
        assertTrue(body.contains(""""telefono_emergencia":"7000-1111""""))
        assertFalse(body.contains("nombres"))
    }
}
