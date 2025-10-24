package ai.fd.shared.aichat.domain.service.impl

import ai.fd.shared.aichat.domain.service.TimeService
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Inject
@ContributesBinding(AppScope::class)
@OptIn(ExperimentalTime::class)
class TimeServiceImpl(private val clock: Clock) : TimeService {
    override fun epochSeconds(): Long {
        return clock.now().epochSeconds
    }
}
