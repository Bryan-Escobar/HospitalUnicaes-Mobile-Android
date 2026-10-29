package sv.edu.unicaes.hospital.ui.recepcion

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.MuestraViewModel
import sv.edu.unicaes.hospital.ui.modelo.OrdenLabUi
import javax.inject.Inject

/** AND-R15 · Cola de laboratorio con sus conteos por estado. */
@HiltViewModel
class ColaLaboratorioViewModel
    @Inject
    constructor() :
    MuestraViewModel<List<OrdenLabUi>>({ DatosMuestra.colaLaboratorio }) {
        val conteos = DatosMuestra.conteoLaboratorio
    }

/** AND-R16 · Detalle de orden. La orden de orina trae el detalle completo del handoff. */
@HiltViewModel
class DetalleOrdenViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
    ) : MuestraViewModel<OrdenLabUi>({
            // Mismo nombre que la propiedad de DetalleOrdenRuta.
            ordenDeMuestra(checkNotNull(savedStateHandle["ordenId"]))
        })

/** AND-R17 · Cargar resultado. */
@HiltViewModel
class CargarResultadoViewModel
    @Inject
    constructor() : MuestraViewModel<OrdenLabUi>({ DatosMuestra.ordenOrina })

internal fun ordenDeMuestra(id: String): OrdenLabUi =
    DatosMuestra.colaLaboratorio
        .firstOrNull { it.id == id }
        ?.let { if (it.numero == DatosMuestra.ordenOrina.numero) DatosMuestra.ordenOrina else it }
        ?: DatosMuestra.ordenOrina
