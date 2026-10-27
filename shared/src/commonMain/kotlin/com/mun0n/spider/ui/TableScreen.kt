package com.mun0n.spider.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.mun0n.spider.game.Card
import com.mun0n.spider.game.Difficulty
import com.mun0n.spider.game.Records
import com.mun0n.spider.game.GameState
import com.mun0n.spider.game.Hint
import com.mun0n.spider.game.Rank
import com.mun0n.spider.game.Suit
import com.mun0n.spider.game.SpiderRules
import com.mun0n.spider.resources.Res
import com.mun0n.spider.resources.choose_game
import com.mun0n.spider.resources.hint
import com.mun0n.spider.resources.ic_menu_hint
import com.mun0n.spider.resources.ic_menu_new
import com.mun0n.spider.resources.ic_menu_undo
import com.mun0n.spider.resources.keep_playing
import com.mun0n.spider.resources.new_game
import com.mun0n.spider.resources.new_game_short
import com.mun0n.spider.resources.new_record
import com.mun0n.spider.resources.points
import com.mun0n.spider.resources.record
import com.mun0n.spider.resources.suits_done
import com.mun0n.spider.resources.undo
import com.mun0n.spider.resources.you_won
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val FeltBrush = Brush.radialGradient(
    colors = listOf(Color(0xFF1B7D45), Color(0xFF0E6234), Color(0xFF084426)),
    radius = 1600f,
)
private val MenuBarColor = Color(0xF01B5C34)
private val Cream = Color(0xFFF6F0E4)
private val Ink = Color(0xFF1B2418)

private enum class Phase { Deal, Slide, Complete }

private data class Play(val phase: Phase, val run: Int, val progress: Float)

private data class Selection(val col: Int, val index: Int)

private data class Shake(val col: Int, val index: Int, val token: Int)

private data class Drag(
    val col: Int,
    val index: Int,
    val cards: List<Card>,
    val grab: Offset,
    val pos: Offset,
)

private data class Snap(
    val id: Int,
    val cards: List<Card>,
    val from: Offset,
    val to: Offset,
    val progress: Float,
    val fan: Float,
)

private data class Demo(
    val fromCol: Int,
    val fromIndex: Int,
    val toCol: Int,
    val cards: List<Card>,
    val spots: List<Pair<Offset, Offset>>,
    val progress: Float,
)

private sealed interface Hit {
    data class CardHit(val col: Int, val index: Int) : Hit
    data class ColumnHit(val col: Int) : Hit
    data object Stock : Hit
}

private data class TableGeom(
    val cardW: Float,
    val cardH: Float,
    val colX: List<Float>,
    val colTop: Float,
    val colMaxH: Float,
    val stockLeft: Float,
    val stockTop: Float,
    val miniW: Float,
    val miniH: Float,
    val foundationTop: Float,
    val fanStep: Float,
    val foundationLeft: Float,
) {
    fun pileLeft(index: Int, count: Int): Float = foundationLeft

    fun pileTop(index: Int): Float = foundationTop - fanStep * index
}

@Composable
fun TableScreen(vm: GameViewModel) {
    val state by vm.ui.collectAsStateWithLifecycle()
    SystemBackHandler(
        enabled = state.dialog is Dialog.NewGame && state.game != null && state.game?.won != true,
    ) {
        vm.onDismissDialog()
    }
    Box(Modifier.fillMaxSize().background(FeltBrush)) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(tableInsets())) {
            val game = state.game
            if (game != null) Board(game, state, vm)
        }
        state.banner?.let { message ->
            Text(
                stringResource(message),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp, start = 20.dp, end = 20.dp)
                    .background(Color(0xF01A1208), RoundedCornerShape(22.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
        when (val dialog = state.dialog) {
            Dialog.NewGame -> NewGameDialog(
                last = state.lastDifficulty,
                records = state.records,
                canDismiss = state.game != null && state.game?.won != true,
                onPick = vm::onNewGame,
                onDismiss = vm::onDismissDialog,
            )
            is Dialog.Won -> WonDialog(
                score = dialog.score,
                best = dialog.best,
                isRecord = dialog.isRecord,
                onNewGame = vm::onRequestNewGame,
            )
            null -> Unit
        }
    }
}

@Composable
private fun BoxScope.TableChrome(
    state: UiState,
    vm: GameViewModel,
    geom: TableGeom,
    finished: Int,
) {
    Row(
        Modifier
            .align(Alignment.TopStart)
            .padding(start = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.game?.score?.toString() ?: "—",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = "puntos" },
        )
        Text(stringResource(Res.string.points), color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
        state.game?.let { game ->
            Text(
                difficultyLabel(game.difficulty),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
            )
        }
    }
    val density = LocalDensity.current
    Text(
        text = stringResource(Res.string.suits_done, finished),
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(
                start = with(density) { (geom.foundationLeft + geom.cardW + 16f).toDp() },
                bottom = 28.dp,
            )
            .semantics { contentDescription = "palos $finished de 8" },
    )
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MenuBarColor)
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuAction(
            icon = Res.drawable.ic_menu_new,
            label = stringResource(Res.string.new_game_short),
            enabled = !state.inputLocked && !state.loading,
            onClick = vm::onRequestNewGame,
        )
        MenuAction(
            icon = Res.drawable.ic_menu_hint,
            label = stringResource(Res.string.hint),
            enabled = state.game != null && !state.inputLocked,
            onClick = vm::onHint,
        )
        MenuAction(
            icon = Res.drawable.ic_menu_undo,
            label = stringResource(Res.string.undo),
            enabled = state.canUndo && !state.inputLocked,
            onClick = vm::onUndo,
        )
    }
}

