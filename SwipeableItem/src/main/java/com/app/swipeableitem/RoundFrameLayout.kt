package com.app.swipeableitem

import android.content.Context
import android.graphics.*
import android.util.DisplayMetrics
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.roundToInt


class RoundFrameLayout(context: Context, private var isForegroundView: Boolean): FrameLayout(context){
    var animationType: AnimationType = AnimationType.Open
    var borderDelta: Float = 0f
    private var path = Path()
    var cornerRadius = 0f
    set(value) {
        field = value
        refreshPath()
    }

    private val cornerFromMoveAction: Float by lazy { cornerRadius }

    var moveDirection: ViewMoveDirection = ViewMoveDirection.EndToStart
    set(value) {
        field = value
        refreshPath()
    }

    private var viewTranslation: Int = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if(isForegroundView) {
            path.reset()
            path.addRoundRect(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                cornerRadius,
                cornerRadius,
                Path.Direction.CCW
            )
            if (moveDirection == ViewMoveDirection.EndToStart) {
                path.addRect(0f, 0f, width - cornerRadius, height.toFloat(), Path.Direction.CCW)
            } else {
                path.addRect(
                    cornerRadius,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    Path.Direction.CCW
                )
            }
        }
    }

    fun measure(translation: Float, parentWidth: Int){
        if(!isForegroundView){
            viewTranslation = abs(translation.toInt())

            if(animationType == AnimationType.Open) {
                if(translation > 0f){
                    translationX = 0f
                    getChildAt(0).translationX = 0f
                } else {
                    translationX = parentWidth - viewTranslation - cornerRadius
                    getChildAt(0).translationX = viewTranslation - parentWidth + cornerRadius
                }

                layout(0, 0, viewTranslation + cornerRadius.toInt(), height)
            } else if (animationType == AnimationType.Move) {
                if(translation > 0f){
                    translationX = 0f
                    getChildAt(0).translationX = viewTranslation + cornerFromMoveAction - parentWidth.toFloat()
                } else {
                    translationX = parentWidth - viewTranslation.toFloat() - cornerFromMoveAction.toInt()
                    getChildAt(0).translationX = 0f
                }

                layout(0, 0, viewTranslation + cornerFromMoveAction.toInt(), height)
            }
            refreshPath()
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if(!isForegroundView){
            var viewWidth = 0
            if(animationType == AnimationType.Open){
                viewWidth = if(viewTranslation != 0) { viewTranslation + cornerRadius.toInt() } else { 0 }//viewWidth = if(viewTranslation != 0) { viewTranslation + 2 * cornerRadius.toInt() } else { 0 }
            } else if(animationType == AnimationType.Move){
                viewWidth = if(viewTranslation != 0) { viewTranslation + cornerFromMoveAction.toInt()} else { 0 }
            }
            setMeasuredDimension(viewWidth,heightMeasureSpec)
        }
    }



    override fun onDraw(canvas: Canvas?) {
        if(isForegroundView){
            canvas?.clipPath(path)
        } else {
            canvas?.clipPath(path)
        }
        super.onDraw(canvas)
    }

    private fun refreshPath(){
        path.reset()

       if(isForegroundView) {
           val cornerArr = if (moveDirection == ViewMoveDirection.EndToStart) {
                floatArrayOf(0f,0f, cornerRadius,cornerRadius, cornerRadius,cornerRadius, 0f,0f)
            } else {
               floatArrayOf(cornerRadius,cornerRadius, 0f,0f, 0f,0f, cornerRadius,cornerRadius)
            }

            path.addRoundRect(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                cornerArr,
                Path.Direction.CCW
            )
        } else {
            path.addRect(
                0f,
                height.toFloat(),
                width.toFloat(),
                0f,
                Path.Direction.CCW
            )
           if(animationType == AnimationType.Open) {
               if (moveDirection == ViewMoveDirection.EndToStart){
                   path.op(Path().apply {
                       addRoundRect(0f,
                           0f,
                           cornerRadius - borderDelta,
                           height.toFloat(),
                           floatArrayOf(0f,0f, cornerRadius,cornerRadius, cornerRadius,cornerRadius, 0f,0f),
                           Path.Direction.CCW)
                   }, Path.Op.DIFFERENCE)
               } else {
                   path.op(Path().apply {
                       addRoundRect(width.toFloat() - cornerRadius + borderDelta,
                           0f,
                           width.toFloat() + borderDelta,
                           height.toFloat(),
                           floatArrayOf(cornerRadius,cornerRadius, 0f,0f, 0f,0f, cornerRadius,cornerRadius),
                           Path.Direction.CCW)
                   }, Path.Op.DIFFERENCE)
               }
           } else if(animationType == AnimationType.Move) {
               if (moveDirection == ViewMoveDirection.EndToStart){
                   path.op(Path().apply {
                       addRoundRect(0f,
                           0f,
                           cornerFromMoveAction - borderDelta,
                           height.toFloat(),
                           floatArrayOf(0f,0f, cornerRadius,cornerRadius, cornerRadius,cornerRadius, 0f,0f),
                           Path.Direction.CCW)
                   }, Path.Op.DIFFERENCE)
               } else {
                   path.op(Path().apply {
                       addRoundRect(width.toFloat() - cornerFromMoveAction + borderDelta,
                           0f,
                           width.toFloat() + borderDelta,
                           height.toFloat(),
                           floatArrayOf(cornerRadius,cornerRadius, 0f,0f, 0f,0f, cornerRadius,cornerRadius),
                           Path.Direction.CCW)
                   }, Path.Op.DIFFERENCE)
               }
           }
        }
    }
}