package neon.com.br.labarcore

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.View
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ux.ArFragment

class PointerDrawable(
    private val contentView: View,
    private val fragment: ArFragment
) : Drawable() {

    private val paint = Paint()

    private var isTracking: Boolean = false
    private var isHitting: Boolean = false

    override fun draw(canvas: Canvas) {
        canvas.clipBounds
        val cx = canvas.clipBounds.width() / 2f
        val cy = canvas.clipBounds.height() / 2f
        if (isHitting) {
            paint.color = Color.GREEN
            canvas.drawCircle(cx, cy, 10f, paint)
        } else {
            paint.color = Color.GRAY
            canvas.drawText("X", cx, cy, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOpacity(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun onUpdate() {
        val trackingChanged = updateTracking()
        if (trackingChanged) {
            if (isTracking) {
                contentView.overlay.add(this)
            } else {
                contentView.overlay.remove(this)
            }
            contentView.invalidate()
        }

        if (isTracking) {
            val hitTestChanged = updateHitTest()
            if (hitTestChanged) {
                contentView.invalidate()
            }
        }
    }

    private fun updateTracking(): Boolean {
        val frame = fragment.arSceneView.arFrame
        val wasTracking = isTracking
        isTracking = frame?.camera?.trackingState === TrackingState.TRACKING
        return isTracking != wasTracking
    }

    private fun updateHitTest(): Boolean {
        val frame = fragment.arSceneView.arFrame
        val (x, y) = getScreenCenter()
        val hits: List<HitResult>
        val wasHitting = isHitting
        isHitting = false
        if (frame != null) {
            hits = frame.hitTest(x, y)
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    isHitting = true
                    break
                }
            }
        }
        return wasHitting != isHitting
    }

    fun getScreenCenter(): Pair<Float, Float> {
        return Pair(contentView.width / 2f, contentView.height / 2f)
    }
}