package sv.edu.unicaes.hospital.ui.recepcion

import androidx.annotation.StringRes
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.domain.model.DatosPaciente
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.domain.model.Sexo
import java.util.Calendar
import java.util.GregorianCalendar

/** Campos del formulario AND-R3, con el nombre que usa la API en los errores 400. */
enum class CampoPaciente(
    val campoApi: String,
) {
    NOMBRES("nombres"),
    APELLIDOS("apellidos"),
    DOCUMENTO("documento_identidad"),
    NACIMIENTO("fecha_nacimiento"),
    SEXO("sexo"),
    TELEFONO("telefono"),
    DIRECCION("direccion"),
    CONTACTO_NOMBRE("contacto_emergencia"),
    CONTACTO_TELEFONO("telefono_emergencia"),
    ;

    companion object {
        fun desdeApi(campo: String): CampoPaciente? = entries.firstOrNull { it.campoApi == campo }
    }
}

/** Lo que el usuario lleva escrito, tal cual: la fecha en `dd/mm/aaaa`. */
data class FormularioPaciente(
    val nombres: String = "",
    val apellidos: String = "",
    val documento: String = "",
    val nacimiento: String = "",
    val sexo: Sexo? = null,
    val telefono: String = "",
    val direccion: String = "",
    val contactoNombre: String = "",
    val contactoTelefono: String = "",
)

private val REGEX_DUI = Regex("""^\d{8}-\d$""")

// 8 dígitos con guion opcional, con o sin código de país: 7000-0000, 70000000, +503 7000-0000.
private val REGEX_TELEFONO = Regex("""^(\+503 ?)?\d{4}-?\d{4}$""")

private val REGEX_FECHA = Regex("""^(\d{2})/(\d{2})/(\d{4})$""")

private const val MAX_NOMBRE = 100
private const val MAX_DIRECCION = 250
private const val MAX_CONTACTO = 150
private const val ANIO_MINIMO = 1900

/**
 * Valida el formulario antes de llamar a la API (que vuelve a validar). Los obligatorios son los
 * marcados con * en el diseño AND-R3. Devuelve el mensaje de cada campo con error; vacío si todo está bien.
 */
fun validarPaciente(
    form: FormularioPaciente,
    hoy: Calendar = Calendar.getInstance(),
): Map<CampoPaciente, Int> =
    buildMap {
        fun revisar(
            campo: CampoPaciente,
            @StringRes error: Int?,
        ) {
            if (error != null) put(campo, error)
        }

        revisar(CampoPaciente.NOMBRES, obligatorio(form.nombres) ?: maximo(form.nombres, MAX_NOMBRE))
        revisar(CampoPaciente.APELLIDOS, obligatorio(form.apellidos) ?: maximo(form.apellidos, MAX_NOMBRE))
        revisar(
            CampoPaciente.DOCUMENTO,
            obligatorio(form.documento)
                ?: R.string.error_dui_formato.takeUnless { REGEX_DUI.matches(form.documento.trim()) },
        )
        revisar(
            CampoPaciente.NACIMIENTO,
            obligatorio(form.nacimiento) ?: errorFecha(form.nacimiento.trim(), hoy),
        )
        revisar(CampoPaciente.TELEFONO, obligatorio(form.telefono) ?: errorTelefono(form.telefono))
        revisar(CampoPaciente.DIRECCION, maximo(form.direccion, MAX_DIRECCION))
        revisar(CampoPaciente.CONTACTO_NOMBRE, maximo(form.contactoNombre, MAX_CONTACTO))
        if (form.contactoTelefono.isNotBlank()) {
            revisar(
                CampoPaciente.CONTACTO_TELEFONO,
                errorTelefono(form.contactoTelefono),
            )
        }
    }

@StringRes
private fun obligatorio(valor: String): Int? = R.string.campo_obligatorio.takeIf { valor.isBlank() }

@StringRes
private fun maximo(
    valor: String,
    limite: Int,
): Int? = R.string.error_max_caracteres.takeIf { valor.trim().length > limite }

@StringRes
private fun errorTelefono(valor: String): Int? =
    R.string.error_telefono_formato.takeUnless { REGEX_TELEFONO.matches(valor.trim()) }

@StringRes
private fun errorFecha(
    valor: String,
    hoy: Calendar,
): Int? {
    val fecha = fechaDesdeTexto(valor) ?: return R.string.error_fecha_invalida
    return R.string.error_fecha_futura.takeIf { fecha.after(hoy) }
}

