package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Assessment
import com.example.model.DateBasis
import com.example.ui.theme.CollideType
import com.example.ui.theme.Collision
import com.example.ui.theme.Ink
import com.example.ui.theme.Ink2
import com.example.ui.theme.Ink3
import com.example.ui.theme.Paper
import com.example.ui.theme.Plate
import com.example.ui.theme.Rule

/**
 * Honest Date Basis Display (Part 7).
 *
 * Spec:
 * - stated: date in mono, plain ink.
 * - derived-from-week: date in mono with a dotted underline. Hover / click reveals source provenance (e.g., "From 'Week 9' on page 3").
 * - inferred / unknown: date slot shows a dash "—" and the row carries a "needs a date" label. It contributes zero hours to the trace until resolved.
 */
@Composable
fun DateBasisDisplay(
    assessment: Assessment,
    modifier: Modifier = Modifier,
    onNeedsDateClick: (() -> Unit)? = null
) {
    var showTooltip by remember { mutableStateOf(false) }

    val dateBasis = assessment.dateBasis
    val hasValidDueDate = !assessment.dueDate.isNullOrBlank()

    when {
        // 1. INFERRED / UNKNOWN / NO DATE: Date slot shows a dash and row carries "needs a date" label
        dateBasis == DateBasis.UNKNOWN || dateBasis == DateBasis.INFERRED || !hasValidDueDate -> {
            Row(
                modifier = modifier
                    .testTag("date_basis_needs_date_${assessment.id}")
                    .then(
                        if (onNeedsDateClick != null) Modifier.clickable { onNeedsDateClick() } else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "—",
                    style = CollideType.measured13,
                    color = Ink3,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .background(Collision.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                        .border(1.dp, Collision.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NEEDS A DATE",
                        style = CollideType.measured11,
                        fontWeight = FontWeight.Bold,
                        color = Collision
                    )
                }
            }
        }

        // 2. DERIVED FROM WEEK: Date in mono with a dotted underline. Tap/hover reveals provenance
        dateBasis == DateBasis.DERIVED_FROM_WEEK -> {
            Column(
                modifier = modifier
                    .clickable { showTooltip = !showTooltip }
                    .testTag("date_basis_derived_${assessment.id}")
                    .semantics {
                        contentDescription = "Derived date ${assessment.dueDate}. Source: From syllabus page ${assessment.source.page}"
                    }
            ) {
                Text(
                    text = assessment.dueDate ?: "—",
                    style = CollideType.measured13,
                    color = Ink,
                    modifier = Modifier.drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val y = size.height - strokeWidth
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        drawLine(
                            color = Ink2,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth,
                            pathEffect = pathEffect
                        )
                    }
                )

                if (showTooltip) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Plate, RoundedCornerShape(2.dp))
                            .border(1.dp, Rule, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "From 'Week' on p. ${assessment.source.page}",
                            style = CollideType.measured11,
                            color = Ink2
                        )
                    }
                }
            }
        }

        // 3. STATED: Date in mono, plain ink
        else -> {
            Text(
                text = assessment.dueDate ?: "—",
                style = CollideType.measured13,
                color = Ink,
                modifier = modifier
                    .testTag("date_basis_stated_${assessment.id}")
                    .semantics {
                        contentDescription = "Stated date ${assessment.dueDate}"
                    }
            )
        }
    }
}
