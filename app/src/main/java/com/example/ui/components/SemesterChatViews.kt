package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.MessageSender
import com.example.data.SemesterEntity
import com.example.model.Assessment
import com.example.model.Course
import com.example.model.SemesterLoadResult
import com.example.ui.theme.CollideType
import com.example.ui.theme.Ink
import com.example.ui.theme.Ink2
import com.example.ui.theme.Ink3
import com.example.ui.theme.MartianMonoFamily
import com.example.ui.theme.NewsreaderFamily
import com.example.ui.theme.Paper
import com.example.ui.theme.Plate
import com.example.ui.theme.Rule

@Composable
fun SemesterChatPanel(
    semester: SemesterEntity?,
    courses: List<Course>,
    assessments: List<Assessment>,
    loadResult: SemesterLoadResult?,
    messages: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val sampleCourseCode = courses.firstOrNull()?.code ?: "POL 201"
    val sampleAssessment = assessments.firstOrNull { it.dueDate != null }?.title ?: "Essay 1"
    val peakWeek = loadResult?.weeks?.maxByOrNull { it.totalHours }?.weekNumber ?: 10

    val suggestedChips = remember(courses, assessments, peakWeek) {
        listOf(
            "What should I start today?",
            "Is week $peakWeek as bad as it looks?",
            "Can I hand in the $sampleCourseCode assignment late?",
            "What if I ask for a 3-day extension on $sampleAssessment?"
        )
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .drawBehind {
                // Hairline left border
                drawLine(
                    color = Rule,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        color = Paper
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TALK TO THE SEMESTER",
                        style = CollideType.measured11,
                        color = Ink2,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Grounded on semester digest",
                        style = CollideType.measured11,
                        color = Ink3
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = onClearChat,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear chat history",
                                tint = Ink3,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("close_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close chat panel",
                            tint = Ink
                        )
                    }
                }
            }

            // Divider rule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Rule)
            )

            // Conversation transcript
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ask about triage, what-if scenarios, policy citations, or workload framing.",
                        style = CollideType.prose15,
                        color = Ink2,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "SUGGESTED INQUIRIES",
                        style = CollideType.measured11,
                        color = Ink3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedChips.forEach { prompt ->
                            SuggestionChip(
                                text = prompt,
                                onClick = { onSendMessage(prompt) }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageItem(message = msg)
                    }

                    if (isLoading) {
                        item {
                            ThinkingIndicator()
                        }
                    }
                }
            }

            // Quick suggestion chips bar above input if there are messages
            if (messages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestedChips.take(3).forEach { prompt ->
                        MiniPromptChip(
                            text = prompt,
                            onClick = { onSendMessage(prompt) }
                        )
                    }
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Rule, RoundedCornerShape(2.dp))
                    .background(Plate)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    textStyle = CollideType.interface15.copy(color = Ink),
                    cursorBrush = SolidColor(Ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank() && !isLoading) {
                            onSendMessage(inputText.trim())
                            inputText = ""
                        }
                    }),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Ask about triage, what-ifs, or syllabus policies...",
                                style = CollideType.interface13,
                                color = Ink3
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (inputText.isNotBlank() && !isLoading) Ink else Ink3.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        .clickable(enabled = inputText.isNotBlank() && !isLoading) {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText.trim())
                                inputText = ""
                            }
                        }
                        .testTag("chat_send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = Paper,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender label
        Text(
            text = if (isUser) "YOU" else "SEMESTER",
            style = CollideType.measured11,
            color = Ink3,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (isUser) {
            Box(
                modifier = Modifier
                    .background(Plate, RoundedCornerShape(2.dp))
                    .border(1.dp, Rule, RoundedCornerShape(2.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = CollideType.interface15,
                    color = Ink
                )
            }
        } else {
            // Assistant response: Render in Newsreader prose, monospace numbers/codes, left hairline rule for quotes
            FormattedNewsreaderResponse(text = message.text)
        }
    }
}

/**
 * Renders prose in Newsreader font, numbers/dates/course codes in Martian Mono,
 * and quotes in a left-hairline ruled container.
 */
@Composable
fun FormattedNewsreaderResponse(
    text: String,
    modifier: Modifier = Modifier
) {
    val lines = text.lines()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Paper)
            .border(1.dp, Rule, RoundedCornerShape(2.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith(">") || (trimmed.startsWith("\"") && trimmed.contains("(page", ignoreCase = true))) {
                // Quoted syllabus text with left hairline rule
                val quoteClean = trimmed.removePrefix(">").trim()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = Ink,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        .padding(start = 12.dp, top = 2.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = buildMonoAnnotatedString(quoteClean),
                        style = CollideType.prose15.copy(
                            color = Ink,
                            lineHeight = (15 * 1.55).sp
                        )
                    )
                }
            } else if (trimmed.isNotBlank()) {
                Text(
                    text = buildMonoAnnotatedString(trimmed),
                    style = CollideType.prose15.copy(
                        color = Ink,
                        lineHeight = (15 * 1.55).sp
                    )
                )
            }
        }
    }
}

/**
 * Annotates numbers, percentages, dates (YYYY-MM-DD or Month DD), week numbers,
 * and standard course codes (e.g. POL 201, CS 201) with monospace font styling.
 */
fun buildMonoAnnotatedString(input: String): AnnotatedString {
    return buildAnnotatedString {
        append(input)

        // Regex patterns for measured values:
        // 1. Course codes like "POL 201", "CS 106A", "CHEM 101"
        // 2. Dates like "2026-11-14", "2026-09-07"
        // 3. Numbers and units like "34 hours", "25h", "20%", "3-day", "week 10", "week 7", "page 4"
        val pattern = Regex(
            """(\b[A-Z]{2,5}\s+\d{3}[A-Z]?\b|\b\d{4}-\d{2}-\d{2}\b|\bweek\s+\d+\b|\b\d+(?:\.\d+)?(?:h|%|hrs| hours| days| day|-day)?\b|\bpage\s+\d+\b)""",
            RegexOption.IGNORE_CASE
        )

        pattern.findAll(input).forEach { matchResult ->
            val range = matchResult.range
            addStyle(
                style = SpanStyle(
                    fontFamily = MartianMonoFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink
                ),
                start = range.first,
                end = range.last + 1
            )
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    Row(
        modifier = Modifier
            .background(Plate, RoundedCornerShape(2.dp))
            .border(1.dp, Rule, RoundedCornerShape(2.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 1.5.dp,
            color = Ink2
        )
        Text(
            text = "Consulting semester digest...",
            style = CollideType.measured11,
            color = Ink2
        )
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Rule, RoundedCornerShape(2.dp))
            .background(Plate)
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Text(
            text = text,
            style = CollideType.interface13,
            color = Ink
        )
    }
}

@Composable
private fun MiniPromptChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .border(1.dp, Rule, RoundedCornerShape(2.dp))
            .background(Plate)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = CollideType.measured11,
            color = Ink2,
            maxLines = 1
        )
    }
}
