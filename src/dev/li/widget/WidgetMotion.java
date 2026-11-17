package dev.li.widget;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AnimationUtils;

final class WidgetMotion {
  // Two 900 ms pulses, matching widget_charge_pulse.xml.
  static final int CHARGE_PULSE_MILLIS = 1800;

  static boolean enabled(WidgetStyle style) {
    return !style.motion.equals("off") && ValueAnimator.areAnimatorsEnabled();
  }

  static void preview(View widget, WidgetStyle style) {
    if (!enabled(style)) return;
    View avatar = widget.findViewById(R.id.animated_avatar);
    boolean glide = style.motion.equals("avatar") && avatar != null && avatar.isShown();
    View animated = glide ? avatar : widget.findViewById(R.id.status_text);
    if (animated != null && animated.isShown())
      animated.startAnimation(AnimationUtils.loadAnimation(widget.getContext(),
          glide ? R.anim.widget_avatar_glide : R.anim.widget_status_fade));
  }
}
