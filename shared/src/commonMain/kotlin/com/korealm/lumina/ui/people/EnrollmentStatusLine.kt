package com.korealm.lumina.ui.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.korealm.lumina.protocol.EnrollPhase
import lumina.shared.generated.resources.Res
import lumina.shared.generated.resources.enroll_phase_capturing
import lumina.shared.generated.resources.enroll_phase_starting
import lumina.shared.generated.resources.enroll_phase_stopping
import lumina.shared.generated.resources.enroll_progress
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Recording
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A compact enrollment status line for the app shell.
 *
 * Responsibility: keep a blind/low-vision user informed while an enrollment runs **even when they
 * are not on the Personas tab** (FE-INV-010, CHG-FE-0021). The shell shows this only when the
 * Personas tab is not composed; that screen keeps the full card with the cancel action, so this line
 * never duplicates it on-screen. The leading recording icon is decorative (FE-INV-026).
 *
 * The **phase label** is a polite `liveRegion`, so a phase change is announced without the user
 * focusing it. The per-frame counter is drawn but not announced: a `liveRegion` must sit on the node
 * whose content changes (the phase text), not on the card, or a merging container would re-announce
 * on every captured-frame update (FE-INV-010, CHG-FE-0024).
 *
 * @param state the shared people state (the enrollment fields are what this line renders).
 * @param modifier layout modifier from the shell.
 */
@Composable
internal fun EnrollmentStatusLine(state: PeopleUiState, modifier: Modifier = Modifier) {
    if (!state.enrolling) return
    val colors = MiuixTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = colors.secondaryContainer,
            contentColor = colors.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MiuixIcons.Recording,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = colors.onSecondaryContainer,
                )
                Text(
                    text = phaseLabel(state.phase),
                    style = MiuixTheme.textStyles.title4,
                    color = colors.onSecondaryContainer,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            val captured = state.captured
            val total = state.total
            if (captured != null && total != null) {
                Text(
                    text = stringResource(Res.string.enroll_progress, captured, total),
                    style = MiuixTheme.textStyles.body1,
                    color = colors.onSecondaryContainer,
                )
            }
        }
    }
}

/** Maps the enrollment phase to a Spanish label (shared by the shell line and the Personas card). */
@Composable
internal fun phaseLabel(phase: EnrollPhase?): String = when (phase) {
    EnrollPhase.StoppingRuntime -> stringResource(Res.string.enroll_phase_stopping)
    EnrollPhase.StartingRuntime -> stringResource(Res.string.enroll_phase_starting)
    EnrollPhase.Capturing, EnrollPhase.Unknown, null ->
        stringResource(Res.string.enroll_phase_capturing)
}

/**
 * The determinate progress in `0f..1f`, or `null` (indeterminate) when the frame counts are not
 * usable yet. Pure so the mapping is obvious and side-effect free.
 */
internal fun progressFraction(captured: Int?, total: Int?): Float? {
    if (captured == null || total == null || total <= 0) return null
    return (captured.toFloat() / total).coerceIn(0f, 1f)
}
