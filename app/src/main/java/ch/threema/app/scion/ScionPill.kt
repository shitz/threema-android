package ch.threema.app.scion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ch.threema.app.R

/** The pill that marks a conversation as reached over SCION, like the `Threema.ScionPill` style. */
@Composable
fun ScionPill(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.scion_pill_description)
    Text(
        modifier = modifier
            .semantics { contentDescription = description }
            .background(color = colorResource(R.color.scion_pill_background), shape = CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        text = stringResource(R.string.scion_pill),
        color = colorResource(R.color.scion_pill_text),
        maxLines = 1,
        style = TextStyle(
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.em,
        ),
    )
}
