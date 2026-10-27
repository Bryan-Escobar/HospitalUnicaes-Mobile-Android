package sv.edu.unicaes.hospital.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sv.edu.unicaes.hospital.R

/** Tamaños del lockup de marca: Splash (logo 96, título 28/34) y Login (logo 72, título 26/32). */
enum class TamanoMarca(
    val logo: Dp,
    val separacion: Dp,
    val titulo: TextStyle,
    val subtitulo: TextStyle,
) {
    SPLASH(
        logo = 96.dp,
        separacion = 18.dp,
        titulo = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        subtitulo = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    ),
    LOGIN(
        logo = 72.dp,
        separacion = 12.dp,
        titulo = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
        subtitulo = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
    ),
}

/** Logo + "HospitalUnicaes" + "Hospital Universitario Central" (diseño AND-00 y AND-01). */
@Composable
fun MarcaHospital(
    colorTitulo: Color,
    colorSubtitulo: Color,
    tamano: TamanoMarca,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tamano.separacion),
    ) {
        // Decorativo: el nombre del hospital va justo debajo como texto.
        Image(
            painter = painterResource(R.drawable.ic_logo_hospital),
            contentDescription = null,
            modifier = Modifier.size(tamano.logo),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(R.string.marca_nombre), style = tamano.titulo, color = colorTitulo)
            Text(text = stringResource(R.string.marca_hospital), style = tamano.subtitulo, color = colorSubtitulo)
        }
    }
}
