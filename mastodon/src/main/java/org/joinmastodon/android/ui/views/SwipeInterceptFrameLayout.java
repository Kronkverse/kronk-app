package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.view.MotionEvent;
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

	private static final float SWIPE_MIN_DISTANCE=100f;
	private static final float SWIPE_MAX_OFF_PATH=200f;

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
					// If the user is clearly swiping horizontally, intercept
					if(Math.abs(dx)>SWIPE_MIN_DISTANCE && Math.abs(dy)<SWIPE_MAX_OFF_PATH && Math.abs(dx)>Math.abs(dy)*1.5f){
						swipeHandled=true;
						tracking=false;
						if(swipeListener!=null){
							if(dx>0){
								swipeListener.onSwipeRight();
							}else{
								swipeListener.onSwipeLeft();
							}
						}
						// Don't actually intercept - we already handled it
						// Return false so children still get ACTION_CANCEL naturally
						return false;
					}
					// If clearly vertical, stop tracking
					if(Math.abs(dy)>50 && Math.abs(dy)>Math.abs(dx)){
						tracking=false;
					}
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				tracking=false;
				break;
		}
		return false; // never steal the touch sequence from children
	}
}
