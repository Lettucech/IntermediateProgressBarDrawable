package com.gmail.pingkiuho.intermediateprogressbardrawable.progressbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.gmail.pingkiuho.intermediateprogressbardrawable.R;
import com.gmail.pingkiuho.intermediateprogressbardrawable.util.DimensionUtil;

/**
 * Created by Brian Ho on 28/3/2018.
 */

public class IntermediateProgressBar extends ProgressBar {
    public static final String TAG = IntermediateProgressBar.class.getSimpleName();

    private IntermediateProgressBarDrawable mDrawable;

    public IntermediateProgressBar(Context context) {
        this(context, null);
    }

    public IntermediateProgressBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.intermediateProgressBarStyle);
    }

    public IntermediateProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray styleArray = context.obtainStyledAttributes(attrs, R.styleable.IntermediateProgressBar, defStyleAttr, R.style.IntermediateProgressBarStyle);

        IntermediateProgressBarDrawable.Builder builder = new IntermediateProgressBarDrawable.Builder(context);

        int color = styleArray.getColor(R.styleable.IntermediateProgressBar_ipb_color, R.attr.colorPrimary);
        CharSequence[] gradient = styleArray.getTextArray(R.styleable.IntermediateProgressBar_ipb_gradientColorArray);
        if (gradient != null) {
            try {
                if (gradient.length > 1) {
                    int firstColor = Color.parseColor(gradient[0].toString());
                    int secondColor = Color.parseColor(gradient[1].toString());
                    int[] otherColors = new int[gradient.length - 2];
                    for (int i = 0; i < gradient.length - 2; i++) {
                        otherColors[i] = Color.parseColor(gradient[i + 2].toString());
                    }
                    builder.gradient(firstColor, secondColor, otherColors);
                } else {
                    builder.color(Color.parseColor(gradient[0].toString()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Invalid color code");
            }
        } else {
            builder.color(color);
        }

        builder.sweepSpeed(styleArray.getFloat(R.styleable.IntermediateProgressBar_ipb_sweepSpeed, 1f));
        builder.rotationSpeed(styleArray.getFloat(R.styleable.IntermediateProgressBar_ipb_rotationSpeed, 1f));
        builder.minSweepAngle(styleArray.getFloat(R.styleable.IntermediateProgressBar_ipb_minSweepAngle, 0f));
        builder.maxSweepAngle(styleArray.getFloat(R.styleable.IntermediateProgressBar_ipb_maxSweepAngle, 360f));
        builder.startAtDegree(styleArray.getFloat(R.styleable.IntermediateProgressBar_ipb_startAngle, 0f));
        builder.sweep(styleArray.getBoolean(R.styleable.IntermediateProgressBar_ipb_sweep, true));
        builder.strokeWidth(styleArray.getDimensionPixelSize(R.styleable.IntermediateProgressBar_ipb_strokeWidth, (int) DimensionUtil.dpToPx(context, 8)));
        switch (styleArray.getInt(R.styleable.IntermediateProgressBar_ipb_strokeCap, 0)) {
            case 1:
                builder.strokeCap(Paint.Cap.ROUND);
                break;
            case 2:
                builder.strokeCap(Paint.Cap.SQUARE);
                break;
            default:
                builder.strokeCap(Paint.Cap.BUTT);
        }
        builder.size(styleArray.getDimensionPixelSize(R.styleable.IntermediateProgressBar_ipb_size, 72));

        styleArray.recycle();

        mDrawable = builder.build();
        setIndeterminateDrawable(mDrawable);
        setIndeterminate(true);
    }

    public void start() {
        mDrawable.start();
    }

    public void stop() {
        mDrawable.stop();
    }

    public void progressiveStop() {
        mDrawable.progressiveStop(null);
    }
}