@Composable
private fun MenuAction(
    icon: DrawableResource,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = Color.White.copy(alpha = if (enabled) 1f else 0.38f)
    Column(
        Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            color = tint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun Board(game: GameState, state: UiState, vm: GameViewModel) {
    val visual = state.visual
    var play by remember(visual?.id) { mutableStateOf<Play?>(null) }
    LaunchedEffect(visual?.id) {
        val current = visual ?: return@LaunchedEffect
        val anim = Animatable(0f)
        suspend fun go(phase: Phase, run: Int, duration: Int) {
            anim.snapTo(0f)
            play = Play(phase, run, 0f)
            anim.animateTo(1f, tween(duration, easing = FastOutSlowInEasing)) {
                play = Play(phase, run, value)
            }
        }
        if (current.dealtIds.isNotEmpty()) go(Phase.Deal, 0, 720)
        if (current.slide != null) go(Phase.Slide, 0, 230)
        current.removed.indices.forEach { index -> go(Phase.Complete, index, 460) }
        vm.onVisualFinished(current.id)
    }
    val shownPlay = play ?: visual?.let { openingPlay(it) }
    val columns = when (shownPlay?.phase) {
        Phase.Deal, Phase.Slide -> visual?.before ?: game.columns
        Phase.Complete -> visual?.mid ?: game.columns
        null -> game.columns
    }
    var drag by remember { mutableStateOf<Drag?>(null) }
    var snap by remember { mutableStateOf<Snap?>(null) }
    var snapHome by remember { mutableStateOf<Snap?>(null) }
    var snapSerial by remember { mutableIntStateOf(0) }
    var selection by remember { mutableStateOf<Selection?>(null) }
    var shake by remember { mutableStateOf<Shake?>(null) }
    var shakeSerial by remember { mutableIntStateOf(0) }
    val shakeAnim = remember { Animatable(0f) }
    val rejectFeedback = rememberRejectFeedback()
    LaunchedEffect(shake?.token) {
        val current = shake ?: return@LaunchedEffect
        rejectFeedback()
        shakeAnim.snapTo(0f)
        shakeAnim.animateTo(1f, tween(420, easing = LinearEasing))
        if (shake?.token == current.token) shake = null
    }
    var demo by remember { mutableStateOf<Demo?>(null) }
    var demoToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(snapHome) {
        val home = snapHome ?: return@LaunchedEffect
        val anim = Animatable(0f)
        anim.animateTo(1f, tween(160)) {
            snap = home.copy(progress = value)
        }
        snap = null
        snapHome = null
    }
    LaunchedEffect(state.inputLocked) {
        if (state.inputLocked) {
            drag = null
            selection = null
            demo = null
        }
    }
    val hintPulse = rememberInfiniteTransition(label = "hint")
    val hintAlpha by hintPulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "hintAlpha",
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val geom = remember(constraints.maxWidth, constraints.maxHeight, density.density) {
            // En pantallas de 3x (iPhone) 60 px no dejan sitio a la puntuación.
            val header = maxOf(60f, with(density) { 40.dp.toPx() })
            measureTable(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), header)
        }
        val cardW = with(density) { geom.cardW.toDp() }
        val cardH = with(density) { geom.cardH.toDp() }
        LaunchedEffect(state.hintSerial) {
            val plays = state.hintPlays
            if (plays.isEmpty()) {
                demo = null
                return@LaunchedEffect
            }
            selection = null
            val token = demoToken
            try {
                for (play in plays) {
                    if (demoToken != token) return@LaunchedEffect
                    val ride = demoRide(columns, play, geom) ?: continue
                    val anim = Animatable(0f)
                    suspend fun glide(target: Float, ms: Int) {
                        anim.animateTo(target, tween(ms, easing = FastOutSlowInEasing)) {
                            if (demoToken == token) demo = ride.copy(progress = value)
                        }
                    }
                    glide(1f, 380)
                    if (demoToken != token) return@LaunchedEffect
                    delay(340)
                    glide(0f, 280)
                    if (demoToken != token) return@LaunchedEffect
                    delay(110)
                }
            } finally {
                if (demoToken == token) demo = null
            }
        }
        val hidden = hiddenIds(visual, shownPlay, drag, snap)
        val riding = demo?.cards?.map { it.id }?.toSet().orEmpty()
        val guide = demo?.let { Hint.MoveCards(it.fromCol, it.fromIndex, it.toCol) } ?: state.hint
        val active = drag?.let { it.col to it.index } ?: selection?.let { it.col to it.index }
        val hover = drag?.let { columnAt(geom, it.pos.x, it.pos.y) }
        val buzzing = shake
        val shakeX = if (buzzing == null) {
            0f
        } else {
            val t = shakeAnim.value
            sin(t * PI * 6).toFloat() * (1f - t) * 16f
        }

        columns.forEachIndexed { col, cards ->
            val valid = active != null && SpiderRules.canMove(game, active.first, active.second, col)
            val hintedCol = (guide as? Hint.MoveCards)?.toCol == col
            val hot = valid && (hover == null || hover == col) || hintedCol
            if (cards.isEmpty() || hot) {
                val slotH = if (hot && cards.isNotEmpty()) geom.colMaxH else geom.cardH
                Box(
                    Modifier
                        .offset { IntOffset(geom.colX[col].roundToInt(), geom.colTop.roundToInt()) }
                        .size(cardW, with(density) { slotH.toDp() })
                        .clip(RoundedCornerShape(CardCorner))
                        .background(Color.White.copy(alpha = if (hot) 0.16f else 0.08f))
                        .border(
                            width = if (hot) 2.dp else 1.dp,
                            color = if (hintedCol) HintGold.copy(alpha = hintAlpha) else Color.White.copy(alpha = if (hot) 0.7f else 0.28f),
                            shape = RoundedCornerShape(CardCorner),
                        ),
                )
            }
            val tops = ColumnLayout.tops(cards, geom.cardH, geom.colMaxH)
            cards.forEachIndexed { index, card ->
                if (card.id in hidden || card.id in riding) return@forEachIndexed
                val hinted = hintMatches(guide, col, index)
                val selected = selection?.let { it.col == col && index >= it.index } == true
                val dx = if (buzzing != null && buzzing.col == col && index >= buzzing.index) shakeX else 0f
                PlayingCard(
                    card = card,
                    selected = selected,
                    hinted = hinted,
                    locked = card.faceUp && !SpiderRules.isMovableRun(cards, index),
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (geom.colX[col] + dx).roundToInt(),
                                (geom.colTop + tops[index]).roundToInt(),
                            )
                        }
                        .size(cardW, cardH),
                )
            }
        }

        val dealsShown = game.stock.size / SpiderRules.COLUMN_COUNT + if (shownPlay?.phase == Phase.Deal) 1 else 0
        if (dealsShown > 0) {
            StockPile(
                deals = dealsShown,
                hinted = state.hint is Hint.Deal,
                hintAlpha = hintAlpha,
                geom = geom,
                cardW = cardW,
                cardH = cardH,
            )
        }
        val finishedShown = shownFinished(game, visual, shownPlay)
        FoundationRow(
            suits = finishedShown,
            layoutCount = game.finished.size.coerceAtLeast(finishedShown.size),
            geom = geom,
        )

        val flying = visual?.let { overlayCards(it, shownPlay, geom, game.finished.size) }.orEmpty()
        flying.forEach { placed ->
            PlayingCard(
                card = placed.card,
                selected = placed.scale > 0.92f,
                modifier = Modifier
                    .offset { IntOffset(placed.at.x.roundToInt(), placed.at.y.roundToInt()) }
                    .size(cardW, cardH)
                    .graphicsLayer {
                        scaleX = placed.scale
                        scaleY = placed.scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
            )
        }
        val dragging = drag
        if (dragging != null) {
            val fan = geom.cardH * if (dragging.cards.size > 8) 0.16f else 0.28f
            dragging.cards.forEachIndexed { index, card ->
                val left = dragging.pos.x - dragging.grab.x
                val top = dragging.pos.y - dragging.grab.y + index * fan
                PlayingCard(
                    card = card,
                    selected = true,
                    modifier = Modifier
                        .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                        .size(cardW, cardH),
                )
            }
        }
        snap?.let { flyingSnap ->
            val t = smooth(flyingSnap.progress)
            flyingSnap.cards.forEachIndexed { index, card ->
                val left = lerp(flyingSnap.from.x, flyingSnap.to.x, t)
                val top = lerp(flyingSnap.from.y, flyingSnap.to.y, t) + index * flyingSnap.fan
                PlayingCard(
                    card = card,
                    modifier = Modifier
                        .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                        .size(cardW, cardH),
                )
            }
        }
        demo?.let { ride ->
            val t = smooth(ride.progress)
            val lift = sin(t * PI).toFloat() * geom.cardH * 0.05f
            ride.cards.forEachIndexed { index, card ->
                val (start, end) = ride.spots[index]
                PlayingCard(
                    card = card,
                    hinted = true,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                lerp(start.x, end.x, t).roundToInt(),
                                (lerp(start.y, end.y, t) - lift).roundToInt(),
                            )
                        }
                        .size(cardW, cardH),
                )
            }
        }

        val columnsState = rememberUpdatedState(columns)
        val hiddenState = rememberUpdatedState(hidden)
        val gameState = rememberUpdatedState(game)
        val selectionState = rememberUpdatedState(selection)
        val lockedState = rememberUpdatedState(state.inputLocked)
        val geomState = rememberUpdatedState(geom)
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapOrDrag(
                        onTap = { pos ->
                            if (!lockedState.value) {
                                demoToken++
                                demo = null
                                vm.dismissHint()
                                selection = onTableTap(
                                    pos = pos,
                                    geom = geomState.value,
                                    columns = columnsState.value,
                                    hidden = hiddenState.value,
                                    game = gameState.value,
                                    selection = selectionState.value,
                                    vm = vm,
                                    onDead = { col, index ->
                                        shakeSerial += 1
                                        shake = Shake(col, index, shakeSerial)
                                    },
                                )
                            }
                        },
                        onDragStart = { origin ->
                            if (!lockedState.value) {
                                demoToken++
                                demo = null
                                vm.dismissHint()
                            }
                            val layout = geomState.value
                            val shown = columnsState.value
                            shake = null
                            val hit = hitTest(
                                origin,
                                layout,
                                shown,
                                hiddenState.value,
                                gameState.value.finished.size,
                            ) as? Hit.CardHit
                            val column = hit?.let { shown[it.col] }
                            if (lockedState.value || hit == null || column == null ||
                                !SpiderRules.isMovableRun(column, hit.index)
                            ) {
                                false
                            } else {
                                val tops = ColumnLayout.tops(column, layout.cardH, layout.colMaxH)
                                drag = Drag(
                                    col = hit.col,
                                    index = hit.index,
                                    cards = column.drop(hit.index),
                                    grab = Offset(
                                        origin.x - layout.colX[hit.col],
                                        origin.y - (layout.colTop + tops[hit.index]),
                                    ),
                                    pos = origin,
                                )
                                selection = null
                                true
                            }
                        },
                        onDrag = { pos -> drag = drag?.copy(pos = pos) },
                        onDragEnd = { pos ->
                            val current = drag
                            drag = null
                            if (current != null && !lockedState.value) {
                                val layout = geomState.value
                                val shown = columnsState.value
                                val board = gameState.value
                                val col = columnAt(layout, pos.x, pos.y)
                                if (col != null && SpiderRules.canMove(board, current.col, current.index, col)) {
                                    vm.onDragDrop(current.col, current.index, col)
                                } else {
                                    val tops = ColumnLayout.tops(shown[current.col], layout.cardH, layout.colMaxH)
                                    val home = Offset(layout.colX[current.col], layout.colTop + tops[current.index])
                                    val release = Offset(pos.x - current.grab.x, pos.y - current.grab.y)
                                    val fan = layout.cardH * if (current.cards.size > 8) 0.16f else 0.28f
                                    snapSerial += 1
                                    snapHome = Snap(snapSerial, current.cards, release, home, 0f, fan)
                                    if (col != null && col != current.col) vm.onIllegalMove()
                                }
                            }
                        },
                    )
                },
        )
        TableChrome(
            state = state,
            vm = vm,
            geom = geom,
            finished = finishedShown.size,
        )
    }
}

