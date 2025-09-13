package com.lizpostudio.kgoptometrycrm.graphics

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.lizpostudio.kgoptometrycrm.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MaskView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val LINE_STROKE = 3f
        private const val cRadius = 3f
        private const val shiftLeft = 16
    }

    var fillMask = mutableListOf<MutableList<PointF>>()
    var customDrawing = 0
    private var selectedColor = ContextCompat.getColor(context, R.color.greenCircle)

    private val screenDst = Resources.getSystem().displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = selectedColor
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        strokeWidth = screenDst * LINE_STROKE
        // textSize =screenDst* TEXT_HEIGHT_SP
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val h = (height).toFloat()
        val w = (width / 2).toFloat()

        val radius = 3 * min(h, w) / 8
        val xCenter = w / 2 + w / shiftLeft

        //         Log.d(TAG, "h = $h , w = $w , rd = $radius")

        when (customDrawing) {
            1 -> {
                // Draw Cross - Circle
                paint.color = Color.BLACK
                canvas.drawLine(xCenter, h / 2, xCenter, h / 2 - radius, paint)
                canvas.drawLine(xCenter, h / 2, xCenter, h / 2 + radius, paint)
                canvas.drawLine(xCenter, h / 2, xCenter + radius, h / 2, paint)
                canvas.drawLine(xCenter, h / 2, xCenter - radius, h / 2, paint)


                canvas.drawCircle(w + w / 2, h / 2, radius, paint)
                val innerRadius = radius - paint.strokeWidth
                paint.color = Color.rgb(235, 244, 255)
                canvas.drawCircle(w + w / 2, h / 2, innerRadius, paint)

            }
            else -> paint.color = Color.BLUE

        }

        if (fillMask.isNotEmpty()) {
            for (index in 0..fillMask.lastIndex) {
                //          Log.d(TAG, "fillMask SIZE at drawing = ${fillMask[index].size} ")
                if (fillMask[index].size > 1) {
                    paint.color = (fillMask[index][0].x).toInt()

                    if (fillMask[index].size <= 2) {
                        canvas.drawCircle(
                            fillMask[index][1].x,
                            fillMask[index][1].y,
                            cRadius,
                            paint
                        )
                    } else {
                        for (drawIndex in 2..fillMask[index].lastIndex) {
                            canvas.drawLine(
                                fillMask[index][drawIndex - 1].x, fillMask[index][drawIndex - 1].y,
                                fillMask[index][drawIndex].x, fillMask[index][drawIndex].y, paint
                            )
                        }
                    }
                }
            }
        }
    }

    private fun PointF.computePosition(pos: Float, radius: Float) {
        val startAngle = Math.PI
        val angle = startAngle + pos * 0.0174533
        x = (radius * cos(angle)).toFloat() + width / 2
        y = (radius * sin(angle)).toFloat() + height / 2
    }
}