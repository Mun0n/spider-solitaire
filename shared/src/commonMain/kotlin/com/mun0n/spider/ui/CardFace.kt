package com.mun0n.spider.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.mun0n.spider.game.Card
import com.mun0n.spider.game.Rank
import com.mun0n.spider.game.Suit
import com.mun0n.spider.resources.Res
import com.mun0n.spider.resources.court_jack_black
import com.mun0n.spider.resources.court_jack_red
import com.mun0n.spider.resources.court_king_black
import com.mun0n.spider.resources.court_king_red
import com.mun0n.spider.resources.court_queen_black
import com.mun0n.spider.resources.court_queen_red
import org.jetbrains.compose.resources.painterResource

private val CardFaceColor = Color.White
private val CardEdge = Color(0xFFB9B9B9)
private val BackRed = Color(0xFF8E1B2C)
private val BackEdge = Color(0xFF5C1018)
private val GoldLine = Color(0xFFE7C27A)
private val PipRed = Color(0xFFE10600)
private val PipBlack = Color(0xFF161616)

val HintGold = Color(0xFFFFD56A)
val SelectGold = Color(0xFFF2C14E)
val CardCorner = 6.dp

private val PipPositions: Map<Int, List<Pair<Float, Float>>> = mapOf(
    2 to listOf(0.50f to 0.30f, 0.50f to 0.70f),
    3 to listOf(0.50f to 0.27f, 0.50f to 0.50f, 0.50f to 0.73f),
    4 to listOf(0.33f to 0.28f, 0.67f to 0.28f, 0.33f to 0.72f, 0.67f to 0.72f),
    5 to listOf(0.33f to 0.28f, 0.67f to 0.28f, 0.50f to 0.50f, 0.33f to 0.72f, 0.67f to 0.72f),
    6 to listOf(0.33f to 0.24f, 0.67f to 0.24f, 0.33f to 0.50f, 0.67f to 0.50f, 0.33f to 0.76f, 0.67f to 0.76f),
    7 to listOf(
        0.33f to 0.22f, 0.67f to 0.22f, 0.50f to 0.36f,
        0.33f to 0.50f, 0.67f to 0.50f, 0.33f to 0.78f, 0.67f to 0.78f,
    ),
    8 to listOf(
        0.33f to 0.20f, 0.67f to 0.20f, 0.50f to 0.35f,
        0.33f to 0.47f, 0.67f to 0.47f, 0.50f to 0.65f,
        0.33f to 0.80f, 0.67f to 0.80f,
    ),
    9 to listOf(
        0.33f to 0.18f, 0.67f to 0.18f, 0.33f to 0.36f, 0.67f to 0.36f,
        0.50f to 0.50f, 0.33f to 0.64f, 0.67f to 0.64f, 0.33f to 0.82f, 0.67f to 0.82f,
    ),
    10 to listOf(
        0.33f to 0.17f, 0.67f to 0.17f, 0.50f to 0.30f,
        0.33f to 0.39f, 0.67f to 0.39f, 0.33f to 0.61f, 0.67f to 0.61f,
        0.50f to 0.70f, 0.33f to 0.83f, 0.67f to 0.83f,
    ),
)

private val LockedVeil = Color(0xFF1C1C1C)

@Composable
fun PlayingCard(
    card: Card,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    hinted: Boolean = false,
    locked: Boolean = false,
) {
    val shape = RoundedCornerShape(CardCorner)
    val borderColor = when {
        selected -> SelectGold
        hinted -> HintGold
        card.faceUp -> CardEdge
        else -> BackEdge
    }
    Box(
        modifier
            .shadow(elevation = if (card.faceUp) 3.dp else 2.dp, shape = shape, clip = false)
            .clip(shape)
            .background(if (card.faceUp) CardFaceColor else BackRed)
            .border(width = if (selected || hinted) 2.5.dp else 1.dp, color = borderColor, shape = shape)
            .semantics {
                contentDescription = if (card.faceUp) card.description() else "carta boca abajo"
            },
    ) {
        if (card.faceUp) CardFace(card) else CardBackFace()
        if (locked) {
            Box(Modifier.fillMaxSize().background(LockedVeil.copy(alpha = 0.40f)))
        }
    }
}

