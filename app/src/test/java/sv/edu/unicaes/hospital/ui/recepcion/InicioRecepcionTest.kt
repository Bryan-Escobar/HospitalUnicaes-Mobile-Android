package sv.edu.unicaes.hospital.ui.recepcion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sv.edu.unicaes.hospital.domain.model.Cama
import sv.edu.unicaes.hospital.domain.model.Cita
import sv.edu.unicaes.hospital.domain.model.EstadoCama
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.domain.model.Habitacion
import sv.edu.unicaes.hospital.domain.model.SolicitudAmbulancia
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion
import sv.edu.unicaes.hospital.domain.model.TipoTurno
import sv.edu.unicaes.hospital.domain.model.Turno
import sv.edu.unicaes.hospital.ui.modelo.fechaHoraHospital

class InicioRecepcionTest {
    private val ahora = checkNotNull(fechaHoraHospital("2026-11-17", "17:00"))

    @Test
    fun `la bandeja muestra la solicitud pendiente mas reciente y el conteo`() {
        val inicio =
            armarInicio(
                turnos = emptyList(),
                pendientes =
                    listOf(
                        solicitud("vieja", "2026-10-28T19:52:00-06:00"),
                        solicitud("nueva", "2026-11-17T17:39:00-06:00", tipo = "cardiaca"),
                    ),
                laboratorioPendiente = 7,
                habitaciones = emptyList(),
                citas = emptyList(),
                ahora = ahora,
            )

        assertEquals("nueva", inicio.solicitudAmbulancia?.id)
        assertEquals("Cardíaca", inicio.solicitudAmbulancia?.tipo)
        assertEquals(2, inicio.solicitudesAmbulancia)
        assertEquals(7, inicio.laboratorioPendiente)
        assertTrue(inicio.turno.sinTurno)
    }

    @Test
    fun `sin pendientes no hay tarjeta de ambulancia`() {
        val inicio =
            armarInicio(
                turnos = emptyList(),
                pendientes = emptyList(),
                laboratorioPendiente = 0,
                habitaciones = emptyList(),
                citas = emptyList(),
                ahora = ahora,
            )

        assertNull(inicio.solicitudAmbulancia)
        assertEquals(0, inicio.solicitudesAmbulancia)
    }

    @Test
    fun `el turno en curso gana sobre el que ya termino`() {
        val inicio =
            armarInicio(
                turnos =
                    listOf(
                        turno("07:00", "15:00", "2026-11-17T13:00:00Z", "2026-11-17T21:00:00Z"),
                        turno("15:00", "23:00", "2026-11-17T21:00:00Z", "2026-11-18T05:00:00Z", TipoTurno.TARDE),
                    ),
                pendientes = emptyList(),
                laboratorioPendiente = 0,
                habitaciones = emptyList(),
                citas = emptyList(),
                ahora = ahora,
            )

        assertEquals("3:00 p. m. – 11:00 p. m.", inicio.turno.horario)
        assertEquals("Recepción", inicio.turno.area)
        assertEquals("Recepcionista", inicio.turno.tipo)
        assertTrue(inicio.turno.enCurso)
    }

    @Test
    fun `la ocupacion cuenta como libres solo las camas disponibles`() {
        val inicio =
            armarInicio(
                turnos = emptyList(),
                pendientes = emptyList(),
                laboratorioPendiente = 0,
                habitaciones =
                    listOf(
                        habitacion(
                            "h2",
                            TipoHabitacion.SEMIPRIVADA,
                            cama("a", EstadoCama.OCUPADA),
                            cama("b", EstadoCama.DISPONIBLE),
                        ),
                        habitacion("h3", TipoHabitacion.VIP, cama("v", EstadoCama.LIMPIEZA)),
                    ),
                citas =
                    listOf(
                        Cita(
                            id = "c1",
                            fechaHora = "2026-11-17T16:30:00-06:00",
                            doctor = "Dra. Rivas",
                            motivo = "Control",
                            estado = EstadoCita.PROGRAMADA,
                            paciente = "Luis Martínez",
                        ),
                    ),
                ahora = ahora,
            )

        val semiprivada = inicio.ocupacion.first { it.tipo == TipoHabitacion.SEMIPRIVADA }
        val vip = inicio.ocupacion.first { it.tipo == TipoHabitacion.VIP }
        assertEquals(1, semiprivada.ocupadas)
        assertEquals(2, semiprivada.total)
        assertEquals(1, semiprivada.libres)
        assertEquals(1, vip.ocupadas)
        assertEquals(0, vip.libres)
        assertEquals(1, inicio.citasHoyTotal)
        assertEquals("Luis Martínez", inicio.citasHoy.single().paciente)
    }

    private fun solicitud(
        id: String,
        fecha: String,
        tipo: String? = null,
    ) = SolicitudAmbulancia(id = id, tipo = tipo, fechaSolicitud = fecha)

    private fun turno(
        horaInicio: String,
        horaFin: String,
        inicio: String,
        fin: String,
        tipo: TipoTurno = TipoTurno.MANANA,
    ) = Turno(
        id = horaInicio,
        personalId = "p",
        persona = "Karla",
        tipoPersonal = "Recepcionista",
        esMedico = false,
        area = "Recepción",
        tipoTurno = tipo,
        fecha = "2026-11-17",
        horaInicio = horaInicio,
        horaFin = horaFin,
        inicio = inicio,
        fin = fin,
    )

    private fun habitacion(
        id: String,
        tipo: TipoHabitacion,
        vararg camas: Cama,
    ) = Habitacion(id = id, codigo = id, planta = tipo.planta, tipo = tipo, camas = camas.toList())

    private fun cama(
        id: String,
        estado: EstadoCama,
    ) = Cama(id = id, codigo = id, letra = "A", estado = estado)
}
