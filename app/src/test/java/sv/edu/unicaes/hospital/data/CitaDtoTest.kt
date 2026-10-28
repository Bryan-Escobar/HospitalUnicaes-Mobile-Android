package sv.edu.unicaes.hospital.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import sv.edu.unicaes.hospital.data.remote.dto.CitaDto
import sv.edu.unicaes.hospital.data.remote.dto.DoctorDisponibleDto
import sv.edu.unicaes.hospital.data.remote.dto.TurnoDto
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.domain.model.EstadoCita

class CitaDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `lee paciente, doctor y duracion de v_citas`() {
        val fila =
            """
            {"id":"c1","paciente_id":"p1","paciente_nombres":"Luis","paciente_apellidos":"Martínez",
             "doctor_id":"d1","doctor_nombres":"Ana","doctor_apellidos":"Rivas","especialidad_id":"e1",
             "especialidad":"Alergista","fecha_hora":"2026-10-30T16:30:00+00:00","duracion_minutos":45,
             "fecha_hora_fin":"2026-10-30T17:15:00+00:00","motivo":null,"estado":"programada",
             "created_at":"2026-10-20T10:00:00+00:00"}
            """.trimIndent()
        val cita = json.decodeFromString<CitaDto>(fila).aDominio()

        assertEquals("p1", cita.pacienteId)
        assertEquals("Luis Martínez", cita.paciente)
        assertEquals("d1", cita.doctorId)
        assertEquals("Ana Rivas", cita.doctor)
        assertEquals("Alergista", cita.especialidad)
        assertEquals(45, cita.duracionMinutos)
        assertEquals(null, cita.motivo)
        assertEquals(EstadoCita.PROGRAMADA, cita.estado)
    }

    @Test
    fun `lee un turno de v_turnos`() {
        val fila =
            """
            {"id":"t1","personal_id":"d1","nombres":"Ana","apellidos":"Rivas","tipo_personal":"Alergista",
             "es_medico":true,"especialidad_id":"e1","especialidad":"Alergista","area":"Consulta externa",
             "tipo_turno":"manana","fecha":"2026-10-30","hora_inicio":"08:00:00","hora_fin":"14:00:00",
             "inicio":"2026-10-30T14:00:00+00:00","fin":"2026-10-30T20:00:00+00:00","notas":null}
            """.trimIndent()
        val turno = json.decodeFromString<TurnoDto>(fila).aDominio()

        assertEquals("d1", turno.doctorId)
        assertEquals("Ana Rivas", turno.doctor)
        assertEquals("2026-10-30T14:00:00+00:00", turno.inicio)
        assertEquals("2026-10-30T20:00:00+00:00", turno.fin)
    }

    @Test
    fun `lee un doctor de doctores_disponibles`() {
        val fila =
            """{"id":"d1","nombres":"Ana","apellidos":"Rivas","tipo_personal":"Alergista","especialidad_id":null,"especialidad":null}"""
        val doctor = json.decodeFromString<DoctorDisponibleDto>(fila).aDominio()

        assertEquals("Ana Rivas", doctor.nombre)
        assertEquals(null, doctor.especialidad)
    }
}
