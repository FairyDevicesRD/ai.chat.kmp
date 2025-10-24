package ai.fd.shared.aichat.presentation.ui

import ai.fd.shared.aichat.presentation.ui.component.CameraButton
import ai.fd.shared.aichat.presentation.ui.component.ChatBubble
import ai.fd.shared.aichat.presentation.ui.component.RecordButton
import ai.fd.shared.aichat.presentation.viewmodel.AppViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(modifier: Modifier, viewModel: AppViewModel, onNavigateToCamera: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BottomAppBar(
                modifier =
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp)),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentPadding = PaddingValues(0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    CameraButton(
                        modifier = Modifier.weight(0.5f),
                        contentPadding = PaddingValues(16.dp),
                        shape = RectangleShape,
                        featureState = uiState.buttonState,
                        onClick = onNavigateToCamera,
                    )
                    RecordButton(
                        modifier = Modifier.weight(0.5f),
                        contentPadding = PaddingValues(16.dp),
                        shape = RectangleShape,
                        featureState = uiState.buttonState,
                        onClick = viewModel::onRecordButtonClick,
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
            ) {
                // ASRのテキスト
                ChatBubble(
                    message = uiState.asrRecognizedText,
                    imageBitmap = uiState.imageBmp,
                    isRight = true,
                    widthMax = 300.dp,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )

                // AIの回答テキスト
                ChatBubble(
                    message = uiState.aiAnswerText,
                    imageBitmap = null,
                    isRight = false,
                    widthMax = 300.dp,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
            }

            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.isThinking) {
                    CircularProgressIndicator(modifier = Modifier.size(72.dp))
                }
            }
        }
    }
}
