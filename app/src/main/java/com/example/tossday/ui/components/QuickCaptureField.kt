package com.example.tossday.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import com.example.tossday.data.repository.NoteBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun QuickCaptureField(
    text: String,
    onTextChange: (String) -> Unit,
    noteBackground: NoteBackground = NoteBackground.NONE,
    isHapticEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    // rememberSaveable, щоб після навігації (наприклад в Налаштування й назад) ми НЕ
    // ставили курсор знову в кінець. Раніше тут було просто remember — тому при поверненні
    // initialCursorSet=false → курсор стрибав у кінець → внутрішній скрол поля з'їжджав
    // на верх (або в інше місце), створюючи відчуття "перелистнуло всі нотатки".
    var initialCursorSet by rememberSaveable { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    // remember (НЕ saveable): після cold start / повернення з Settings ми ХОЧЕМО, щоб поле
    // знову забрало фокус і клавіатура відкрилась. Saveable раніше зберігав true і блокував
    // автофокус, через що користувач відкривав апку — а курсор/клавіатура не з'являлись.
    var hasAutoFocused by remember { mutableStateOf(false) }
    // Одноразовий стрибок viewport-а в кінець після першого підвантаження тексту з БД.
    // Saveable — щоб після Settings → Назад не "перегортувало" вже прокручений вручну текст.
    var didJumpToEnd by rememberSaveable { mutableStateOf(false) }

    // rememberSaveable з TextFieldValue.Saver — зберігає і текст, і selection (позицію
    // курсора/виділення) при навігації Settings ↔ Main. Раніше після повернення selection
    // обнулявся в (0,0), і вся секція "перегортувалась" наверх.
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text))
    }
    val scrollState = rememberScrollState()
    val interactionSource = remember { MutableInteractionSource() }

    // Кольори маркерів і підсвітки беруться з MaterialTheme; transformation створюється раз
    // на тему (а не на кожен символ) — Compose зрівняє інстанс по equals і не перевиконує
    // filter без потреби. Але тепер ми передаємо ще й selection для розгортання цитат!
    val highlightBackground = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
    val markdownTransformation = remember(highlightBackground, textFieldValue.selection) {
        MarkdownVisualTransformation(highlightBackground, textFieldValue.selection)
    }

    LaunchedEffect(text) {
        if (textFieldValue.text != text) {
            if (!initialCursorSet && text.isNotEmpty()) {
                // Перший запуск: якщо текст не закінчується на '\n' — додаємо один Enter,
                // щоб курсор одразу стояв на порожньому рядку і можна писати без натискання.
                val finalText = if (text.endsWith("\n")) text else "$text\n"
                textFieldValue = TextFieldValue(finalText, TextRange(finalText.length))
                initialCursorSet = true
                if (finalText != text) onTextChange(finalText)
            } else {
                // Зовнішня заміна тексту (напр., очищення чернетки) — обмежуємо selection
                // довжиною нового тексту, щоб поле не намагалося прокрутити в неіснуючу позицію.
                val safeSelection = textFieldValue.selection.let { sel ->
                    val end = sel.end.coerceAtMost(text.length)
                    val start = sel.start.coerceAtMost(end)
                    TextRange(start, end)
                }
                textFieldValue = textFieldValue.copy(text = text, selection = safeSelection)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAutoFocused) {
            delay(250)
            focusRequester.requestFocus()
            hasAutoFocused = true
        }
    }

    // Після того, як збережений нотатник підвантажився з БД (initialCursorSet=true),
    // курсор стоїть у TextRange(end), але зовнішній verticalScroll лишається на 0 — тому
    // користувач бачить ПОЧАТОК нотатки, а не кінець, де він збирається писати. Чекаємо
    // на завершення layout (maxValue стає > 0) і одноразово стрибаємо в самий низ.
    // withTimeoutOrNull захищає від зависання, якщо текст короткий і overflow не виникає.
    LaunchedEffect(initialCursorSet) {
        if (!initialCursorSet || didJumpToEnd) return@LaunchedEffect
        withTimeoutOrNull(500) {
            snapshotFlow { scrollState.maxValue }.first { it > 0 }
            scrollState.scrollTo(scrollState.maxValue)
        }
        didJumpToEnd = true
    }

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    // Висота рядка береться з тієї самої bodyLarge типографіки, якою рендериться текст.
    // Якщо хтось колись змінить шрифт у темі — лінії автоматично під нього підлаштуються.
    val bodyLargeStyle = MaterialTheme.typography.bodyLarge
    val density = LocalDensity.current
    val lineSpacingPx = with(density) {
        val lh = bodyLargeStyle.lineHeight
        if (lh.isUnspecified) 24.sp.toPx() else lh.toPx()
    }
    // BasicTextField + DecorationBox без label має внутрішній padding 16.dp зверху —
    // звідти починається перший рядок тексту. Перша лінія йде під першим рядком.
    val topPaddingPx = with(density) { 16.dp.toPx() }

    // Сітка/лінійка кешується в Path через drawWithCache: Path перебудовується лише
    // коли змінюється розмір Box або noteBackground, інакше малюється один кешований
    // Path за кадр (без N drawLine викликів). Це усуває джанк скролу при тривалому тексті.
    val stroke = remember { Stroke(width = 1f) }
    
    // Кеш для TextLayoutResult
    var textLayoutResultState by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    val quoteBgColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
    val quoteBorderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                val path = Path()
                when (noteBackground) {
                    NoteBackground.NONE -> Unit
                    NoteBackground.LINES -> {
                        var y = topPaddingPx + lineSpacingPx
                        while (y < size.height) {
                            path.moveTo(0f, y)
                            path.lineTo(size.width, y)
                            y += lineSpacingPx
                        }
                    }
                    NoteBackground.GRID -> {
                        var y = topPaddingPx + lineSpacingPx
                        while (y < size.height) {
                            path.moveTo(0f, y)
                            path.lineTo(size.width, y)
                            y += lineSpacingPx
                        }
                        var x = lineSpacingPx
                        while (x < size.width) {
                            path.moveTo(x, 0f)
                            path.lineTo(x, size.height)
                            x += lineSpacingPx
                        }
                    }
                }
                onDrawBehind {
                    if (noteBackground != NoteBackground.NONE) {
                        drawPath(path = path, color = gridColor, style = stroke)
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    initialCursorSet = true
                    onTextChange(newValue.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .padding(16.dp)
                    .drawBehind {
                        val layoutResult = textLayoutResultState
                        if (layoutResult != null) {
                            val quotes = layoutResult.layoutInput.text.getStringAnnotations("QUOTE", 0, layoutResult.layoutInput.text.length)
                            for (quote in quotes) {
                                val startLine = layoutResult.getLineForOffset(quote.start)
                                val endLine = layoutResult.getLineForOffset(quote.end)
                                
                                val top = layoutResult.getLineTop(startLine)
                                val bottom = layoutResult.getLineBottom(endLine)
                                
                                val leftMargin = -2.dp.toPx()
                                val rightMargin = size.width + 12.dp.toPx()
                                
                                // Фон цитати
                                drawRoundRect(
                                    color = quoteBgColor,
                                    topLeft = Offset(leftMargin, top),
                                    size = Size(rightMargin - leftMargin, bottom - top),
                                    cornerRadius = CornerRadius(6.dp.toPx())
                                )
                                // Ліва лінія (бордер)
                                drawRoundRect(
                                    color = quoteBorderColor,
                                    topLeft = Offset(leftMargin, top),
                                    size = Size(3.dp.toPx(), bottom - top),
                                    cornerRadius = CornerRadius(2.dp.toPx())
                                )
                            }
                        }
                    }
                    .focusRequester(focusRequester),
                textStyle = bodyLargeStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                visualTransformation = markdownTransformation,
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Default,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                onTextLayout = { textLayoutResultState = it },
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = "Що плануєш зробити?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        // iOS-стиль скролбар: зʼявляється під час прокрутки, плавно зникає через ~900мс.
        // Якщо overflow немає (текст вміщується) — взагалі нічого не малює.
        ScrollIndicator(
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp, top = 8.dp, bottom = 8.dp)
        )

        // Toolbar форматування: B / I / Marker. Зʼявляється slide-in зверху при будь-якому
        // не-згорнутому виділенні; зникає при collapse або зміні тексту. Розташований по
        // центру зверху, щоб не конфліктувати з системним меню Cut/Copy/Paste, що
        // зазвичай зʼявляється над виділенням.
        val showFormatToolbar = !textFieldValue.selection.collapsed && textFieldValue.text.isNotEmpty()
        AnimatedVisibility(
            visible = showFormatToolbar,
            enter = fadeIn(tween(150)) + slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(180)
            ),
            exit = fadeOut(tween(120)) + slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(150)
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
        ) {
            FormatToolbar(
                onApplyMarker = { action ->
                    val updated = if (action == "quote") {
                        toggleQuote(textFieldValue)
                    } else {
                        wrapSelectionInMarker(textFieldValue, action)
                    }
                    if (updated !== textFieldValue) {
                        textFieldValue = updated
                        onTextChange(updated.text)
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = text.isEmpty(),
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    text = "Enter — нове завдання. Рядки з '//' ігноруються",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        val hasExtraNewlines = text.contains("\n\n\n") || text.trim().isEmpty() && text.length > 2

        // Логіка: ховаємо кнопку як тільки відкривається діалог (запобігає стрибкам)
        val isButtonVisible = hasExtraNewlines && !showCleanupDialog

        AnimatedVisibility(
            visible = isButtonVisible,
            enter = fadeIn(tween(250)) +
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { 40 }
                    ) +
                    scaleIn(initialScale = 0.8f, animationSpec = tween(200)),
            exit = fadeOut(tween(150)) +
                    slideOutVertically(
                        targetOffsetY = { 20 },
                        animationSpec = tween(150)
                    ) +
                    scaleOut(targetScale = 0.8f, animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clickable { showCleanupDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = "Оптимізувати текст",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (showCleanupDialog) {
            AlertDialog(
                onDismissRequest = { showCleanupDialog = false },
                title = { Text("Прибрати зайві відступи?") },
                text = { Text("Видалити великі проміжки, залишивши максимум по одному порожньому рядку між абзацами тексту?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val cleaned = "\n" + text.replace(Regex("\\n{3,}"), "\n\n").trim()
                            // Курсор у самому кінці — після останнього завдання, не на верх.
                            textFieldValue = TextFieldValue(cleaned, TextRange(cleaned.length))
                            onTextChange(cleaned)
                            showCleanupDialog = false
                        }
                    ) {
                        Text("Очистити")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCleanupDialog = false }) {
                        Text("Скасувати")
                    }
                }
            )
        }
    }
}

/**
 * Маленький pill-toolbar з трьома кнопками форматування (B / I / Marker), який обгортає
 * виділений діапазон у markdown-маркери. Toggle: повторний тап знімає обгортку.
 */
@Composable
private fun FormatToolbar(
    onApplyMarker: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(
            icon = Icons.Default.FormatBold,
            description = "Жирний"
        ) { onApplyMarker("**") }
        ToolbarButton(
            icon = Icons.Default.FormatItalic,
            description = "Курсив"
        ) { onApplyMarker("*") }
        ToolbarButton(
            icon = Icons.Default.FormatQuote,
            description = "Цитата"
        ) { onApplyMarker("quote") }
        ToolbarButton(
            icon = Icons.Default.BorderColor,
            description = "Маркер"
        ) { onApplyMarker("==") }
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "buttonScale"
    )

    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Додає або видаляє цитування (`> `) для всіх рядків, які перетинаються з виділенням.
 */
fun toggleQuote(tfv: TextFieldValue): TextFieldValue {
    val text = tfv.text
    val sel = tfv.selection
    
    var startLine = sel.min
    while (startLine > 0 && text[startLine - 1] != '\n') {
        startLine--
    }
    var endLine = sel.max
    while (endLine < text.length && text[endLine] != '\n') {
        endLine++
    }
    
    val lines = text.substring(startLine, endLine).split('\n')
    val hasQuotes = lines.any { it.startsWith("> ") || it.startsWith(">") }
    val allQuoted = hasQuotes && lines.all { it.startsWith(">") || it.isEmpty() }
    
    val newLines = if (allQuoted) {
        lines.map { 
            if (it.startsWith("> ")) it.substring(2) 
            else if (it.startsWith(">")) it.substring(1) 
            else it 
        }
    } else {
        lines.map { if (it.isEmpty()) "> " else "> $it" }
    }
    
    val newSelectedText = newLines.joinToString("\n")
    val newText = text.substring(0, startLine) + newSelectedText + text.substring(endLine)
    
    return tfv.copy(
        text = newText,
        selection = TextRange(startLine, startLine + newSelectedText.length)
    )
}

/**
 * Тонкий вертикальний індикатор прокрутки в стилі iOS:
 *  • не малюється поки немає overflow (maxValue == 0);
 *  • з'являється під час будь-якої зміни scroll value;
 *  • плавно зникає через ~900мс простою.
 */
@Composable
private fun ScrollIndicator(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.value) {
        if (scrollState.maxValue > 0) {
            visible = true
            delay(900)
            visible = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.45f else 0f,
        animationSpec = tween(if (visible) 120 else 350),
        label = "scrollbarAlpha"
    )

    if (scrollState.maxValue == 0 || alpha <= 0.01f) return

    val thumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(3.dp)
    ) {
        val totalH = size.height
        val maxScroll = scrollState.maxValue.toFloat().coerceAtLeast(1f)
        val visibleRatio = (totalH / (totalH + maxScroll)).coerceIn(0.12f, 1f)
        val thumbH = totalH * visibleRatio
        val travel = totalH - thumbH
        val progress = (scrollState.value.toFloat() / maxScroll).coerceIn(0f, 1f)
        val thumbY = travel * progress
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(0f, thumbY),
            size = Size(size.width, thumbH),
            cornerRadius = CornerRadius(size.width / 2, size.width / 2)
        )
    }
}