private fun onTableTap(
    pos: Offset,
    geom: TableGeom,
    columns: List<List<Card>>,
    hidden: Set<Int>,
    game: GameState,
    selection: Selection?,
    vm: GameViewModel,
    onDead: (Int, Int) -> Unit,
): Selection? {
    when (val hit = hitTest(pos, geom, columns, hidden, game.finished.size)) {
        Hit.Stock -> {
            vm.onDeal()
            return null
        }
        is Hit.ColumnHit -> {
            if (selection != null) {
                if (SpiderRules.canMove(game, selection.col, selection.index, hit.col)) {
                    vm.onDrop(selection.col, selection.index, hit.col)
                    return null
                }
                vm.onIllegalMove()
            }
            return selection
        }
        is Hit.CardHit -> {
            if (selection != null && selection.col != hit.col &&
                SpiderRules.canMove(game, selection.col, selection.index, hit.col)
            ) {
                vm.onDrop(selection.col, selection.index, hit.col)
                return null
            }
            if (SpiderRules.isMovableRun(columns[hit.col], hit.index)) {
                val target = SpiderRules.bestTarget(game, hit.col, hit.index)
                if (target != null) {
                    vm.onDrop(hit.col, hit.index, target)
                    return null
                }
                onDead(hit.col, hit.index)
                return null
            }
            onDead(hit.col, hit.index)
            return null
        }
        null -> return null
    }
}

