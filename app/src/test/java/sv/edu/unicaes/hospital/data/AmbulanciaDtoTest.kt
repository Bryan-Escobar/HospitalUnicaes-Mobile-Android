package sv.edu.unicaes.hospital.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.CODIGO_AMBULANCIA_NO_DISPONIBLE
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.safeApiCall
import sv.edu.unicaes.hospital.data.remote.dto.AmbulanciaDto
import sv.edu.unicaes.hospital.data.remote.dto.ApiRespuesta
import sv.edu.unicaes.hospital.data.remote.dto.BitacoraAmbulanciaDto
import sv.edu.unicaes.hospital.data.remote.dto.PaginaDto
import sv.edu.unicaes.hospital.data.remote.dto.SolicitudAmbulanciaDto
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.di.NetworkModule
import sv.edu.unicaes.hospital.domain.model.EstadoAmbulancia

class AmbulanciaDtoTest {
    private val json: Json = NetworkModule.provideJson()

    @Test
    fun `lee el estado y la solicitud activa`() {
        val respuesta =
            """
            {"statusCode":200,"success":true,"message":"Ambulancia","data":{
              "id":"a1","placa":"AMB-001","estado":"en_servicio","ubicacion_actual":null,
              "updated_at":"2026-11-17T16:14:00.000Z",
              "solicitud_activa":{"id":"s1","prioridad":"urgente","estado":"despachada",
                "tipo_emergencia":"cardiaca","direccion_origen":"Col. Escalón",
                "latitud":13.7,"longitud":-89.2,"fecha_despacho":"2026-11-17T16:14:00.000Z"}}}
            """.trimIndent()

        val ambulancia = json.decodeFromString<ApiRespuesta<AmbulanciaDto>>(respuesta).data.aDominio()

        assertEquals(EstadoAmbulancia.EN_SERVICIO, ambulancia.estado)
        assertEquals("s1", ambulancia.solicitudActiva?.id)
        assertEquals("cardiaca", ambulancia.solicitudActiva?.tipo)
        assertEquals(EstadoAmbulancia.EN_MANTENIMIENTO, EstadoAmbulancia.desdeApi("otro"))
    }

    @Test
    fun `lee una solicitud pendiente con paciente`() {
        val respuesta =
            """
            {"statusCode":200,"success":true,"message":"Página","data":{"items":[{
              "id":"s1","ambulancia_id":"a1","canal":"app",
              "solicitada_por":{"id":"u1","nombres":"Luis","apellidos":"Martínez","rol":"paciente"},
              "paciente":{"id":"p1","nombres":"Luis","apellidos":"Martínez"},
              "direccion_origen":"Col. Escalón","latitud":null,"longitud":null,"precision_m":null,
              "destino":null,"descripcion_emergencia":"dolor de pecho","tipo_emergencia":"cardiaca",
              "prioridad":"urgente","estado":"pendiente","telefono_contacto":null,"nombre_contacto":null,
              "fecha_solicitud":"2026-11-17T16:12:00.000Z","fecha_despacho":null,"fecha_en_curso":null,
              "fecha_cierre":null,"notas_cierre":null,"motivo_cancelacion":null}],
              "paginacion":{"pagina":1,"por_pagina":20,"total":1,"total_paginas":1}}}
            """.trimIndent()

        val solicitud =
            json
                .decodeFromString<ApiRespuesta<PaginaDto<SolicitudAmbulanciaDto>>>(respuesta)
                .data.items
                .single()
                .aDominio()

        assertEquals("Luis Martínez", solicitud.paciente)
        assertEquals("app", solicitud.canal)
        assertEquals("pendiente", solicitud.estado)
        assertEquals("cardiaca", solicitud.tipo)
        assertNull(solicitud.fechaDespacho)
    }

    @Test
    fun `la bitacora tolera una fila de mantenimiento sin solicitud`() {
        val cuerpo =
            """
            {"id":"b1","estado_anterior":"disponible","estado_nuevo":"en_mantenimiento",
             "descripcion":"Cambio de llantas","registrado_por":{"id":"u9","nombres":"Ana","apellidos":"Rivas"},
             "solicitud":null,"created_at":"2026-11-15T14:00:00.000Z"}
            """.trimIndent()

        val entrada = json.decodeFromString<BitacoraAmbulanciaDto>(cuerpo).aDominio()

        assertEquals(EstadoAmbulancia.EN_MANTENIMIENTO, entrada.estadoNuevo)
        assertEquals("Ana Rivas", entrada.registradoPor)
        assertNull(entrada.solicitud)
    }

    @Test
    fun `un 409 de ambulancia conserva el codigo para el banner`() =
        runBlocking {
            val cuerpo =
                """{"code":"AMBULANCIA_NO_DISPONIBLE","message":"La ambulancia no está disponible"}"""
                    .toResponseBody("application/json".toMediaType())
            val resultado =
                safeApiCall<Unit> {
                    throw HttpException(Response.error<Unit>(409, cuerpo))
                }

            assertEquals(
                AppError.Rechazada("La ambulancia no está disponible", CODIGO_AMBULANCIA_NO_DISPONIBLE),
                (resultado as Resultado.Fallo).error,
            )
        }
}