@Composable
fun CardBackFace(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().padding(5.dp)) {
        val stroke = size.minDimension * 0.018f
        drawRoundRect(
            color = GoldLine,
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = stroke),
        )
        val inset = size.minDimension * 0.055f
        drawRoundRect(
            color = BackEdge,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = stroke * 0.8f),
        )
        val cx = size.width / 2f
        val cy = size.height / 2f
        val arm = size.minDimension * 0.16f
        drawLine(GoldLine, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = stroke)
        drawLine(GoldLine, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = stroke)
    }
}

@Composable
private fun CardFace(card: Card) {
    val color = if (card.suit.red) PipRed else PipBlack
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val ten = card.rank == Rank.TEN
        val rankDp = maxWidth * if (ten) 0.175f else 0.24f
        val mark = maxWidth * 0.16f
        val rankStyle = TextStyle(
            color = color,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = with(density) { rankDp.toSp() },
            lineHeight = with(density) { rankDp.toSp() },
            letterSpacing = if (ten) (-0.06).em else 0.em,
            textAlign = TextAlign.Center,
        )
        val indexPadH = maxWidth * 0.035f
        val indexPadV = maxHeight * 0.008f
        if (card.rank == Rank.ACE) {
            SuitMark(
                card.suit,
                color,
                Modifier.align(Alignment.Center).size(maxWidth * 0.52f),
            )
        } else if (card.rank.value >= Rank.JACK.value) {
            CourtArt(card = card, modifier = Modifier.fillMaxSize())
        } else {
            Pips(card = card, color = color, modifier = Modifier.fillMaxSize())
        }
        Index(
            card,
            color,
            rankStyle,
            mark,
            Modifier
                .align(Alignment.TopStart)
                .background(CardFaceColor)
                .padding(start = indexPadH, top = indexPadV, end = maxWidth * 0.02f, bottom = maxHeight * 0.012f),
        )
        Index(
            card,
            color,
            rankStyle,
            mark,
            Modifier
                .align(Alignment.BottomEnd)
                .background(CardFaceColor)
                .padding(end = indexPadH, bottom = indexPadV, start = maxWidth * 0.02f, top = maxHeight * 0.012f)
                .graphicsLayer { rotationZ = 180f },
        )
    }
}

@Composable
private fun Index(
    card: Card,
    color: Color,
    rankStyle: TextStyle,
    mark: Dp,
    modifier: Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(card.rank.label, style = rankStyle)
        SuitMark(
            card.suit,
            color,
            Modifier.padding(top = mark * 0.06f).size(mark),
        )
    }
}

@Composable
private fun CourtArt(card: Card, modifier: Modifier) {
    val id = when (card.rank) {
        Rank.KING -> if (card.suit.red) Res.drawable.court_king_red else Res.drawable.court_king_black
        Rank.QUEEN -> if (card.suit.red) Res.drawable.court_queen_red else Res.drawable.court_queen_black
        else -> if (card.suit.red) Res.drawable.court_jack_red else Res.drawable.court_jack_black
    }
    Image(
        painter = painterResource(id),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.padding(horizontal = 2.dp, vertical = 3.dp),
    )
}

@Composable
private fun Pips(card: Card, color: Color, modifier: Modifier) {
    val spots = PipPositions[card.rank.value].orEmpty()
    Canvas(modifier) {
        val side = size.width * when {
            card.rank.value >= 9 -> 0.16f
            card.rank.value >= 7 -> 0.185f
            else -> 0.23f
        }
        spots.forEach { (x, y) ->
            val cx = size.width * x
            val cy = size.height * y
            val degrees = if (y > 0.56f) 180f else 0f
            rotate(degrees, Offset(cx, cy)) {
                drawFrenchSuit(card.suit, color, cx - side / 2f, cy - side / 2f, side)
            }
        }
    }
}

