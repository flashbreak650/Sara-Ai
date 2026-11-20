package com.sara.assistant.system

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.sin

/**
 * Renders the floating SARA button & compact voice conversation card outside the app
 * over the Android Home Screen (requires explicit SYSTEM_ALERT_WINDOW overlay permission).
 * Light-only aesthetic: crisp white surfaces, soft blue tint, cyan accents.
 */
class SaraFloatingOverlayManager(
    private val context: Context,
    private val onTalkClick: () -> Unit,
    private val onPauseResumeClick: () -> Unit,
    private val onStopConversationClick: () -> Unit,
    private val onOpenAppClick: () -> Unit,
    private val onDismissOverlayClick: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var bubbleView: View? = null
    private var cardView: View? = null
    private var isExpanded = false

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var cardParams: WindowManager.LayoutParams? = null

    private var bubbleOrbView: FloatingOrbPulseView? = null
    private var cardWaveformView: FloatingWaveformView? = null
    private var statusBadgeText: TextView? = null
    private var transcriptText: TextView? = null
    private var replyText: TextView? = null
    private var pauseResumeButton: TextView? = null
    private var talkButton: TextView? = null

    private var latestState = BackgroundAssistantState()

    fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(context)

    fun isShowing(): Boolean = bubbleView != null || cardView != null

    fun showBubble() {
        if (!canDrawOverlay()) return
        if (isExpanded) {
            showCompactCard()
            return
        }
        if (bubbleView != null) {
            updateState(latestState)
            return
        }
        removeCardOnly()

        val dp = context.resources.displayMetrics.density
        val sizePx = (66 * dp).toInt()
        val params = createOverlayParams(
            width = sizePx,
            height = sizePx,
            x = (16 * dp).toInt(),
            y = (180 * dp).toInt()
        )
        bubbleParams = params

        val orb = FloatingOrbPulseView(context).apply {
            setOnTouchListener(DraggableTouchListener(params) {
                expandToCard()
            })
        }
        bubbleOrbView = orb
        bubbleView = orb
        runCatching { windowManager.addView(orb, params) }
        updateState(latestState)
    }

    fun expandToCard() {
        isExpanded = true
        showCompactCard()
    }

    fun collapseToBubble() {
        isExpanded = false
        removeCardOnly()
        showBubble()
    }

    fun hideAll() {
        isExpanded = false
        removeBubbleOnly()
        removeCardOnly()
    }

    fun updateState(state: BackgroundAssistantState) {
        latestState = state
        bubbleOrbView?.setPhase(state.phase, state.isPaused)
        cardWaveformView?.setPhase(state.phase, state.isPaused)

        val statusLabel = when {
            state.isPaused -> "বিরতি · Paused"
            state.phase == BackgroundAssistantPhase.LISTENING_COMMAND -> "কথা বলুন · Listening"
            state.phase == BackgroundAssistantPhase.LISTENING_WAKE -> "শুনছি · Ready"
            state.phase == BackgroundAssistantPhase.THINKING -> "ভাবছি · Thinking"
            state.phase == BackgroundAssistantPhase.SPEAKING -> "বলছি · Speaking"
            else -> "প্রস্তুত · Standby"
        }
        statusBadgeText?.text = statusLabel
        statusBadgeText?.setTextColor(
            when {
                state.isPaused -> Color.parseColor("#D97706")
                state.phase == BackgroundAssistantPhase.SPEAKING -> Color.parseColor("#0284C7")
                else -> Color.parseColor("#0891B2")
            }
        )

        transcriptText?.text = if (state.lastHeardText.isNotBlank()) {
            "আপনি: ${state.lastHeardText}"
        } else {
            "\"Hey SARA\" বলুন অথবা নিচে 'কথা বলুন' ট্যাপ করুন"
        }

        replyText?.text = if (state.lastResponseText.isNotBlank()) {
            "SARA: ${state.lastResponseText}"
        } else {
            state.statusMessage
        }

        pauseResumeButton?.text = if (state.isPaused) "চালু · Resume" else "বিরতি · Pause"
        talkButton?.text = if (state.phase == BackgroundAssistantPhase.LISTENING_COMMAND) {
            "শুনছি..."
        } else {
            "কথা বলুন · Talk"
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showCompactCard() {
        if (!canDrawOverlay()) return
        removeBubbleOnly()
        if (cardView != null) {
            updateState(latestState)
            return
        }

        val dp = context.resources.displayMetrics.density
        val cardWidth = (312 * dp).toInt()
        val params = createOverlayParams(
            width = cardWidth,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            x = (16 * dp).toInt(),
            y = (120 * dp).toInt()
        )
        cardParams = params

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (14 * dp).toInt()
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#EFF6FF"))
            ).apply {
                cornerRadius = 22 * dp
                setStroke((1.5f * dp).toInt(), Color.parseColor("#BAE6FD"))
            }
            elevation = 12 * dp
        }

        // Header row (draggable)
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setOnTouchListener(DraggableTouchListener(params, onTap = null, targetViewProvider = { cardView }))
        }

        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val titleTxt = TextView(context).apply {
            text = "SARA Assistant"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
        }
        val badgeTxt = TextView(context).apply {
            text = "শুনছি · Ready"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0891B2"))
        }
        statusBadgeText = badgeTxt
        titleCol.addView(titleTxt)
        titleCol.addView(badgeTxt)

        val minimizeBtn = createPillAction(
            label = "—",
            bgHex = "#E0F2FE",
            textHex = "#0369A1",
            dp = dp
        ) { collapseToBubble() }

        val closeBtn = createPillAction(
            label = "×",
            bgHex = "#FEE2E2",
            textHex = "#DC2626",
            dp = dp
        ) { onDismissOverlayClick() }

        header.addView(titleCol)
        header.addView(minimizeBtn)
        val spacerHeader = View(context).apply {
            layoutParams = LinearLayout.LayoutParams((6 * dp).toInt(), 1)
        }
        header.addView(spacerHeader)
        header.addView(closeBtn)
        container.addView(header)

        // Animated Waveform Bar
        val wave = FloatingWaveformView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (34 * dp).toInt()
            ).apply {
                topMargin = (8 * dp).toInt()
                bottomMargin = (8 * dp).toInt()
            }
        }
        cardWaveformView = wave
        container.addView(wave)

        // Transcript & Reply Box
        val transcriptBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = (10 * dp).toInt()
            setPadding(p, p, p, p)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F8FBFF"))
                cornerRadius = 14 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#D8E8FA"))
            }
        }
        val heard = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#334155"))
            maxLines = 2
        }
        val reply = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            maxLines = 3
            setPadding(0, (4 * dp).toInt(), 0, 0)
        }
        transcriptText = heard
        replyText = reply
        transcriptBox.addView(heard)
        transcriptBox.addView(reply)
        container.addView(transcriptBox)

        // Primary Actions Row: Talk + Stop Conversation
        val row1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (10 * dp).toInt()
            }
        }
        val btnTalk = createCardButton(
            label = "কথা বলুন · Talk",
            bgStart = "#0891B2",
            bgEnd = "#0284C7",
            textHex = "#FFFFFF",
            dp = dp
        ) { onTalkClick() }
        talkButton = btnTalk

        val btnStop = createCardButton(
            label = "থামান · Stop",
            bgStart = "#FEE2E2",
            bgEnd = "#FECACA",
            textHex = "#B91C1C",
            dp = dp
        ) { onStopConversationClick() }

        row1.addView(btnTalk, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = (6 * dp).toInt()
        })
        row1.addView(btnStop, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        container.addView(row1)

        // Secondary Actions Row: Pause/Resume + Open App
        val row2 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * dp).toInt()
            }
        }
        val btnPause = createCardButton(
            label = "বিরতি · Pause",
            bgStart = "#E0F2FE",
            bgEnd = "#E0F2FE",
            textHex = "#0369A1",
            dp = dp
        ) { onPauseResumeClick() }
        pauseResumeButton = btnPause

        val btnOpen = createCardButton(
            label = "অ্যাপ খুলুন · App",
            bgStart = "#FFFFFF",
            bgEnd = "#F1F5F9",
            textHex = "#0F172A",
            dp = dp
        ) { onOpenAppClick() }

        row2.addView(btnPause, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = (6 * dp).toInt()
        })
        row2.addView(btnOpen, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        container.addView(row2)

        cardView = container
        runCatching { windowManager.addView(container, params) }
        updateState(latestState)
    }

    private fun createOverlayParams(width: Int, height: Int, x: Int, y: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    private fun createPillAction(
        label: String,
        bgHex: String,
        textHex: String,
        dp: Float,
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(textHex))
            gravity = Gravity.CENTER
            val s = (30 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(s, s)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgHex))
                cornerRadius = 15 * dp
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createCardButton(
        label: String,
        bgStart: String,
        bgEnd: String,
        textHex: String,
        dp: Float,
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(textHex))
            gravity = Gravity.CENTER
            val hPad = (8 * dp).toInt()
            val vPad = (9 * dp).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor(bgStart), Color.parseColor(bgEnd))
            ).apply {
                cornerRadius = 12 * dp
            }
            setOnClickListener { onClick() }
        }
    }

    private fun removeBubbleOnly() {
        bubbleOrbView?.stopAnim()
        bubbleView?.let { v -> runCatching { windowManager.removeView(v) } }
        bubbleView = null
        bubbleOrbView = null
    }

    private fun removeCardOnly() {
        cardWaveformView?.stopAnim()
        cardView?.let { v -> runCatching { windowManager.removeView(v) } }
        cardView = null
        cardWaveformView = null
        statusBadgeText = null
        transcriptText = null
        replyText = null
        pauseResumeButton = null
        talkButton = null
    }

    private inner class DraggableTouchListener(
        private val params: WindowManager.LayoutParams,
        private val onTap: (() -> Unit)?,
        private val targetViewProvider: (() -> View?)? = null
    ) : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var moved = false

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    val target = targetViewProvider?.invoke() ?: v
                    runCatching { windowManager.updateViewLayout(target, params) }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onTap?.invoke()
                    return true
                }
            }
            return false
        }
    }
}