private fun hiddenIds(visual: Visual?, play: Play?, drag: Drag?, snap: Snap?): Set<Int> {
    val ids = mutableSetOf<Int>()
    drag?.cards?.mapTo(ids) { it.id }
    snap?.cards?.mapTo(ids) { it.id }
    if (visual == null || play == null) return ids
    when (play.phase) {
        Phase.Deal -> ids += visual.dealtIds
        Phase.Slide -> visual.slide?.cards?.mapTo(ids) { it.id }
        Phase.Complete -> {
            visual.removed.take(play.run + 1).forEach { run ->
                run.cards.mapTo(ids) { it.id }
            }
        }
    }
    return ids
}

private data class Placed(val card: Card, val at: Offset, val scale: Float = 1f)

private fun overlayCards(visual: Visual, play: Play?, geom: TableGeom, finishedCount: Int): List<Placed> {
    val current = play ?: return emptyList()
    return when (current.phase) {
        Phase.Deal -> dealFlight(visual, geom, current.progress)
        Phase.Slide -> slideFlight(visual, geom, current.progress)
        Phase.Complete -> completeFlight(visual, geom, current.run, current.progress, finishedCount)
    }
}

private fun dealFlight(visual: Visual, geom: TableGeom, progress: Float): List<Placed> {
    return visual.dealtIds.mapIndexedNotNull { index, id ->
        val column = visual.mid.getOrNull(index) ?: return@mapIndexedNotNull null
        val cardIndex = column.indexOfFirst { it.id == id }
        if (cardIndex < 0) return@mapIndexedNotNull null
        val card = column[cardIndex]
        val tops = ColumnLayout.tops(column, geom.cardH, geom.colMaxH)
        val end = Offset(geom.colX[index], geom.colTop + tops[cardIndex])
        val start = Offset(geom.stockLeft, geom.stockTop)
        val t = smooth(stagger(index, progress))
        Placed(card, Offset(lerp(start.x, end.x, t), lerp(start.y, end.y, t)))
    }
}

