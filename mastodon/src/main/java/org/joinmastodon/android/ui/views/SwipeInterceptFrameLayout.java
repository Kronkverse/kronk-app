package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * A FrameLayout that detects horizontal swipe gestures even when children consume touch events.
 * Reports swipe direction via a callback without blocking vertical scrolling in children.
 */
public class SwipeInterceptFrameLayout extends FrameLayout{
	private float startX, startY;
	private boolean tracking;
	private boolean swipeHandled;
	private OnSwipeListener swipeListener;

	private static final float SWIPE_MIN_DISTANCE_DP=80f;
	private static final float MAX_VERTICAL_RATIO=1.5f;

	public interface OnSwipeListener{
		void onSwipeLeft();
		void onSwipeRight();
	}

	public SwipeInterceptFrameLayout(Context context){
		super(context);
	}

	public void setOnSwipeListener(OnSwipeListener listener){
		this.swipeListener=listener;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev){
		float density=getResources().getDisplayMetrics().density;
		float minDistance=SWIPE_MIN_DISTANCE_DP*density;

		switch(ev.getActionMasked()){
			case MotionEvent.ACTION_DOWN:
				startX=ev.getX();
				startY=ev.getY();
				tracking=true;
				swipeHandled=false;
				break;
			case MotionEvent.ACTION_MOVE:
				if(tracking && !swipeHandled){
					float dx=ev.getX()-startX;
					float dy=ev.getY()-startY;
					if(Math.abs(dx)>minDistance && Math.abs(dx)>Math.abs(dy)*MAX_VERTICAL_RATIO){
						swipeHandled=true;
						tracking=false;
						if(swipeListener!=null){
							if(dx>0){
								swipeListener.onSwipeRight();
							}else{
								swipeListener.onSwipeLeft();
							}
						}
						return false;
					}
					if(Math.abs(dy)>minDistance*0.5f && Math.abs(dy)>Math.abs(dx)){
						tracking=false;
					}
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				tracking=false;
				break;
		}
		return false;
	}

	/**
	 * Animate a slide transition between two child views.
	 * @param outgoing The view sliding out
	 * @param incoming The view sliding in
	 * @param slideLeft True if content slides left (going to next tab), false for right
	 * @param onComplete Called when animation finishes
	 */
	public void animateSlide(android.view.View outgoing, android.view.View incoming, boolean slideLeft, Runnable onComplete){
		int width=getWidth();
		if(width==0) width=getResources().getDisplayMetrics().widthPixels;

		incoming.setVisibility(VISIBLE);
		incoming.setTranslationX(slideLeft ? width : -width);
		incoming.setAlpha(1f);

		int duration=280;
		DecelerateInterpolator interpolator=new DecelerateInterpolator(1.5f);

		outgoing.animate()
				.translationX(slideLeft ? -width*0.3f : width*0.3f)
				.alpha(0.5f)
				.setDuration(duration)
				.setInterpolator(interpolator)
				.withEndAction(()->{
					outgoing.setTranslationX(0);
					outgoing.setAlpha(1f);
					outgoing.setVisibility(GONE);
					if(onComplete!=null) onComplete.run();
				})
				.start();

		incoming.animate()
				.translationX(0)
				.setDuration(duration)
				.setInterpolator(interpolator)
				.start();
	}
}
