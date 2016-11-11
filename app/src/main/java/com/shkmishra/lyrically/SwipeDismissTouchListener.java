package com.shkmishra.lyrically;

// Modified the SwipeDimissTouchListener by Roman Nurik to work for vertical dismissing


import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class SwipeDismissTouchListener implements View.OnTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    // Fixed properties
    private View mView;
    private DismissCallbacks mCallbacks;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero
    private int mViewHeight = 1; // 1 and not 0 to prevent dividing by zero

    // Transient properties
    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private int mSwipingSlop;
    private Object mToken;
    private VelocityTracker mVelocityTracker;
    private float mTranslationX;
    private float mTranslationY;


    /**
     * Constructs a new swipe-to-dismiss touch listener for the given view.
     *
     * @param view      The view to make dismissable.
     * @param token     An optional token/cookie object to be passed through to the callback.
     * @param callbacks The callback to trigger when the user has indicated that she would like to
     *                  dismiss this view.
     */
    public SwipeDismissTouchListener(View view, Object token, DismissCallbacks callbacks) {
        ViewConfiguration vc = ViewConfiguration.get(view.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = view.getContext().getResources().getInteger(R.integer.animationDuration);
        mView = view;
        mToken = token;
        mCallbacks = callbacks;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        // offset because the view is translated during swipe
        motionEvent.offsetLocation(0, mTranslationY);

        if (mViewWidth < 2) {
            mViewWidth = mView.getWidth();
            mViewHeight = mView.getHeight();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                // TODO: ensure this is a finger, and set a flag
                mDownX = motionEvent.getRawX();
                mDownY = motionEvent.getRawY();
                if (mCallbacks.canDismiss(mToken)) {
                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(motionEvent);
                }
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaY = motionEvent.getRawY() - mDownY;

                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float velocityY = mVelocityTracker.getYVelocity();
                float absVelocityX = Math.abs(velocityX);
                float absVelocityY = Math.abs(velocityY);
                boolean dismiss = false;
                boolean dismissRight = false;
                if (Math.abs(deltaY) > mViewHeight / 2 && mSwiping) {
                    dismiss = true;
                    dismissRight = deltaY > 0;
                } else if (mMinFlingVelocity <= absVelocityY && absVelocityY <= mMaxFlingVelocity
                        && absVelocityX < absVelocityY
                        && absVelocityX < absVelocityY && mSwiping) {
                    // dismiss only if flinging in the same direction as dragging
                    dismiss = (velocityY < 0) == (deltaY < 0);
                    dismissRight = mVelocityTracker.getYVelocity() > 0;
                }
                if (dismiss) {
                    performDismiss();
                } else if (mSwiping) {
                    // cancel

                    mView.animate()
                            .translationY(0)

                            .setDuration(mAnimationTime)
                            .setListener(null);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mTranslationX = 0;
                mTranslationY = 0;
                mDownX = 0;
                mDownY = 0;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                if (mVelocityTracker == null) {
                    break;
                }

                mView.animate()
                        .translationY(0)
                        .setDuration(mAnimationTime)
                        .setListener(null);
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mTranslationX = 0;
                mTranslationY = 0;
                mDownX = 0;
                mDownY = 0;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaY = motionEvent.getRawY() - mDownY;
                if (Math.abs(deltaY) > mSlop && Math.abs(deltaX) < Math.abs(deltaY) / 2) {
                    mSwiping = true;
                    mSwipingSlop = (deltaY > 0 ? mSlop : -mSlop);
                    mView.getParent().requestDisallowInterceptTouchEvent(true);

                }

                if (mSwiping && deltaY > 0) {
                    mTranslationY = deltaY;
                    mView.setTranslationY(deltaY - mSwipingSlop);
                    // TODO: use an ease-out interpolator or such
                    return true;
                }
                break;
            }
        }
        return false;
    }

    private void performDismiss() {
        // Animate the dismissed view to zero-height and then fire the dismiss callback.
        // This triggers layout on each animation frame; in the future we may want to do something
        // smarter and more performant.
        Animation animation = AnimationUtils.loadAnimation(mView.getContext(), R.anim.slide_down);
        mView.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mView.setTranslationY(0);
                mCallbacks.onDismiss(mView, mToken);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


    }

    /**
     * The callback interface used by {@link SwipeDismissTouchListener} to inform its client
     * about a successful dismissal of the view for which it was created.
     */
    public interface DismissCallbacks {
        /**
         * Called to determine whether the view can be dismissed.
         */
        boolean canDismiss(Object token);

        /**
         * Called when the user has indicated they she would like to dismiss the view.
         *
         * @param view  The originating {@link android.view.View} to be dismissed.
         * @param token The optional token passed to this object's constructor.
         */
        void onDismiss(View view, Object token);
    }
}