@Composable
private fun SuitMark(suit: Suit, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val side = minOf(size.width, size.height)
        drawFrenchSuit(suit, color, (size.width - side) / 2f, (size.height - side) / 2f, side)
    }
}

private fun DrawScope.drawFrenchSuit(suit: Suit, color: Color, left: Float, top: Float, side: Float) {
    val px: (Float) -> Float = { f -> left + side * f }
    val py: (Float) -> Float = { f -> top + side * f }
    when (suit) {
        Suit.HEARTS -> drawPath(heart(px, py), color)
        Suit.DIAMONDS -> drawPath(diamondPip(px, py), color)
        Suit.SPADES -> {
            drawPath(spadeLeaf(px, py), color)
            drawPath(spadeStem(px, py), color)
        }
        Suit.CLUBS -> {
            val r = side * 0.23f
            drawCircle(color, r, Offset(px(0.50f), py(0.30f)))
            drawCircle(color, r, Offset(px(0.28f), py(0.56f)))
            drawCircle(color, r, Offset(px(0.72f), py(0.56f)))
            drawPath(clubStem(px, py), color)
        }
    }
}

private fun heart(px: (Float) -> Float, py: (Float) -> Float) = Path().apply {
    moveTo(px(0.50f), py(0.92f))
    cubicTo(px(0.10f), py(0.62f), px(0.0f), py(0.40f), px(0.04f), py(0.28f))
    cubicTo(px(0.08f), py(0.10f), px(0.28f), py(0.06f), px(0.40f), py(0.16f))
    cubicTo(px(0.46f), py(0.22f), px(0.49f), py(0.30f), px(0.50f), py(0.34f))
    cubicTo(px(0.51f), py(0.30f), px(0.54f), py(0.22f), px(0.60f), py(0.16f))
    cubicTo(px(0.72f), py(0.06f), px(0.92f), py(0.10f), px(0.96f), py(0.28f))
    cubicTo(px(1.0f), py(0.40f), px(0.90f), py(0.62f), px(0.50f), py(0.92f))
    close()
}

private fun diamondPip(px: (Float) -> Float, py: (Float) -> Float) = Path().apply {
    moveTo(px(0.50f), py(0.02f))
    lineTo(px(0.96f), py(0.50f))
    lineTo(px(0.50f), py(0.98f))
    lineTo(px(0.04f), py(0.50f))
    close()
}

private fun spadeLeaf(px: (Float) -> Float, py: (Float) -> Float) = Path().apply {
    moveTo(px(0.50f), py(0.02f))
    cubicTo(px(0.14f), py(0.26f), px(0.0f), py(0.42f), px(0.02f), py(0.58f))
    cubicTo(px(0.04f), py(0.78f), px(0.24f), py(0.84f), px(0.38f), py(0.70f))
    cubicTo(px(0.44f), py(0.64f), px(0.48f), py(0.60f), px(0.50f), py(0.54f))
    cubicTo(px(0.52f), py(0.60f), px(0.56f), py(0.64f), px(0.62f), py(0.70f))
    cubicTo(px(0.76f), py(0.84f), px(0.96f), py(0.78f), px(0.98f), py(0.58f))
    cubicTo(px(1.0f), py(0.42f), px(0.86f), py(0.26f), px(0.50f), py(0.02f))
    close()
}

private fun spadeStem(px: (Float) -> Float, py: (Float) -> Float) = Path().apply {
    moveTo(px(0.42f), py(0.60f))
    quadraticTo(px(0.38f), py(0.80f), px(0.30f), py(0.98f))
    lineTo(px(0.70f), py(0.98f))
    quadraticTo(px(0.62f), py(0.80f), px(0.58f), py(0.60f))
    close()
}

private fun clubStem(px: (Float) -> Float, py: (Float) -> Float) = Path().apply {
    moveTo(px(0.44f), py(0.62f))
    quadraticTo(px(0.40f), py(0.80f), px(0.32f), py(0.98f))
    lineTo(px(0.68f), py(0.98f))
    quadraticTo(px(0.60f), py(0.80f), px(0.56f), py(0.62f))
    close()
}
