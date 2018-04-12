package com.gmail.pingkiuho.intermediateprogressbardrawable.progressbar;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.TypedValue;
import android.view.animation.LinearInterpolator;

import com.gmail.pingkiuho.intermediateprogressbardrawable.R;

/**
 * Created by Brian Ho on 9/4/2018.
 * Referencing https://github.com/castorflex/SmoothProgressBar
 */

public class IntermediateProgressBarDrawable extends Drawable implements Animatable {
    public static final String TAG = IntermediateProgressBarDrawable.class.getSimpleName();

    public interface OnStopListener {
        void onStop();
    }

    private boolean mRunning;
    private RectF mRectF = new RectF();
    private Paint mPaint;

    private float mCurrentSweepAngle;
    private float mCurrentRotation;
    private float mCurrentRotationOffset;
    private float mCurrentEndRatio = 1;
    private boolean mSweepAppearing = true;
    private boolean mFirstSweepAnimation;

    private ValueAnimator mRotationAnimator;
    private ValueAnimator mSweepAppearingAnimator;
    private ValueAnimator mSweepDisappearingAnimator;
    private ValueAnimator mEndAnimator;

    private OnStopListener mOnStopListener;

    private boolean mSweep;
    private boolean mRotation;
    private long mRotationDuration;
    private long mSweepDuration;
    private float mRotationSpeed;
    private float mSweepSpeed;
    private float mMaxSweepAngle;
    private float mAdjustedMaxSweepAngle;
    private float mMinSweepAngle;
    private float mAdjustedMinSweepAngle;
    private float mStartAtDegree;
    private float mStrokeWidthInPx = 0f;
    private Paint.Cap mStrokeCap = Paint.Cap.ROUND;
    private int[] mColors;
    private boolean mGradient;
    private int mSize;


    public static Builder builder(Context context) {
        return new Builder(context);
    }

    private IntermediateProgressBarDrawable(Context context,
                                            boolean sweep,
                                            boolean rotate,
                                            long rotationDuration,
                                            long sweepDuration,
                                            float rotationSpeed,
                                            float sweepSpeed,
                                            float maxSweepAngle,
                                            float minSweepAngle,
                                            float startAtDegree,
                                            float strokeWidthInPx,
                                            Paint.Cap strokeCap,
                                            int[] colors,
                                            boolean gradient,
                                            int size) {
        mSweep = sweep;
        mRotation = rotate;
        mRotationDuration = rotationDuration;
        mSweepDuration = sweepDuration;
        mRotationSpeed = rotationSpeed;
        mSweepSpeed = sweepSpeed;
        mMaxSweepAngle = maxSweepAngle;
        mAdjustedMaxSweepAngle = maxSweepAngle;
        mMinSweepAngle = minSweepAngle;
        mAdjustedMinSweepAngle = minSweepAngle;
        mStartAtDegree = startAtDegree;
        mStrokeWidthInPx = strokeWidthInPx;
        mStrokeCap = strokeCap;
        mGradient = gradient;
        mSize = size;

        if (gradient && colors.length > 1) {
            // The order of colors need to be reversed since the end of the gradient will be the "head" of stroke
            mColors = new int[colors.length];
            for (int i = 0; i < colors.length; i++) {
                mColors[i] = colors[colors.length - 1 - i];
            }
        } else if (colors == null) {
            // Apply app theme primary color if there is no color specified
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
            mColors = new int[]{typedValue.data};
            mGradient = false;
        } else {
            mColors = colors;
        }

        mPaint = new Paint();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidthInPx);
        mPaint.setStrokeCap(mStrokeCap);