private class FloatingOrbPulseView(context: Context) : View(context) {
    private var phase = BackgroundAssistantPhase.LISTENING_WAKE
    private var paused = false
    private var animProgress = 0f

    private val animator = ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat()).apply {
        duration = 1800
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            animProgress = it.animatedValue as Float
            invalidate()
        }
        start()
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setPhase(nextPhase: BackgroundAssistantPhase, isPaused: Boolean) {
        phase = nextPhase
        paused = isPaused
        invalidate()
    }

    fun stopAnim() {
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = (minOf(width, height) * 0.36f)
        val pulse = if (paused) 0f else (sin(animProgress) * 0.5f + 0.5f)

        // Outer translucent pulse ring
        ringPaint.color = when {
            paused -> Color.argb(120, 217, 119, 6)
            phase == BackgroundAssistantPhase.SPEAKING -> Color.argb((120 + pulse * 100).toInt(), 2, 132, 199)
            else -> Color.argb((110 + pulse * 110).toInt(), 8, 145, 178)
        }
        ringPaint.strokeWidth = width * 0.055f
        canvas.drawCircle(cx, cy, r + pulse * (width * 0.06f), ringPaint)

        // Gradient core sphere
        bgPaint.shader = LinearGradient(
            cx - r,
            cy - r,
            cx + r,
            cy + r,
            if (paused) Color.parseColor("#94A3B8") else Color.parseColor("#06B6D4"),
            if (paused) Color.parseColor("#64748B") else Color.parseColor("#0284C7"),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, bgPaint)

        // Label "SARA"
        textPaint.textSize = r * 0.52f
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("SARA", cx, textY, textPaint)

        // Status indicator dot
        dotPaint.color = when {
            paused -> Color.parseColor("#F59E0B")
            phase == BackgroundAssistantPhase.SPEAKING -> Color.parseColor("#38BDF8")
            else -> Color.parseColor("#10B981")
        }
        canvas.drawCircle(cx + r * 0.68f, cy - r * 0.68f, r * 0.24f, dotPaint)
    }
}

