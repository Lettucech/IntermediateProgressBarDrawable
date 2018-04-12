package com.gmail.pingkiuho.intermediateprogressbardrawable.util;

import android.content.Context;
import android.util.SizeF;
import android.widget.ImageView;

/**
 * Created by Brian Ho on 19/9/2017.
 */

public class DimensionUtil {
    public static float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    public static float pxToDp(Context context, float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }
}