private fun slideFlight(visual: Visual, geom: TableGeom, progress: Float): List<Placed> {
    val slide = visual.slide ?: return emptyList()
    val fromTops = ColumnLayout.tops(visual.before[slide.fromCol], geom.cardH, geom.colMaxH)
    val toTops = ColumnLayout.tops(visual.mid[slide.toCol], geom.cardH, geom.colMaxH)
    val fromStart = visual.before[slide.fromCol].size - slide.cards.size
    val toStart = visual.mid[slide.toCol].size - slide.cards.size
    val t = smooth(progress)
    return slide.cards.mapIndexed { index, card ->
        val start = Offset(geom.colX[slide.fromCol], geom.colTop + fromTops[fromStart + index])
        val end = Offset(geom.colX[slide.toCol], geom.colTop + toTops[toStart + index])
        Placed(card, Offset(lerp(start.x, end.x, t), lerp(start.y, end.y, t)))
    }
}

private fun completeFlight(
    visual: Visual,
    geom: TableGeom,
    runIndex: Int,
    progress: Float,
    finishedCount: Int,
): List<Placed> {
    val run = visual.removed.getOrNull(runIndex) ?: return emptyList()
    val column = visual.mid[run.column]
    val tops = ColumnLayout.tops(column, geom.cardH, geom.colMaxH)
    val sameColumn = visual.removed.filter { it.column == run.column }
    val which = sameColumn.indexOf(run).coerceAtLeast(0)
    val startIndex = runStart(column.size, sameColumn, which)
    val t = smooth(progress)
    val lift = sin(t * PI).toFloat() * geom.cardH * 0.18f
    val already = (finishedCount - visual.removed.size).coerceAtLeast(0)
    val slot = already + visual.removed.take(runIndex).size
    val count = finishedCount.coerceAtLeast(slot + 1)
    val destX = geom.pileLeft(slot, count)
    val destY = geom.pileTop(slot)
    val endScale = 1f
    return run.cards.mapIndexed { index, card ->
        val cardSlot = startIndex + index
        val start = if (cardSlot in tops.indices) {
            Offset(geom.colX[run.column], geom.colTop + tops[cardSlot])
        } else {
            Offset(geom.colX[run.column], geom.colTop)
        }
        Placed(
            card,
            Offset(
                lerp(start.x, destX + index * (1f - t) * 3f, t),
                lerp(start.y, destY, t) - lift,
            ),
            scale = lerp(1f, endScale, t),
        )
    }
}