        setCallback(new Callback() {
            @Override
            public void invalidateDrawable(@NonNull Drawable who) {
                invalidateSelf();
            }

            @Override
            public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
                scheduleSelf(what, when);
            }

            @Override
            public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
                unscheduleSelf(what);
            }
        });

        setupAnimations();
    }

    @Override
    public void start() {
        mRunning = true;
        reset();
        if (mRotation) {
            mRotationAnimator.start();
        }
        if (mSweep) {
            mSweepAppearingAnimator.start();
        }
    }

    @Override
    public void stop() {
        mRunning = false;
        reset();
        invalidateSelf();
    }

    public void progressiveStop(@Nullable OnStopListener listener) {
        if (!isRunning() && mEndAnimator.isRunning()) {
            return;
        } else if (!mSweep) {
            stop();
            if (listener != null) {
                listener.onStop();
            }
            return;
        }

        mOnStopListener = listener;

        mEndAnimator.addListener(new SimpleAnimatorListener() {
            @Override
            protected void onPreAnimationEnd(Animator animation) {
                mEndAnimator.removeListener(this);
                if (isStartedAndNotCancelled()) {
                    mCurrentEndRatio = 0f;
                    stop();
                    if (mOnStopListener != null) {
                        mOnStopListener.onStop();
                        mOnStopListener = null;
                    }
                }
            }
        });
        mEndAnimator.start();
    }

    private void reset() {
        mSweepAppearing = true;
        mFirstSweepAnimation = true;
        mCurrentEndRatio = 1f;
        mCurrentRotation = mStartAtDegree;
        mCurrentRotationOffset = 0f;
        if (!mSweep) {
            mCurrentSweepAngle = mAdjustedMaxSweepAngle;
        }

        mEndAnimator.cancel();
        if (mRotation) {
            mRotationAnimator.cancel();
        }
        if (mSweep) {
            mSweepAppearingAnimator.cancel();
            mSweepDisappearingAnimator.cancel();
        }
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (isRunning()) {
            float nextStartAngle = mCurrentRotation - mCurrentRotationOffset;
            float nextSweepAngle = mSweep ? mCurrentSweepAngle : mAdjustedMaxSweepAngle;

            if (mSweep) {
                if (!mSweepAppearing) {
                    // if sweep is disappearing, reverse the sweep direction
                    nextStartAngle = nextStartAngle + (360f - nextSweepAngle);
                }
                nextStartAngle %= 360;
            }

            if (mCurrentEndRatio < 1f) {
                float newSweepAngle = nextSweepAngle * mCurrentEndRatio;
                nextStartAngle = (nextStartAngle + (nextSweepAngle - newSweepAngle)) % 360;
                nextSweepAngle = newSweepAngle;
            }

            if (mGradient) {
                mPaint.setShader(calculateGradientShader(nextStartAngle, nextSweepAngle));
            }

            canvas.drawArc(mRectF, nextStartAngle, nextSweepAngle, false, mPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        mRectF.left = bounds.left + mPaint.getStrokeWidth() / 2;
        mRectF.right = bounds.right - mPaint.getStrokeWidth() / 2;
        mRectF.top = bounds.top + mPaint.getStrokeWidth() / 2;
        mRectF.bottom = bounds.bottom - mPaint.getStrokeWidth() / 2;

        float width = mRectF.right - mRectF.left;
        float height = mRectF.bottom - mRectF.top;
        float trimWidth = 0;
        float trimHeight = 0;

        if (mSize > 0 && (width > mSize || height > mSize)) {
            trimWidth = width - mSize;
            trimHeight = height - mSize;
        }

        if (width > height) {
            trimWidth = width - height;
        } else if (height > width) {
            trimHeight = height - width;

        }
        mRectF.left += trimWidth / 2;
        mRectF.right -= trimWidth / 2;
        mRectF.top += trimHeight / 2;
        mRectF.bottom -= trimHeight / 2;


        adjustSweepAngleForStroke();

        if (mGradient) {
            mPaint.setShader(calculateGradientShader(mCurrentRotation, mCurrentSweepAngle));
        }
    }

    private void setupAnimations() {
        if (mRotation) {
            mRotationAnimator = ValueAnimator.ofFloat(0f, 360f);
            mRotationAnimator.setInterpolator(new LinearInterpolator());
            mRotationAnimator.setDuration((long) (mRotationDuration / mRotationSpeed));
            mRotationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentRotation = mStartAtDegree + animation.getAnimatedFraction() * 360f;
                    invalidateSelf();
                }
            });
            mRotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mRotationAnimator.setRepeatMode(ValueAnimator.RESTART);
        }

        if (mSweep) {
            mSweepAppearingAnimator = ValueAnimator.ofFloat(mAdjustedMinSweepAngle, mAdjustedMaxSweepAngle);
            mSweepAppearingAnimator.setInterpolator(new FastOutSlowInInterpolator());
            mSweepAppearingAnimator.setDuration((long) (mSweepDuration / mSweepSpeed));
            mSweepAppearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float newAngle;
                    if (mFirstSweepAnimation) {
                        newAngle = animation.getAnimatedFraction() * mAdjustedMaxSweepAngle;
                    } else {
                        newAngle = mAdjustedMinSweepAngle + animation.getAnimatedFraction() * (mAdjustedMaxSweepAngle - mAdjustedMinSweepAngle);
                    }
                    mCurrentSweepAngle = newAngle;
                    invalidateSelf();
                }
            });
            mSweepAppearingAnimator.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation, boolean isReverse) {
                    super.onAnimationStart(animation);
                    mSweepAppearing = true;
                }

                @Override
                protected void onPreAnimationEnd(Animator animation) {
                    if (isStartedAndNotCancelled()) {
                        mFirstSweepAnimation = false;
                        mSweepAppearing = false;
                        mCurrentRotationOffset = mCurrentRotationOffset + (360f - mAdjustedMaxSweepAngle);
                        if (mCurrentRotationOffset > 360f) {
                            mCurrentRotationOffset -= 360f;
                        }
                        mSweepDisappearingAnimator.start();
                    }

                }
            });

            mSweepDisappearingAnimator = ValueAnimator.ofFloat(mAdjustedMaxSweepAngle, mAdjustedMinSweepAngle);
            mSweepDisappearingAnimator.setInterpolator(new FastOutSlowInInterpolator());
            mSweepDisappearingAnimator.setDuration((long) (mSweepDuration / mSweepSpeed));
            mSweepDisappearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentSweepAngle = mAdjustedMaxSweepAngle - animation.getAnimatedFraction() * (mAdjustedMaxSweepAngle - mAdjustedMinSweepAngle);
                    invalidateSelf();
                }
            });
            mSweepDisappearingAnimator.addListener(new SimpleAnimatorListener() {
                @Override
                protected void onPreAnimationEnd(Animator animation) {
                    if (isStartedAndNotCancelled()) {
                        mSweepAppearing = true;
                        mCurrentRotationOffset += mAdjustedMinSweepAngle;
                        mSweepAppearingAnimator.start();
                    }
                }
            });
        }

        mEndAnimator = ValueAnimator.ofFloat(1f, 0f);
        mEndAnimator.setInterpolator(new LinearInterpolator());
        mEndAnimator.setDuration((long) (mSweepDuration * (1 + mCurrentSweepAngle / 360f) / mSweepSpeed));
        mEndAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurrentEndRatio = 1f - animation.getAnimatedFraction();
            }
        });
    }

    /**
     * Since StrokeCap style, ROUND and SQUARE, will added extra stroke length, stroke ends may overlapped
     * Also, they need at least 1px min sweep in order to prevent flash problem when sweep disappear
     */
    private void adjustSweepAngleForStroke() {
        /*
        adjust min/max sweep angle if matched below conditions
        - Cap type is ROUND or SQUARE (not BUTT)
            - Stroke head will overlap the stroke end when max sweep (Max draw angle + stroke width > 360 degrees)
            - Enabled gradient
                - Min sweep < two stroke width
         */
        if (!mStrokeCap.equals(Paint.Cap.BUTT)) {
            if (mGradient) {
                double strokeWidthAngle = Math.toDegrees(Math.atan2(mStrokeWidthInPx / 2, (mRectF.right - mRectF.left) / 2)) * 2;
                if (mMaxSweepAngle + mMinSweepAngle + strokeWidthAngle > 360) {
                    mAdjustedMinSweepAngle = (float) (mMinSweepAngle + strokeWidthAngle);
                    mAdjustedMaxSweepAngle = (float) (mMaxSweepAngle - strokeWidthAngle);
                }
                if (mMinSweepAngle < strokeWidthAngle * 2) {
                    mAdjustedMinSweepAngle = (float) (strokeWidthAngle * 2);
                }
            }

            // make sure at least 1px for min sweep angle
            if (mAdjustedMinSweepAngle < 1f) {
                mAdjustedMinSweepAngle = 1f;
            }
        }
    }

    private SweepGradient calculateGradientShader(float startAngle, float sweepAngle) {
        SweepGradient newShader;

        /*
        For some reason, gradient's color positions need to have 0f, 1f position in previewer (not sure in actually device)
        But the belows calculate will ignore this two position in order to make even color section
        Thus, duplicating the first and last color can fix this issues
         */
        int[] colorListForShader = new int[mColors.length + 2];
        colorListForShader[0] = mColors[0];
        colorListForShader[colorListForShader.length - 1] = mColors[mColors.length - 1];
        System.arraycopy(mColors, 0, colorListForShader, 1, mColors.length);

        float[] positions = new float[colorListForShader.length];
        float nonSweepSpacesOffset = (360f - sweepAngle) / 2;
        float degreesPerColor = sweepAngle / mColors.length;

        positions[0] = 0f;
        positions[positions.length - 1] = 1f;
        for (int i = 0; i < colorListForShader.length - 2; i++) {
            positions[i + 1] = (degreesPerColor * (i + 0.5f) + nonSweepSpacesOffset) / 360f;
        }

        newShader = new SweepGradient(mRectF.centerX(), mRectF.centerY(), colorListForShader, positions);

        Matrix gradientMatrix = new Matrix();
        gradientMatrix.setRotate(startAngle + sweepAngle + nonSweepSpacesOffset, mRectF.centerX(), mRectF.centerY());
        newShader.setLocalMatrix(gradientMatrix);
        return newShader;
    }

    abstract class SimpleAnimatorListener implements Animator.AnimatorListener {
        private boolean mStarted = false;
        private boolean mCancelled = false;

        @Override
        @CallSuper
        public void onAnimationStart(Animator animation) {
            mCancelled = false;
            mStarted = true;
        }

        @Override
        public final void onAnimationEnd(Animator animation) {
            onPreAnimationEnd(animation);
            mStarted = false;
        }

        protected void onPreAnimationEnd(Animator animation) {
        }

        @Override
        @CallSuper
        public void onAnimationCancel(Animator animation) {
            mCancelled = true;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        public boolean isStartedAndNotCancelled() {
            return mStarted && !mCancelled;
        }
    }

    public static class Builder {
        private Context context;
        private boolean rotation = true;
        private boolean sweep = true;
        private long rotationDuration = 3000;
        private long sweepDuration = 1200;
        private float rotationSpeed = 1f;
        private float sweepSpeed = 1f;
        private float minSweepAngle = 25f;
        private float maxSweepAngle = 335f;
        private float startAtDegree = 0f;
        private float strokeWidthInPx = 0f;
        private Paint.Cap strokeCap = Paint.Cap.ROUND;
        private int[] colors;
        private boolean gradient;
        private int size;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder rotation(boolean enable) {
            rotation = enable;
            return this;
        }

        public Builder sweep(boolean enable) {
            sweep = enable;
            return this;
        }

        public Builder rotationDuration(long duration) {
            rotationDuration = duration;
            return this;
        }

        public Builder sweepDuration(long duration) {
            sweepDuration = duration;
            return this;
        }

        public Builder rotationSpeed(float speed) {
            rotationSpeed = speed;
            return this;
        }

        public Builder sweepSpeed(float speed) {
            sweepSpeed = speed;
            return this;
        }

        public Builder minSweepAngle(float angle) {
            if (angle < 0f) {
                minSweepAngle = 0f;
            } else if (angle > 360f) {
                minSweepAngle = 360f;
            } else {
                minSweepAngle = angle;
            }
            return this;
        }

        public Builder maxSweepAngle(float angle) {
            if (angle < 0f) {
                maxSweepAngle = 0f;
            } else if (angle > 360f) {
                maxSweepAngle = 360f;
            } else {
                maxSweepAngle = angle;
            }
            return this;
        }

        public Builder startAtDegree(float degree) {
            if (degree < 0f) {
                startAtDegree = 360f - degree;
            } else if (degree > 360f) {
                startAtDegree = degree - 360f;
            } else {
                startAtDegree = degree;
            }
            return this;
        }

        public Builder strokeWidth(float px) {
            strokeWidthInPx = px;
            return this;
        }

        public Builder strokeCap(Paint.Cap capStyle) {
            strokeCap = capStyle;
            return this;
        }

        public Builder color(int color) {
            colors = new int[]{color};
            gradient = false;
            return this;
        }

        public Builder gradient(int firstColor, int secondColor, int... otherColors) {
            colors = new int[otherColors.length + 2];
            colors[0] = firstColor;
            colors[1] = secondColor;
            for (int i = 0; i < otherColors.length; i++) {
                colors[i + 2] = otherColors[i];
            }
            gradient = true;
            return this;
        }

        public Builder size(int px) {
            size = px;
            return this;
        }

        public IntermediateProgressBarDrawable build() {
            return new IntermediateProgressBarDrawable(
                    context,
                    sweep,
                    rotation,
                    rotationDuration,
                    sweepDuration,
                    rotationSpeed,
                    sweepSpeed,
                    maxSweepAngle,
                    minSweepAngle,
                    startAtDegree,
                    strokeWidthInPx,
                    strokeCap,
                    colors,
                    gradient,
                    size);
        }
    }
}
