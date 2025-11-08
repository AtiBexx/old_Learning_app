package com.attibexx.old_learning_app.ui


import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.core.graphics.withSave

/**
 * A "Cooperative Zoom" modellt megvalósító konténer.
 * Letiltja a szülőt nagyításkor, de visszaadja neki a vezérlést,
 * ha a belső pásztázás eléri a határait.
 */
class CooperativeZoomContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Nagyítás engedélyezése
    var isZoomEnabled: Boolean = true


    private val scaleDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f

    private var posX = 0f
    private var posY = 0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var isDragging = false

    init {
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    // A dispatchTouchEvent adja a legmagasabb szintű kontrollt az események felett.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
       // Ha engedélyezve van a nagyítás
       // if allowed this zoom
        if (!isZoomEnabled) {
           if (scaleFactor > 1.0f) {
               resetZoom()
           }
       }
        // A scale detector mindig kapja meg az eseményt.
        scaleDetector.onTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = ev.x
                lastTouchY = ev.y
                activePointerId = ev.getPointerId(0)
                isDragging = false

                // Nagyításkor mindig miénk a mozdulat, tiltsuk a szülőt.
                if (scaleFactor > 1.0f) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - lastTouchX
                val dy = ev.y - lastTouchY

                if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    isDragging = true
                }

                if (isDragging && scaleFactor > 1.0f && !scaleDetector.isInProgress) {
                    // Pásztázás logikája
                    val newPosY = posY + dy
                    val contentHeight = (getChildAt(0)?.height ?: 0) * scaleFactor
                    val maxScroll = height - contentHeight

                    val canPanUp = newPosY < 0 // Felfelé pásztázás
                    val canPanDown = newPosY > maxScroll // Lefelé pásztázás

                    if ((dy > 0 && canPanUp) || (dy < 0 && canPanDown)) {
                        // Ha a mozdulat a határokon belül tart, mi kezeljük.
                        parent.requestDisallowInterceptTouchEvent(true)
                        posX += dx
                        posY += dy
                        applyConstraints()
                        invalidate()
                    } else {
                        // Ha elértük a határt, visszaadjuk a vezérlést a szülőnek.
                        parent.requestDisallowInterceptTouchEvent(false)
                    }

                    lastTouchX = ev.x
                    lastTouchY = ev.y
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                activePointerId = MotionEvent.INVALID_POINTER_ID
                isDragging = false
            }
        }
        super.dispatchTouchEvent(ev)
        return true // Mindig kezeljük, hogy az ACTION_MOVE és ACTION_UP is eljusson hozzánk.
    }

    // A többi kód (dispatchDraw, applyConstraints, ScaleListener) változatlan,
    // mert azok már helyesen működtek. Csak az eseménykezelés logikája változott.

    override fun dispatchDraw(canvas: Canvas) {
        canvas.withSave {
            applyConstraints()
            translate(posX, posY)
            scale(scaleFactor, scaleFactor)
            super.dispatchDraw(this)
        }
    }

    private fun applyConstraints() {
        val child = getChildAt(0) ?: return
        if (child.height == 0 || child.width == 0) return

        val contentTotalWidth = child.width * scaleFactor
        if (contentTotalWidth < width) {
            posX = (width - contentTotalWidth) / 2f
        } else {
            posX = max(posX, width - contentTotalWidth)
            posX = min(posX, 0f)
        }

        val contentTotalHeight = child.height * scaleFactor
        if (contentTotalHeight < height) {
            posY = (height - contentTotalHeight) / 2f
        } else {
            posY = max(posY, height - contentTotalHeight)
            posY = min(posY, 0f)
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val previousScaleFactor = scaleFactor
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(1.0f, min(scaleFactor, 3.0f))

            val scaleChange = scaleFactor / previousScaleFactor
            posX = detector.focusX - (detector.focusX - posX) * scaleChange
            posY = detector.focusY - (detector.focusY - posY) * scaleChange

            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (scaleFactor <= 1.0f) {
                // Visszaállás alaphelyzetbe
                posX = 0f
                posY = 0f
                scaleFactor = 1.0f
                parent.requestDisallowInterceptTouchEvent(false) // Engedélyezzük a szülőt
            }
            invalidate()
        }
    }

    // Nagyítás visszaállítása osztály
    // Reset zoom class
    private fun resetZoom() {
        posX = 0f
        posY = 0f
        scaleFactor = 1.0f
        parent.requestDisallowInterceptTouchEvent(false)
        invalidate()
    }
}
