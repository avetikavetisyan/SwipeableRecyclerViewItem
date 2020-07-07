package com.app.swipeableitem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.roundToInt

class SwipeableItem(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs), View.OnTouchListener, View.OnAttachStateChangeListener {
    companion object {
        private var viewRef: WeakReference<SwipeableItem?>? = null
            set(value) {
                val oldView = field?.get()
                if (oldView != null && !oldView.multipleOpen && (oldView.isOpen || oldView.valueAnimator?.isRunning == true) && oldView.itemPosition != value?.get()?.itemPosition) {
                    oldView.close()
                }
                field = value
            }
    }
    var cornerRadius = dpToPx(10).toFloat()
        set(value) {
            field = value
            viewForeground.cornerRadius = cornerRadius
            viewBackground.cornerRadius = cornerRadius
        }
    var borderWidth: Float = dpToPx(2).toFloat()
    var viewElevation: Float = dpToPx(3).toFloat()
    var openingDim: Float = 0.3f
    var borderColor: Int? = Color.LTGRAY
    var swipeDirection: SwipeDirection = SwipeDirection.ToEnd
    var closeDuration: Long = 200L
    var isOpenOverShoot: Boolean = false
    var animationType: AnimationType = AnimationType.Open
    var removeRadiusAfterMove: Boolean = true
    var closeAfterScroll: Boolean = false
        set(value) {
            field = value
            if(field) {
                this.viewTreeObserver.addOnScrollChangedListener {
                    if (viewState == ViewState.Opened && viewStartY != 0 && abs(abs(viewStartY) - abs(y)) > yMoveDelta) {
                        close()
                        viewStartY = abs(y.toInt())
                    }
                    if (viewStartY == 0) {
                        viewStartY = abs(y.toInt())
                    }
                }
            }
        }


    private lateinit var viewForeground: RoundFrameLayout
    private lateinit var viewBackground: RoundFrameLayout
    private var drawable: ShapeDrawable? = null
    private val moveMaxDelta: Float by lazy {
        if(isOpenOverShoot) {viewForeground.width.toFloat() * (openingDim / 2f) * 3f} else { viewForeground.width.toFloat() * openingDim }
    }
    var itemPosition: Int = 0
    private val yMoveDelta = dpToPx(20)
    private var viewStartY = 0
    private var startX = 0f
    private var startY = 0f
    private var viewStartX = 0f
    private var needRadius = true
    private val multipleOpen = false
    private val moveDelta = dpToPx(2).toFloat()
    private var viewState = ViewState.Closed
    private var viewDirection = ViewMoveDirection.No
    private var touchDirection = TouchDirection.No
    private var returnAction = false
    private var isAfterMove: Boolean = false
    var valueAnimator: ValueAnimator? = null

    var isOpen: Boolean
        get() {
            return viewState == ViewState.Opened
        }
        set(value) {
            if (value) {
                if (viewState == ViewState.Closed)
                    open()
            } else {
                if (viewState == ViewState.Opened || valueAnimator?.isStarted == true)
                    close(withAnimation = false)
            }
        }

    private var isStartToEnd: Boolean = false

    init {
        setOnTouchListener(this)
        addOnAttachStateChangeListener(this)
    }

