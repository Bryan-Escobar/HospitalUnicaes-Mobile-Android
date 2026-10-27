package sv.edu.unicaes.hospital.ui.auth

import sv.edu.unicaes.hospital.BuildConfig

/** Dominios que se ofrecen al escribir el correo. En dev, primero el de las cuentas demo del seed. */
val DOMINIOS_CORREO: List<String> =
    buildList {
        if (BuildConfig.FLAVOR == "dev") add("demo.hospitalunicaes.test")
        addAll(listOf("gmail.com", "hotmail.com", "outlook.com", "yahoo.com"))
    }

/**
 * Completa el dominio de lo que se lleva escrito: `ana` → `ana@gmail.com`…, `ana@ho` → `ana@hotmail.com`.
 * No sugiere nada si el correo ya está completo con uno de los dominios.
 */
fun sugerenciasCorreo(
    texto: String,
    dominios: List<String> = DOMINIOS_CORREO,
    maximo: Int = 3,
): List<String> {
    val partes = texto.trim().split("@")
    val usuario = partes.first()
    if (usuario.isEmpty() || partes.size > 2) return emptyList()
    val parcial = partes.getOrElse(1) { "" }
    return dominios
        .filter { it.startsWith(parcial, ignoreCase = true) && !it.equals(parcial, ignoreCase = true) }
        .take(maximo)
        .map { "$usuario@$it" }
}
