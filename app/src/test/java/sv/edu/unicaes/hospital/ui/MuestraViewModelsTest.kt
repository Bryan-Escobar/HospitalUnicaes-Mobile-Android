package sv.edu.unicaes.hospital.ui

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.ui.doctor.DetalleCitaViewModel
import sv.edu.unicaes.hospital.ui.doctor.ExpedienteViewModel
import sv.edu.unicaes.hospital.ui.doctor.NuevaConsultaViewModel
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.MuestraViewModel
import sv.edu.unicaes.hospital.ui.recepcion.ColaLaboratorioViewModel
import sv.edu.unicaes.hospital.ui.recepcion.DetalleOrdenViewModel
import sv.edu.unicaes.hospital.ui.recepcion.MapaCamasViewModel

/** ViewModels con datos de ejemplo de la navegación principal (HUNI-43). */
class MuestraViewModelsTest {
    private fun <T> MuestraViewModel<T>.datos(): T = (uiState.value as UiState.Exito).datos

    @Test
    fun mapaDeCamasRespetaLaCapacidadFija() {
        val habitaciones = MapaCamasViewModel().datos()

        assertEquals(11, habitaciones.size)
        assertEquals(17, habitaciones.sumOf { it.camas.size })
    }

    @Test
    fun recargarMantieneLosDatos() {
        val viewModel = ColaLaboratorioViewModel()
        val antes = viewModel.datos()

        viewModel.cargar()

        assertEquals(antes, viewModel.datos())
        assertEquals(DatosMuestra.conteoLaboratorio, viewModel.conteos)
    }

    @Test
    fun detalleDeOrdenLeeElIdDeLaRuta() {
        val otra = DatosMuestra.colaLaboratorio.first { it.numero != DatosMuestra.ordenOrina.numero }

        val viewModel = DetalleOrdenViewModel(SavedStateHandle(mapOf("ordenId" to otra.id)))

        assertEquals(otra, viewModel.datos())
    }

    @Test
    fun ordenDesconocidaMuestraLaDeEjemplo() {
        val viewModel = DetalleOrdenViewModel(SavedStateHandle(mapOf("ordenId" to "no-existe")))

        assertEquals(DatosMuestra.ordenOrina, viewModel.datos())
    }

    @Test
    fun detalleDeCitaYNuevaConsultaUsanLaCitaElegida() {
        val cita = DatosMuestra.agendaDoctor.citas[2]
        val ruta = SavedStateHandle(mapOf("citaId" to cita.id))

        val detalle = DetalleCitaViewModel(ruta).datos()
        val consulta = NuevaConsultaViewModel(ruta).datos()

        assertEquals(cita.id, detalle.cita.id)
        assertTrue(detalle.cita.fecha.endsWith(cita.hora))
        assertEquals(cita, consulta)
    }

    @Test
    fun expedienteMuestraAlPacienteElegido() {
        val paciente = DatosMuestra.misPacientes.first()

        val expediente = ExpedienteViewModel(SavedStateHandle(mapOf("pacienteId" to paciente.id))).datos()

        assertEquals(paciente, expediente.paciente)
    }
}