private fun runStart(columnSize: Int, runsTopFirst: List<com.mun0n.spider.game.RemovedRun>, which: Int): Int {
    var end = columnSize
    var start = 0
    for (index in 0..which) {
        start = end - runsTopFirst[index].cards.size
        if (index != which) end = start
    }
    return start
}

private fun shownFinished(game: GameState, visual: Visual?, play: Play?): List<Suit> {
    if (visual == null || visual.removed.isEmpty()) return game.finished
    val arrived = if (play?.phase == Phase.Complete) play.run else 0
    val pending = visual.removed.size - arrived
    return game.finished.dropLast(pending.coerceAtLeast(0))
}

private fun openingPlay(visual: Visual): Play? = when {
    visual.dealtIds.isNotEmpty() -> Play(Phase.Deal, 0, 0f)
    visual.slide != null -> Play(Phase.Slide, 0, 0f)
    visual.removed.isNotEmpty() -> Play(Phase.Complete, 0, 0f)
    else -> null
}

private fun demoRide(columns: List<List<Card>>, play: Hint.MoveCards, geom: TableGeom): Demo? {
    val column = columns.getOrNull(play.fromCol) ?: return null
    if (play.fromIndex !in column.indices) return null
    val cards = column.drop(play.fromIndex)
    if (cards.isEmpty()) return null
    val fromTops = ColumnLayout.tops(column, geom.cardH, geom.colMaxH)
    val dest = columns[play.toCol]
    val destTops = ColumnLayout.tops(dest + cards, geom.cardH, geom.colMaxH)
    val destStart = dest.size
    val spots = cards.indices.map { index ->
        Offset(geom.colX[play.fromCol], geom.colTop + fromTops[play.fromIndex + index]) to
            Offset(geom.colX[play.toCol], geom.colTop + destTops[destStart + index])
    }
    return Demo(play.fromCol, play.fromIndex, play.toCol, cards, spots, 0f)
}

private fun hintMatches(hint: Hint?, col: Int, index: Int): Boolean {
    val move = hint as? Hint.MoveCards ?: return false
    return move.fromCol == col && index >= move.fromIndex
}

