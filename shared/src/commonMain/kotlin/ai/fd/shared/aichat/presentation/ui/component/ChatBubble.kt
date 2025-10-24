package ai.fd.shared.aichat.presentation.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun ChatBubble(
    message: String?,
    imageBitmap: ImageBitmap?,
    isRight: Boolean,
    widthMax: Dp,
    modifier: Modifier,
) {
    if (message == null && imageBitmap == null) return

    val bubbleColor =
        if (isRight) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.surfaceVariant
    val textColor =
        if (isRight) MaterialTheme.colorScheme.onSecondary
        else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier,
        horizontalArrangement = if (isRight) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = widthMax),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // Text field
                if (message != null) {
                    Markdown(
                        content = message,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        colors = markdownColor(text = textColor),
                        typography = markdownTypography(),
                    )
                }
                // image field
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(widthMax),
                    )
                }
            }
        }
    }
}
