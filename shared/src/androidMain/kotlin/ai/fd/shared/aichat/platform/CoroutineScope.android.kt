package ai.fd.shared.aichat.platform

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

actual fun ioCoroutineContext(): CoroutineContext = (Dispatchers.IO)
