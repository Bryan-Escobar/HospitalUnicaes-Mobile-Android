package sv.edu.unicaes.hospital.ui.theme

import androidx.compose.ui.graphics.Color

// Tokens del "Sistema de diseño · propuesta base" (HUNI-27).
// Si el equipo cambia la paleta, se reemplazan solo estos valores.

// Primario · teal clínico
val Primario50 = Color(0xFFECFEFF) // fondos
val Primario100 = Color(0xFFCFFAFE) // selección
val Primario200 = Color(0xFFA5F3FC) // borde "En vivo", cama elegida
val Primario700 = Color(0xFF0E7490) // acción (5,4:1)
val Primario800 = Color(0xFF155E75) // presionado
val Primario900 = Color(0xFF164E63) // texto sobre 50

// Marca institucional · solo Splash y logo (diseño AND-00)
val MarcaGranate = Color(0xFF5A1716) // vino-900 · fondo del splash
val MarcaVino = Color(0xFF862524) // vino-700 · fondo del logo
val MarcaDorado = Color(0xFFE8B840) // oro-400 · aro del logo, progreso, pie
val MarcaRosado = Color(0xFFF4DCDA) // texto secundario sobre vino

// Neutros · grises fríos
val Fondo = Color(0xFFF8FAFC)
val Superficie = Color(0xFFFFFFFF)
val Superficie2 = Color(0xFFF1F5F9)
val Borde = Color(0xFFCBD5E1)
val BordeSuave = Color(0xFFE2E8F0)
val BordeCampo = Color(0xFF94A3B8)
val Texto3 = Color(0xFF64748B) // (4,8:1)
val Texto2 = Color(0xFF475569)
val Texto1 = Color(0xFF0F172A)

// Error y acción destructiva
val Error = Color(0xFFB91C1C)
val ErrorFondo = Color(0xFFFEF2F2)
val ErrorBorde = Color(0xFFFCA5A5)
val ErrorTextoBanner = Color(0xFF7F1D1D)
val Neutro700 = Color(0xFF334155) // etiquetas, snackbar

// Semánticos de estado (texto / fondo). Siempre acompañados de texto e icono.
val AmbarTexto = Color(0xFF92400E)
val AmbarFondo = Color(0xFFFEF3C7)
val AmbarFondoSuave = Color(0xFFFFFBEB) // warn-50 · banners de aviso
val AmbarTextoBanner = Color(0xFF78350F) // warn-900
val AzulTexto = Color(0xFF1E40AF)
val AzulFondo = Color(0xFFDBEAFE)
val VerdeTexto = Color(0xFF166534)
val VerdeFondo = Color(0xFFDCFCE7)
val RojoTexto = Color(0xFF991B1B)
val RojoFondo = Color(0xFFFEE2E2)
val GrisTexto = Color(0xFF374151)
val GrisFondo = Color(0xFFE5E7EB)
val CamaOcupada = Color(0xFF334155)

// Bordes de los badges de estado
val AmbarBorde = Color(0xFFFCD34D)
val AzulBorde = Color(0xFF93C5FD)
val VerdeBorde = Color(0xFF86EFAC)
val RojoBorde = Color(0xFFFCA5A5)
val GrisBorde = Color(0xFFD1D5DB)
