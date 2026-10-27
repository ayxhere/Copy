package ayx.copy.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible

object AnimationUtil {

    /** Fades a view in from alpha 0 to 1. */
    fun fadeIn(view: View, duration: Long = 300) {
        view.alpha = 0f
        view.isVisible = true
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setListener(null)
            .start()
    }

    /** Fades a view out then hides it. */
    fun fadeOut(view: View, duration: Long = 250, onEnd: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.isVisible = false
                    onEnd?.invoke()
                }
            })
            .start()
    }

    /** Quick spring-like scale bounce, used for FAB taps and card presses. */
    fun scaleBounce(view: View) {
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.5f))
                    .start()
            }
            .start()
    }

    /** Applies the staggered slide-in animation to a sequence of views (splash screen). */
    fun staggeredReveal(views: List<View>, startDelay: Long = 150, stepDelay: Long = 120) {
        views.forEachIndexed { index, view ->
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(startDelay + (index * stepDelay))
                .setDuration(400)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    fun playItemAppear(context: android.content.Context, view: View) {
        val anim = AnimationUtils.loadAnimation(context, ayx.copy.R.anim.item_appear)
        view.startAnimation(anim)
    }
}
