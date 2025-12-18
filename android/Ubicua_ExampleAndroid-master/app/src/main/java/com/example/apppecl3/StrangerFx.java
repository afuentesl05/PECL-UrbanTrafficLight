package com.example.apppecl3;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

public final class StrangerFx {

    private static final String TAG_FOG_1 = "stranger_fog_1";
    private static final String TAG_FOG_2 = "stranger_fog_2";

    private StrangerFx() {}

    public static void attachIfEnabled(Activity activity) {
        if (!ThemeManager.isStrangerEnabled(activity)) return;

        ViewGroup root = activity.findViewById(R.id.main);
        if (root == null) return;
        if (root.findViewWithTag(TAG_FOG_1) != null) return;

        ImageView fog1 = buildFog(activity, TAG_FOG_1, 0.12f);
        ImageView fog2 = buildFog(activity, TAG_FOG_2, 0.08f);

        root.addView(fog1);
        root.addView(fog2);

        root.post(() -> startFogAnim(root, fog1, 0f, 18000));
        root.post(() -> startFogAnim(root, fog2, root.getWidth() * 0.5f, 24000));
    }

    private static ImageView buildFog(Activity a, String tag, float alpha) {
        ImageView fog = new ImageView(a);
        fog.setTag(tag);
        fog.setImageResource(R.drawable.fog_overlay);
        fog.setScaleType(ImageView.ScaleType.FIT_XY);
        fog.setAlpha(alpha);

        fog.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        fog.setClickable(false);
        fog.setFocusable(false);
        fog.setEnabled(false);

        return fog;
    }

    private static void startFogAnim(ViewGroup root, ImageView fog, float startOffset, long duration) {
        float w = root.getWidth();
        if (w <= 0) return;

        fog.setTranslationX(-w + startOffset);

        ObjectAnimator move = ObjectAnimator.ofFloat(fog, "translationX", -w + startOffset, w + startOffset);
        move.setDuration(duration);
        move.setInterpolator(new LinearInterpolator());
        move.setRepeatCount(ValueAnimator.INFINITE);
        move.setRepeatMode(ValueAnimator.RESTART);

        ObjectAnimator pulse = ObjectAnimator.ofFloat(fog, "alpha", fog.getAlpha(), fog.getAlpha() + 0.07f);
        pulse.setDuration(3000);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);

        move.start();
        pulse.start();
    }
}
