package ai.fd.shared.aichat.presentation.ui.component

import ai.fd.shared.aichat.presentation.viewmodel.ButtonState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.microphone.RECORD_AUDIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun RecordButton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    shape: Shape,
    featureState: ButtonState,
    onClick: () -> Unit,
) {
    val scope: CoroutineScope = rememberCoroutineScope()
    val factory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val permissionCtrl: PermissionsController =
        remember(factory) { factory.createPermissionsController() }
    BindEffect(permissionCtrl)

    val buttonColor =
        when (featureState) {
            ButtonState.READY -> MaterialTheme.colorScheme.primary
            ButtonState.MIC_USING -> MaterialTheme.colorScheme.error
            ButtonState.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
        }
    val buttonContentColor =
        when (featureState) {
            ButtonState.READY -> MaterialTheme.colorScheme.onPrimary
            ButtonState.MIC_USING -> MaterialTheme.colorScheme.onError
            ButtonState.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    val icon =
        when (featureState) {
            ButtonState.MIC_USING -> Icons.Filled.Mic
            else -> Icons.Filled.MicOff
        }

    Button(
        onClick = {
            scope.launch {
                runCatching { permissionCtrl.providePermission(Permission.RECORD_AUDIO) }
                    .onSuccess { onClick() }
            }
        },
        modifier = modifier,
        contentPadding = contentPadding,
        enabled = (featureState != ButtonState.DISABLED),
        shape = shape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = buttonContentColor,
            ),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}