@Composable
private fun StockPile(
    deals: Int,
    hinted: Boolean,
    hintAlpha: Float,
    geom: TableGeom,
    cardW: Dp,
    cardH: Dp,
) {
    val shape = RoundedCornerShape(CardCorner)
    val nudge = remember { Animatable(0f) }
    LaunchedEffect(hinted) {
        if (!hinted) {
            nudge.snapTo(0f)
            return@LaunchedEffect
        }
        nudge.animateTo(-16f, tween(160, easing = FastOutSlowInEasing))
        nudge.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        nudge.animateTo(-9f, tween(130, easing = FastOutSlowInEasing))
        nudge.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier
            .offset {
                IntOffset(
                    geom.stockLeft.roundToInt(),
                    (geom.stockTop + nudge.value).roundToInt(),
                )
            }
            .size(cardW, cardH)
            .semantics { contentDescription = "Mazo, $deals repartos" },
        contentAlignment = Alignment.Center,
    ) {
        if (deals <= 0) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .border(1.dp, Color.White.copy(alpha = 0.28f), shape),
            )
        } else {
            val layers = minOf(3, deals)
            repeat(layers) { layer ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .offset { IntOffset((layer - layers) * 3, (layer - layers) * 3) }
                        .clip(shape)
                        .border(
                            width = if (hinted) 3.dp else 1.dp,
                            color = if (hinted) HintGold.copy(alpha = hintAlpha) else Color(0xFF5C1018),
                            shape = shape,
                        ),
                ) {
                    CardBackFace()
                }
            }
            Box(
                Modifier
                    .size(width = cardW * 0.52f, height = cardH * 0.30f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xE65C1018)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = deals.toString(),
                    color = Color.White,
                    fontSize = (cardH.value * 0.20f).sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FoundationRow(suits: List<Suit>, layoutCount: Int, geom: TableGeom) {
    val density = LocalDensity.current
    val miniW = with(density) { geom.miniW.toDp() }
    val miniH = with(density) { geom.miniH.toDp() }
    val count = layoutCount.coerceAtLeast(suits.size)
    suits.forEachIndexed { slot, suit ->
        PlayingCard(
            card = Card(id = -(slot + 1), suit = suit, rank = Rank.KING, faceUp = true),
            modifier = Modifier
                .offset {
                    IntOffset(
                        geom.pileLeft(slot, count.coerceAtLeast(1)).roundToInt(),
                        geom.pileTop(slot).roundToInt(),
                    )
                }
                .size(miniW, miniH)
                .semantics { contentDescription = "palo completado ${suit.spanish}" },
        )
    }
}

@Composable
private fun NewGameDialog(
    last: Difficulty,
    records: Records,
    canDismiss: Boolean,
    onPick: (Difficulty) -> Unit,
    onDismiss: () -> Unit,
) {
    Scrim(onDismiss = if (canDismiss) onDismiss else null) {
        Text(
            stringResource(Res.string.choose_game),
            color = Ink,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        )
        Difficulty.entries.forEach { difficulty ->
            val selected = difficulty == last
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) Color(0xFF0E6A38) else Color(0xFF143222))
                    .clickable { onPick(difficulty) }
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .semantics { contentDescription = difficultyLabel(difficulty) },
            ) {
                Text(difficultyLabel(difficulty), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text(difficultyHelp(difficulty), color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
                records.best[difficulty]?.let { best ->
                    Text(
                        stringResource(Res.string.record, best),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        if (canDismiss) {
            Text(
                stringResource(Res.string.keep_playing),
                color = Color(0xFF0E6A38),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun WonDialog(score: Int, best: Int, isRecord: Boolean, onNewGame: () -> Unit) {
    Scrim(onDismiss = null) {
        Text(stringResource(Res.string.you_won), color = Ink, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(
            score.toString(),
            color = Color(0xFF0E6A38),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(stringResource(Res.string.points), color = Ink.copy(alpha = 0.7f), fontSize = 18.sp)
        Text(
            if (isRecord) stringResource(Res.string.new_record) else stringResource(Res.string.record, best),
            color = Color(0xFF0E6A38),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            stringResource(Res.string.new_game),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0E6A38))
                .clickable(onClick = onNewGame)
                .padding(horizontal = 28.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun Scrim(onDismiss: (() -> Unit)?, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onDismiss?.invoke() },
                ),
        )
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Cream)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

private fun difficultyLabel(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.ONE -> "1 palo"
    Difficulty.TWO -> "2 palos"
    Difficulty.FOUR -> "4 palos"
}

private fun difficultyHelp(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.ONE -> "Un solo palo. La más tranquila."
    Difficulty.TWO -> "Picas y corazones."
    Difficulty.FOUR -> "Los cuatro palos. La de verdad."
}

private const val CARD_RATIO = 1.4f

private fun measureTable(width: Float, height: Float, header: Float): TableGeom {
    val edge = 16f
    val gap = 8f
    val rawW = (width - edge * 2 - gap * 9) / 10f
    var cardW = rawW
    var cardH = cardW * CARD_RATIO
    val room = height - edge - header
    if (cardH * 2.35f > room) {
        cardH = room / 2.35f
        cardW = minOf(rawW, cardH / CARD_RATIO)
    }
    cardH = cardH.coerceAtLeast(minOf(72f, rawW * CARD_RATIO))
    cardW = minOf(rawW, cardH / CARD_RATIO).coerceAtLeast(minOf(48f, rawW))
    val rowWidth = cardW * 10 + gap * 9
    val start = ((width - rowWidth) / 2f).coerceAtLeast(edge)
    val colX = List(10) { index -> start + index * (cardW + gap) }
    val stockLeft = width - edge - cardW
    val stockTop = height - edge - cardH
    val colTop = header
    return TableGeom(
        cardW = cardW,
        cardH = cardH,
        colX = colX,
        colTop = colTop,
        colMaxH = (stockTop - colTop).coerceAtLeast(cardH),
        stockLeft = stockLeft,
        stockTop = stockTop,
        miniW = cardW,
        miniH = cardH,
        foundationTop = stockTop,
        fanStep = 7f,
        foundationLeft = edge,
    )
}

private fun hitTest(
    pos: Offset,
    geom: TableGeom,
    columns: List<List<Card>>,
    hidden: Set<Int>,
    foundations: Int,
): Hit? {
    if (pos.x in geom.stockLeft..(geom.stockLeft + geom.cardW) &&
        pos.y in geom.stockTop..(geom.stockTop + geom.cardH)
    ) {
        return Hit.Stock
    }
    if (foundations > 0) {
        val top = geom.pileTop(foundations - 1)
        if (pos.x in geom.foundationLeft..(geom.foundationLeft + geom.cardW) &&
            pos.y in top..(geom.foundationTop + geom.cardH)
        ) {
            return null
        }
    }
    for (col in 9 downTo 0) {
        val left = geom.colX[col]
        val right = left + geom.cardW
        if (pos.x !in left..right) continue
        val cards = columns[col]
        val tops = ColumnLayout.tops(cards, geom.cardH, geom.colMaxH)
        for (index in cards.indices.reversed()) {
            if (cards[index].id in hidden) continue
            val top = geom.colTop + tops[index]
            if (pos.y in top..(top + geom.cardH) && pos.y <= geom.colTop + geom.colMaxH + geom.cardH) {
                return Hit.CardHit(col, index)
            }
        }
        if (pos.y in geom.colTop..(geom.colTop + geom.colMaxH)) return Hit.ColumnHit(col)
    }
    return null
}

private fun columnAt(geom: TableGeom, x: Float, y: Float): Int? {
    val inBand = y >= geom.stockTop
    for (index in 0 until 10) {
        var left = if (index == 0) {
            geom.colX[0] - 24f
        } else {
            (geom.colX[index - 1] + geom.cardW + geom.colX[index]) / 2f
        }
        var right = if (index == 9) {
            geom.colX[9] + geom.cardW + 24f
        } else {
            (geom.colX[index] + geom.cardW + geom.colX[index + 1]) / 2f
        }
        if (inBand && index == 0) left = maxOf(left, geom.foundationLeft + geom.cardW + 8f)
        if (inBand && index == 9) right = minOf(right, geom.stockLeft - 8f)
        if (left <= right && x in left..right) return index
    }
    return null
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapOrDrag(
    onTap: (Offset) -> Unit,
    onDragStart: (Offset) -> Boolean,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown()
        val origin = down.position
        val pointerId = down.id
        var dragging = false
        var accepted = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) {
                if (accepted) onDragEnd(change.position) else if (!dragging) onTap(origin)
                break
            }
            if (!dragging && (change.position - origin).getDistance() >= slop) {
                dragging = true
                accepted = onDragStart(origin)
            }
            if (accepted) {
                change.consume()
                onDrag(change.position)
            }
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun smooth(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3 - 2 * x)
}

private fun stagger(index: Int, progress: Float): Float {
    val start = index * 0.045f
    return ((progress - start) / 0.58f).coerceIn(0f, 1f)
}
