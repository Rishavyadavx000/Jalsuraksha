package com.jalsuraksha.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jalsuraksha.app.data.DeviceStatus
import com.jalsuraksha.app.data.LinkState
import com.jalsuraksha.app.domain.Formats
import com.jalsuraksha.app.ui.UiSnapshot
import com.jalsuraksha.app.ui.components.JalCard
import com.jalsuraksha.app.ui.components.StatusChip
import com.jalsuraksha.app.ui.components.Tone
import com.jalsuraksha.app.ui.theme.JalSpacing

/**
 * Compact dashboard summary of the ESP32's own dual-sensor leak-monitoring state.
 * Values are shown as reported by firmware; this card does not infer a confirmed leak
 * from flow difference alone.
 */
@Composable
internal fun PipelineStatusCard(ui: UiSnapshot) {
    val status = ui.status
    val stale = ui.link != LinkState.ONLINE
    val (headline, tone, explanation) = pipelineSummary(status, stale)

    JalCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Pipeline status", style = MaterialTheme.typography.titleMedium)
            StatusChip(text = headline, tone = tone)
        }
        Spacer(Modifier.height(JalSpacing.xs))
        Text(
            explanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(JalSpacing.sm))

        if (status == null) {
            StatRow("Firmware state", "Waiting for ESP32")
            StatRow("Sensor health", "--")
            StatRow("Leak segment", "--")
            return@JalCard
        }

        StatRow("Firmware state", status.systemState?.takeIf { it.isNotBlank() } ?: "--")
        StatRow("Sensor health", status.sensorHealth?.takeIf { it.isNotBlank() } ?: "--")
        StatRow(
            "Leak segment",
            status.locationEstimate
                ?.takeIf { it.isNotBlank() && !it.equals("none", ignoreCase = true) }
                ?: "None reported",
        )
        StatRow(
            "Flow difference",
            if (status.calibrated == true) {
                "${Formats.decimal(status.differenceLpm, 2)} L/min (${Formats.decimal(status.differencePercent, 1)}%)"
            } else {
                "Available after both sensors are calibrated"
            },
        )
        StatRow(
            "Baseline capture",
            when (status.baselineActive) {
                true -> "In progress"
                false -> "Idle"
                null -> "Unknown"
            },
        )
    }
}

private data class PipelineSummary(
    val headline: String,
    val tone: Tone,
    val explanation: String,
)

private fun pipelineSummary(status: DeviceStatus?, stale: Boolean): PipelineSummary {
    if (status == null) {
        return PipelineSummary(
            headline = "No data",
            tone = Tone.NEUTRAL,
            explanation = "The dashboard has not received a valid firmware status response yet.",
        )
    }
    if (stale) {
        return PipelineSummary(
            headline = "Last known state",
            tone = Tone.WARNING,
            explanation = "The reading is not live. Reconnect to the ESP32 before relying on this pipeline status.",
        )
    }
    return when {
        status.isLeaking -> PipelineSummary(
            headline = "Leak alarm latched",
            tone = Tone.DANGER,
            explanation = "The ESP32 reports a confirmed leak latch. Follow the firmware alarm and inspect the indicated segment.",
        )
        status.isPossibleLeak -> PipelineSummary(
            headline = "Possible leak / Sensor B check",
            tone = Tone.WARNING,
            explanation = "The ESP32 reports an unconfirmed A-to-B flow mismatch. Check Sensor B and the pipeline before resetting the latch.",
        )
        !status.latch.isNullOrBlank() && !status.latch.equals("none", ignoreCase = true) -> PipelineSummary(
            headline = "Firmware alarm latched",
            tone = Tone.DANGER,
            explanation = "The ESP32 reports a latched sensor or system condition. Read the firmware event details before operating the pump.",
        )
        status.calibrated != true -> PipelineSummary(
            headline = "Calibration required",
            tone = Tone.WARNING,
            explanation = "Both flow sensors are not yet calibrated. Leak comparison is unavailable; pulse rates are shown instead of flow values.",
        )
        else -> PipelineSummary(
            headline = "No alarm reported",
            tone = Tone.SUCCESS,
            explanation = "The latest ESP32 status contains no active leak or sensor-fault latch. This is monitoring information, not a hardware safety certification.",
        )
    }
}