/** `dd/mm/aaaa` → calendario, o null si no es una fecha real (31/02) o es anterior a 1900. */
private fun fechaDesdeTexto(valor: String): Calendar? {
    val (dia, mes, anio) = REGEX_FECHA.matchEntire(valor)?.destructured ?: return null
    if (anio.toInt() < ANIO_MINIMO) return null
    val calendario = GregorianCalendar().apply { isLenient = false }
    return runCatching {
        calendario.apply {
            clear()
            set(anio.toInt(), mes.toInt() - 1, dia.toInt())
            timeInMillis // fuerza la validación: lanza si la fecha no existe
        }
    }.getOrNull()
}

/** `dd/mm/aaaa` → `aaaa-mm-dd` para la API. Suponer que ya pasó [validarPaciente]. */
fun fechaParaApi(texto: String): String {
    val (dia, mes, anio) = REGEX_FECHA.matchEntire(texto.trim())?.destructured ?: return texto.trim()
    return "$anio-$mes-$dia"
}

/** `aaaa-mm-dd` de la API → `dd/mm/aaaa` del formulario. */
fun fechaParaFormulario(fechaApi: String?): String {
    val partes = fechaApi?.take(10)?.split("-") ?: return ""
    return if (partes.size == 3) "${partes[2]}/${partes[1]}/${partes[0]}" else ""
}

/** Años cumplidos a [hoy] desde una fecha `aaaa-mm-dd`, o null si no hay fecha. */
fun edadDesde(
    fechaApi: String?,
    hoy: Calendar = Calendar.getInstance(),
): Int? {
    val partes = fechaApi?.take(10)?.split("-")?.mapNotNull { it.toIntOrNull() } ?: return null
    if (partes.size != 3) return null
    val (anio, mes, dia) = partes
    val cumplioEsteAnio =
        hoy.get(Calendar.MONTH) + 1 > mes ||
            (hoy.get(Calendar.MONTH) + 1 == mes && hoy.get(Calendar.DAY_OF_MONTH) >= dia)
    return hoy.get(Calendar.YEAR) - anio - if (cumplioEsteAnio) 0 else 1
}

/** Inserta el guion del DUI al escribir: `012345678` → `01234567-8`. Solo deja dígitos y un guion. */
fun formatearDui(texto: String): String {
    val digitos = texto.filter { it.isDigit() }.take(9)
    return if (digitos.length > 8) "${digitos.take(8)}-${digitos.drop(8)}" else digitos
}

/** Formulario lleno con los datos actuales del paciente (modo edición). */
fun Paciente.aFormulario(): FormularioPaciente =
    FormularioPaciente(
        nombres = nombres,
        apellidos = apellidos,
        documento = documento.orEmpty(),
        nacimiento = fechaParaFormulario(fechaNacimiento),
        sexo = sexo,
        telefono = telefono.orEmpty(),
        direccion = direccion.orEmpty(),
        contactoNombre = contactoEmergencia.orEmpty(),
        contactoTelefono = telefonoEmergencia.orEmpty(),
    )

/** Datos completos para crear: los opcionales vacíos no se envían. */
fun FormularioPaciente.aDatos(): DatosPaciente =
    DatosPaciente(
        nombres = nombres.trim(),
        apellidos = apellidos.trim(),
        documento = documento.trim(),
        fechaNacimiento = fechaParaApi(nacimiento),
        sexo = sexo,
        telefono = telefono.trim(),
        direccion = direccion.trim().ifEmpty { null },
        contactoEmergencia = contactoNombre.trim().ifEmpty { null },
        telefonoEmergencia = contactoTelefono.trim().ifEmpty { null },
    )

/**
 * Solo los campos que cambiaron respecto a [original], para el PATCH. Los opcionales que se
 * vacían no se pueden borrar (la API no acepta null en el body), así que se omiten. Null si no hubo cambios.
 */
fun FormularioPaciente.cambiosRespectoA(original: FormularioPaciente): DatosPaciente? {
    val nuevos = aDatos()
    val antes = original.aDatos()

    fun <T> cambio(
        nuevo: T?,
        anterior: T?,
    ): T? = nuevo.takeIf { it != null && it != anterior }

    val cambios =
        DatosPaciente(
            nombres = cambio(nuevos.nombres, antes.nombres),
            apellidos = cambio(nuevos.apellidos, antes.apellidos),
            documento = cambio(nuevos.documento, antes.documento),
            fechaNacimiento = cambio(nuevos.fechaNacimiento, antes.fechaNacimiento),
            sexo = cambio(nuevos.sexo, antes.sexo),
            telefono = cambio(nuevos.telefono, antes.telefono),
            direccion = cambio(nuevos.direccion, antes.direccion),
            contactoEmergencia = cambio(nuevos.contactoEmergencia, antes.contactoEmergencia),
            telefonoEmergencia = cambio(nuevos.telefonoEmergencia, antes.telefonoEmergencia),
        )
    return cambios.takeIf { it != DatosPaciente() }
}