    init {
        attrs?.let {
            val typedArray =
                context.obtainStyledAttributes(it, R.styleable.SwipeableItem, 0, 0)

            borderWidth =
                typedArray.getDimension(R.styleable.SwipeableItem_border_width, 0f)
            viewElevation = typedArray.getDimension(
                R.styleable.SwipeableItem_foreground_view_elevation,
                0f
            )
            openingDim = typedArray.getFloat(R.styleable.SwipeableItem_open_value, 0f)
            isOpenOverShoot = typedArray.getBoolean(R.styleable.SwipeableItem_open_over_shoot, false)
            closeAfterScroll = typedArray.getBoolean(R.styleable.SwipeableItem_close_after_scroll, false)
            removeRadiusAfterMove = typedArray.getBoolean(R.styleable.SwipeableItem_remove_radius_after_move, true)
            closeDuration =
                typedArray.getInt(R.styleable.SwipeableItem_close_duration, 200).toLong()
            val borderColorValue = typedArray.getResourceId(R.styleable.SwipeableItem_border_color, -1)
            if(borderColorValue != -1) {
                borderColor = ContextCompat.getColor(context, borderColorValue)
            }
            swipeDirection = SwipeDirection.values()[typedArray.getInt(
                R.styleable.SwipeableItem_swipe_direction,
                0
            )]

            animationType = AnimationType.values()[typedArray.getInt(
                R.styleable.SwipeableItem_animation_type,
                0
            )]

            drawable = getDrawable(null)
            val backgroundViewRes =
                typedArray.getResourceId(R.styleable.SwipeableItem_background_view, -1)
            viewBackground = RoundFrameLayout(context, false).apply {
                View.inflate(context, backgroundViewRes, this)
                setBackgroundColor(Color.TRANSPARENT)
                animationType = this@SwipeableItem.animationType
                borderDelta = borderWidth/3
            }
            addView(viewBackground)

            val foregroundViewRes =
                typedArray.getResourceId(R.styleable.SwipeableItem_foreground_view, -1)
            viewForeground = RoundFrameLayout(context, true).apply {
                View.inflate(context, foregroundViewRes, this)
                isClickable = true
            }
            cornerRadius =
                typedArray.getDimension(R.styleable.SwipeableItem_corner_radius, dpToPx(10).toFloat())

            addView(viewForeground)
            typedArray.recycle()
        }
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                isAfterMove = false
                valueAnimator?.pause()
                viewBackground.measure(viewForeground.translationX, width)
                startX = event.rawX
                startY = event.rawY
                needRadius = true
                if (!multipleOpen && parent is ViewGroup)
                    (parent as ViewGroup).isMotionEventSplittingEnabled = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchDirection == TouchDirection.No) {
                    if (moveDelta < abs(event.rawX - startX) || moveDelta < abs((event.rawY - startY))) {
                        touchDirection =
                            if (abs(event.rawX - startX) > abs((event.rawY - startY))) {
                                if (parent is RecyclerView) {
                                    (parent as RecyclerView).isLayoutFrozen = true
                                }
                                TouchDirection.Horizontal
                            } else {
                                TouchDirection.Vertical
                            }
                    }
                }
                if (touchDirection == TouchDirection.Horizontal) {
                    isAfterMove = true
                    valueAnimator?.cancel()
                    move(event)
                }
            }
            MotionEvent.ACTION_UP -> {
                needRadius = true
                if (view?.parent is RecyclerView) {
                    (parent as RecyclerView).isLayoutFrozen = false
                }
                if (touchDirection == TouchDirection.Horizontal) {
                    close(event)
                }
                if (!isAfterMove) {
                    valueAnimator?.resume()
                }
                touchDirection = TouchDirection.No
            }
            else -> {
                needRadius = true
                if (view?.parent is RecyclerView) {
                    (parent as RecyclerView).isLayoutFrozen = false
                }
                if (touchDirection == TouchDirection.Horizontal) {
                    close(event)
                }
                if (!isAfterMove) {
                    valueAnimator?.resume()
                }
                touchDirection = TouchDirection.No
            }
        }
        return true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        onTouch(this, ev)
        return false
    }

    private fun pxToDp(px: Int): Int {
        val displayMetrics = context.resources.displayMetrics
        return (px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    }

    private fun dpToPx(dp: Int): Int {
        val displayMetrics = context.resources.displayMetrics
        return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    }

    private fun move(event: MotionEvent) {
        val moveDelta = viewStartX + event.rawX - startX
        val foregroundViewXTranslation =
            if (swipeDirection == SwipeDirection.ToEnd && moveDelta > 0) {
                returnAction = false

                if (moveDelta >= moveMaxDelta) {
                    moveMaxDelta
                } else {
                    moveDelta
                }
            } else if (swipeDirection == SwipeDirection.ToStart && moveDelta < 0) {
                returnAction = false
                if (moveDelta <= -moveMaxDelta) {
                    -moveMaxDelta
                } else {
                    moveDelta
                }
            } else if (swipeDirection == SwipeDirection.Both) {
                returnAction = false
                if (abs(moveDelta) >= moveMaxDelta) {
                    if (moveDelta > 0) moveMaxDelta else -moveMaxDelta
                } else {
                    moveDelta
                }
            } else {
                returnAction = true
                0f
            }

        viewBackground.cornerRadius = cornerRadius
        viewBackground.measure(foregroundViewXTranslation, width)
        viewForeground.x = foregroundViewXTranslation

        if (viewForeground.translationX > 0) {
            if (viewDirection != ViewMoveDirection.StartToEnd) {
                viewDirection = ViewMoveDirection.StartToEnd
                drawable = getDrawable(viewDirection)
                viewForeground.moveDirection = viewDirection
                viewBackground.moveDirection = viewDirection
                needRadius = true
            }
        } else if (viewForeground.translationX < 0) {
            if (viewDirection != ViewMoveDirection.EndToStart) {
                viewDirection = ViewMoveDirection.EndToStart
                drawable = getDrawable(viewDirection)
                viewForeground.moveDirection = viewDirection
                viewBackground.moveDirection = viewDirection
                needRadius = true
            }
        } else {
            needRadius = false
        }
        if (needRadius) {
            needRadius = false
            viewForeground.foreground = drawable
            viewForeground.elevation = viewElevation
        }
    }

    private fun open() {
        if (isStartToEnd) {
            viewForeground.translationX = viewForeground.width.toFloat() * openingDim
            viewDirection = ViewMoveDirection.StartToEnd
        } else {
            viewForeground.translationX = viewForeground.width.toFloat() * -openingDim
            viewDirection = ViewMoveDirection.EndToStart
        }
        viewForeground.elevation = viewElevation
        viewForeground.foreground = null
        viewBackground.cornerRadius = if(removeRadiusAfterMove) {
            0f
        } else {
            cornerRadius
        }
        viewState = ViewState.Opened
        viewBackground.measure(viewForeground.translationX, width)
        viewForeground.invalidate()
        viewStartX = viewForeground.x
        viewRef = WeakReference(this)
    }

    private fun close(event: MotionEvent? = null, withAnimation: Boolean = true) {
        val duration: Long
        val currentElevation: Float
        val currentState: ViewState
        var translationPosition: Float
        val viewCurrentDirection: ViewMoveDirection

        if (event != null && !returnAction &&
            ((viewState == ViewState.Closed && (viewForeground.width.toFloat() * openingDim) <= abs(viewForeground.translationX)) ||//event.rawX - startX
                    (viewState == ViewState.Opened && ((viewDirection == ViewMoveDirection.EndToStart && viewForeground.x <= viewStartX && viewStartX <= 0f) ||
                            (viewDirection == ViewMoveDirection.StartToEnd && viewForeground.x >= viewStartX && viewStartX >= 0f))))
        ) {
            duration = if(isOpenOverShoot) { closeDuration / 4 } else { 0 }
            currentElevation = viewElevation
            currentState = ViewState.Opened
            viewRef = WeakReference(this)
            viewCurrentDirection = viewDirection
            translationPosition = viewForeground.width.toFloat() * openingDim

            if (viewDirection == ViewMoveDirection.EndToStart) {
                translationPosition *= -1
            }
        } else {
            duration = closeDuration
            currentState = ViewState.Closed
            viewCurrentDirection = ViewMoveDirection.No
            translationPosition = 0f
            currentElevation = 0f
        }

        startX = 0f
        startY = 0f
        if (withAnimation) {
            valueAnimator?.cancel()
            valueAnimator = ValueAnimator.ofFloat(viewForeground.x, translationPosition).apply {
                this.duration = duration
                this.interpolator = FastOutSlowInInterpolator()
                addUpdateListener { animation ->
                    viewBackground.measure(animation.animatedValue as Float, width)
                    viewForeground.x = animation.animatedValue as Float
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        valueAnimator = animation as ValueAnimator
                        viewForeground.elevation = currentElevation
                        viewForeground.foreground = null
                        viewBackground.cornerRadius = if(removeRadiusAfterMove) {
                            0f
                        } else {
                            cornerRadius
                        }
                        viewState = currentState
                        viewDirection = viewCurrentDirection
                        viewForeground.invalidate()
                        viewStartX = viewForeground.x
                        viewBackground.measure(translationPosition, width)
                    }
                })
            }
            valueAnimator?.start()
        } else {
            viewForeground.translationX = translationPosition
            viewForeground.elevation = currentElevation
            viewForeground.foreground = null
            viewBackground.cornerRadius = 0f
            viewState = currentState
            viewDirection = viewCurrentDirection
            viewBackground.measure(translationPosition, width)
            viewForeground.invalidate()
            viewStartX = viewForeground.x
        }
    }

    private fun getDrawable(direction: ViewMoveDirection?): ShapeDrawable {
        val rect = if (direction == ViewMoveDirection.StartToEnd) {
            RoundRectShape(
                floatArrayOf(
                    cornerRadius,
                    cornerRadius,
                    0f,
                    0f,
                    0f,
                    0f,
                    cornerRadius,
                    cornerRadius
                ), null, null
            )
        } else {
            RoundRectShape(
                floatArrayOf(
                    0f,
                    0f,
                    cornerRadius,
                    cornerRadius,
                    cornerRadius,
                    cornerRadius,
                    0f,
                    0f
                ), null, null
            )
        }

        val drawable = ShapeDrawable(rect)
        drawable.paint.color = borderColor ?: Color.GRAY
        drawable.paint.style = Paint.Style.STROKE
        drawable.paint.strokeWidth = borderWidth
        drawable.intrinsicWidth = (width - drawable.paint.strokeWidth).toInt()
        drawable.intrinsicHeight = (height - drawable.paint.strokeWidth).toInt()

        return drawable
    }

    fun onBindViewHolder(position: Int) {
        itemPosition = position
        isOpen = false
    }

    override fun onViewDetachedFromWindow(p0: View?) {
        valueAnimator?.end()
        valueAnimator = null
    }

    override fun onViewAttachedToWindow(p0: View?) { }
}