private class FloatingWaveformView(context: Context) : View(context) {
    private var phase = BackgroundAssistantPhase.LISTENING_WAKE
    private var paused = false
    private var tick = 0f

    private val animator = ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat()).apply {
        duration = 1200
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            tick = it.animatedValue as Float
            invalidate()
        }
        start()
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    fun setPhase(nextPhase: BackgroundAssistantPhase, isPaused: Boolean) {
        phase = nextPhase
        paused = isPaused
        invalidate()
    }

    fun stopAnim() {
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        val bars = 13
        val barWidth = width / (bars * 2.1f)
        val gap = (width - bars * barWidth) / (bars + 1)
        val intensity = when {
            paused -> 0.16f
            phase == BackgroundAssistantPhase.SPEAKING -> 0.95f
            phase == BackgroundAssistantPhase.LISTENING_COMMAND -> 0.85f
            phase == BackgroundAssistantPhase.THINKING -> 0.6f
            else -> 0.35f
        }

        barPaint.color = when {
            paused -> Color.parseColor("#94A3B8")
            phase == BackgroundAssistantPhase.SPEAKING -> Color.parseColor("#0284C7")
            else -> Color.parseColor("#0891B2")
        }

        for (i in 0 until bars) {
            val wave = if (paused) 0.25f else (sin(tick + i * 0.6f) * 0.5f + 0.5f)
            val h = maxOf(height * 0.16f, height * (0.18f + 0.78f * wave * intensity))
            val left = gap + i * (barWidth + gap)
            val top = (height - h) / 2f
            rect.set(left, top, left + barWidth, top + h)
            canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, barPaint)
        }
    }
}
