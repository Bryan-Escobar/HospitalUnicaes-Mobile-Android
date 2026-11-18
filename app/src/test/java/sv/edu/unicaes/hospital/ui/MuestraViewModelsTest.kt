package sv.edu.unicaes.hospital.ui

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.ui.doctor.DetalleCitaViewModel
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.MuestraViewModel
import sv.edu.unicaes.hospital.ui.modelo.hora12
import sv.edu.unicaes.hospital.ui.recepcion.ColaLaboratorioViewModel
import sv.edu.unicaes.hospital.ui.recepcion.DetalleOrdenViewModel
import sv.edu.unicaes.hospital.ui.recepcion.MapaCamasViewModel

/**
 * ViewModels con datos de ejemplo de la navegación principal (HUNI-43). Las camas (HUNI-67) y los pacientes,
 * expediente y consulta del doctor (HUNI-61) ya usan la API.
 */
class MuestraViewModelsTest {
    private fun <T> MuestraViewModel<T>.datos(): T = (uiState.value as UiState.Exito).datos

    @Test
    fun mapaDeCamasDeEjemploRespetaLaCapacidadFija() {
        val habitaciones = DatosMuestra.planta2 + DatosMuestra.planta3

        assertEquals(11, habitaciones.size)
        assertEquals(17, habitaciones.sumOf { it.camas.size })
    }

    @Test
    fun detalleDeCitaUsaLaCitaElegida() {
        val cita = DatosMuestra.agendaDoctor.citas[2]

        val detalle = DetalleCitaViewModel(SavedStateHandle(mapOf("citaId" to cita.id))).datos()

        assertEquals(cita.id, detalle.cita.id)
        assertTrue(detalle.cita.fecha.endsWith(hora12(cita.hora)))
    }
}
