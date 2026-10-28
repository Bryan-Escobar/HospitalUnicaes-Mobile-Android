package sv.edu.unicaes.hospital.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sv.edu.unicaes.hospital.domain.model.Cita
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.domain.model.TurnoMedico
import sv.edu.unicaes.hospital.ui.modelo.UN_DIA_MS
import sv.edu.unicaes.hospital.ui.modelo.ZONA_HOSPITAL
import sv.edu.unicaes.hospital.ui.modelo.agruparAgenda
import sv.edu.unicaes.hospital.ui.modelo.fechaDesdeIso
import sv.edu.unicaes.hospital.ui.modelo.fechaHoraHospital
import sv.edu.unicaes.hospital.ui.modelo.hora
import sv.edu.unicaes.hospital.ui.modelo.horasDisponibles
import sv.edu.unicaes.hospital.ui.modelo.huecosLibres
import java.util.Date

class HorariosCitaTest {
    // Lunes 2 de noviembre de 2026 en El Salvador (UTC−6): de 06:00Z a 06:00Z del día siguiente.
    private val desde = fechaHoraHospital("2026-11-02")!!
    private val hasta = Date(desde.time + UN_DIA_MS)
    private val antes = fechaDesdeIso("2026-11-01T00:00:00Z")!!

    // 08:00–10:00 hora local.
    private val turnoAna =
        TurnoMedico("d1", "Ana Rivas", "Alergista", "2026-11-02T14:00:00+00:00", "2026-11-02T16:00:00+00:00")

    private fun cita(
        id: String,
        iso: String,
        doctorId: String = "d1",
        minutos: Int = 30,
    ) = Cita(
        id = id,
        fechaHora = iso,
        doctor = "Ana Rivas",
        motivo = "Control",
        estado = EstadoCita.PROGRAMADA,
        pacienteId = "p1",
        paciente = "Luis Martínez",
        doctorId = doctorId,
        duracionMinutos = minutos,
    )

    private fun List<Date>.horas() = map { hora(it, ZONA_HOSPITAL) }

    @Test
    fun `huecos de 30 minutos sin la hora ocupada`() {
        val huecos =
            huecosLibres(listOf(turnoAna), listOf(cita("c1", "2026-11-02T15:00:00+00:00")), desde, hasta, antes)
        assertEquals(listOf("08:00", "08:30", "09:30"), huecos.horas())
    }

    @Test
    fun `una cita de una hora ocupa dos huecos`() {
        val larga = cita("c1", "2026-11-02T14:00:00+00:00", minutos = 60)
        assertEquals(
            listOf("09:00", "09:30"),
            huecosLibres(listOf(turnoAna), listOf(larga), desde, hasta, antes).horas(),
        )
    }

    @Test
    fun `no ofrece horas que ya pasaron`() {
        val ahora = fechaHoraHospital("2026-11-02", "08:45")!!
        assertEquals(listOf("09:00", "09:30"), huecosLibres(listOf(turnoAna), emptyList(), desde, hasta, ahora).horas())
    }

    @Test
    fun `el turno de noche del dia anterior aporta las horas de la madrugada`() {
        // Domingo 19:00 a lunes 07:00 hora local.
        val noche = TurnoMedico("d2", "Carlos Molina", null, "2026-11-02T01:00:00+00:00", "2026-11-02T13:00:00+00:00")
        val huecos = huecosLibres(listOf(noche), emptyList(), desde, hasta, antes).horas()
        assertEquals("00:00", huecos.first())
        assertEquals("06:30", huecos.last())
        assertEquals(14, huecos.size)
    }

    @Test
    fun `agrupa por doctor con horario, citas y huecos`() {
        val sinTurno = cita("c2", "2026-11-02T20:00:00+00:00", doctorId = "d9").copy(doctor = "Pedro Peña")
        val agendas =
            agruparAgenda(
                citas = listOf(cita("c1", "2026-11-02T15:00:00+00:00"), sinTurno),
                turnos = listOf(turnoAna),
                desde = desde,
                hasta = hasta,
                ahora = antes,
            )

        assertEquals(listOf("d1", "d9"), agendas.map { it.doctorId })
        val ana = agendas.first()
        assertEquals("Ana Rivas", ana.doctor)
        assertEquals("Alergista", ana.especialidad)
        assertEquals("08:00–10:00", ana.horario)
        assertEquals("09:00", ana.citas.single().hora)
        assertEquals("Luis Martínez", ana.citas.single().paciente)
        assertEquals(listOf("08:00", "08:30", "09:30"), ana.horasLibres)

        val pedro = agendas.last()
        assertEquals("Pedro Peña", pedro.doctor)
        assertEquals("", pedro.horario)
        assertEquals("14:00", pedro.citas.single().hora)
        assertTrue(pedro.horasLibres.isEmpty())
    }

    @Test
    fun `horas disponibles une los huecos de todos los doctores`() {
        val otro = TurnoMedico("d2", "Carlos Molina", null, "2026-11-02T15:00:00+00:00", "2026-11-02T16:00:00+00:00")
        val agendas =
            agruparAgenda(
                citas = listOf(cita("c1", "2026-11-02T15:00:00+00:00")),
                turnos = listOf(turnoAna, otro),
                desde = desde,
                hasta = hasta,
                ahora = antes,
            )
        // A las 09:00 Ana está ocupada pero Carlos está libre.
        assertEquals(listOf("08:00", "08:30", "09:00", "09:30"), horasDisponibles(agendas))
    }
}
