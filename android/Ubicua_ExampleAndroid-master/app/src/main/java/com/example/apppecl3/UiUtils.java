package com.example.apppecl3;

import android.view.View;

public final class UiUtils {

    private UiUtils() { }

    public static void setAnimatedClick(View view, View.OnClickListener realListener) {
        view.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(70)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(70)
                                .withEndAction(() -> {
                                    if (realListener != null) {
                                        realListener.onClick(v);
                                    }
                                })
                                .start();
                    })
                    .start();
        });
    }

    public static void setAnimatedClick(View view, Runnable action) {
        setAnimatedClick(view, v -> {
            if (action != null) {
                action.run();
            }
        });
    }
}
