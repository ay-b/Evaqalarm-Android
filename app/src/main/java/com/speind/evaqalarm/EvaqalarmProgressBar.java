package com.speind.evaqalarm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.ProgressBar;

public class EvaqalarmProgressBar extends ProgressBar {
	private int alertDelay=3;
	private boolean checked=false;
	private boolean alarm=false;
	
	private Bitmap checkedImage=null;
	private Bitmap normalImage=null;
	private Bitmap alertImage=null;
			
	private Animation sendAlertAnimation=null;
	private Animation alertAnimation=null;
	private boolean animate=false;
	private Vibrator vibrator = null;
	private long[] alertPattern = {0, 200, 200};
	
	private float startX=0;
	private float startY=0;
	
	private View wrap=null;
	
	public static interface  EvaqalarmProgressBarEventListener {
		public void onPressTimer();
		public void onClick();
	}
	
	private EvaqalarmProgressBarEventListener listener=null;
	
	Handler handler=new Handler();
	long timerStart=0;
	
	Runnable animateWaitRunnable=new Runnable(){		
		@Override
		public void run() {
			handler.removeCallbacks(animateWaitRunnable);
			if (timerStart<=0) {
				timerStart=System.currentTimeMillis();
				setProgress(0);
			}
			float newProgress=(System.currentTimeMillis()-timerStart)*getMax()/(alertDelay*1000);
				setProgress((int)newProgress);
			if (newProgress>=getMax()) {
				
				if (sendAlertAnimation!=null) startAnimation(sendAlertAnimation);
					
				timerStart=0;
				if (listener!=null) listener.onPressTimer();
			} else {
				handler.postDelayed(animateWaitRunnable, 10);
			}
		}
	};
	
	public EvaqalarmProgressBar(Context context) {
		super(context);
	}
	
	public EvaqalarmProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EvaqalarmProgressBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		checkedImage=BitmapFactory.decodeResource(getResources(), R.drawable.main_button_parking);
		normalImage=BitmapFactory.decodeResource(getResources(), R.drawable.main_button_not_active);
		alertImage=BitmapFactory.decodeResource(getResources(), R.drawable.main_button_alert);		
		
		vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		
		sendAlertAnimation = AnimationUtils.loadAnimation(context, R.anim.main_button_alarm);
		sendAlertAnimation.setAnimationListener(new AnimationListener(){
			@Override
			public void onAnimationStart(Animation animation) {
				animate=true;
				if (vibrator!=null) vibrator.vibrate(alertPattern, 0);
			}
			@Override
			public void onAnimationEnd(Animation animation) {
				animate=false;
				if (vibrator!=null) vibrator.cancel();
				setProgress(0);
			}
			@Override
			public void onAnimationRepeat(Animation animation) {}
		});

		alertAnimation = AnimationUtils.loadAnimation(context, R.anim.main_button_alarm_inf);
		alertAnimation.setAnimationListener(new AnimationListener(){
			@Override
			public void onAnimationStart(Animation animation) {
			}
			@Override
			public void onAnimationEnd(Animation animation) {
				setProgress(0);
			}
			@Override
			public void onAnimationRepeat(Animation animation) {}
		});
		/*
		this.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (alarm) {
					return false;
				} else {	
					if (event.getAction()==MotionEvent.ACTION_UP) {
						handler.removeCallbacks(animateWaitRunnable);
						if (timerStart==-1) {
							setChecked(!checked);
							if (listener!=null) listener.onClick();
						}
						timerStart=0;
						if (!animate) setProgress(0);
					} else if (event.getAction()==MotionEvent.ACTION_DOWN) {
						clearAnimation();
						timerStart=-1;
						handler.postDelayed(animateWaitRunnable, 200);
					}
				}
				if (wrap!=null) wrap.onTouchEvent(event);
				return true;
			}	
		});
		*/
	}

	
	@Override
	protected synchronized void onDraw(Canvas canvas) {
		//super.onDraw(canvas);
		Bitmap image = null;
		if (checked) image=checkedImage;
		else image=normalImage;
		Paint paint = new Paint();    
		paint.setAlpha(90);  
		canvas.drawBitmap(image, (getWidth()-image.getWidth())/2, (getHeight()-image.getHeight())/2, paint);
		
		int progress=this.getProgress();
		
		float angle=360*progress/getMax();
		if (progress>0||animate) {
			int l=Math.max(alertImage.getWidth(), alertImage.getHeight());
			Bitmap result = Bitmap.createBitmap(l, l, Bitmap.Config.ARGB_8888);
			Canvas resultCanvas = new Canvas(result);
			resultCanvas.drawBitmap(alertImage, 0, 0, null);
			if (angle<360) {
				Bitmap cliper = Bitmap.createBitmap(l,l,Bitmap.Config.ARGB_8888);
				Canvas cliperCanvas = new Canvas(cliper);
				Paint xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
				xferPaint.setColor(Color.RED);
				xferPaint.setStyle(Style.FILL);
				cliperCanvas.drawArc(new RectF(0,0,l,l), -90, angle, true, xferPaint);
				xferPaint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
				resultCanvas.drawBitmap(cliper, 0, 0, xferPaint);			
			}
			
			canvas.drawBitmap(result, (getWidth()-image.getWidth())/2, (getHeight()-image.getHeight())/2, null);
			
		}
		
	}

	public void setChecked(boolean c) {
		if (checked!=c) {
			checked=c;
			invalidate();
		}
	}

	public boolean getChecked() {
		return checked;
	}
	
	public void setEventListener(EvaqalarmProgressBarEventListener l) {
		listener=l;
	}
	
	public void setAlarm(boolean a) {
		handler.removeCallbacks(animateWaitRunnable);
		if (vibrator!=null) vibrator.cancel();
		if (alarm!=a) {
			alarm=a;
			if (alarm) {
				if (alertAnimation!=null) {
					setProgress(this.getMax());
					startAnimation(alertAnimation);
				}
			} else {
				clearAnimation();
				animate=false;
				setProgress(0);
			}
		}
	}
	
	public void stop() {
		if (vibrator!=null) vibrator.cancel();
	}
	
	public void setViewTouchEvent(View v) {
		wrap=v;
	}
	
	public boolean processOnTouch(MotionEvent event) {
		if (alarm) {
			return false;
		} else {	
			if (event.getAction()==MotionEvent.ACTION_UP) {
				handler.removeCallbacks(animateWaitRunnable);
				if (timerStart==-1) {
					setChecked(!checked);
					if (listener!=null) listener.onClick();
				}
				timerStart=0;
				if (!animate) setProgress(0);
			} else if (event.getAction()==MotionEvent.ACTION_DOWN) {
				startX=event.getX();
				startY=event.getY();
				clearAnimation();
				timerStart=-1;
				handler.postDelayed(animateWaitRunnable, 200);
			} else if (event.getAction()==MotionEvent.ACTION_MOVE) {
				if (Math.abs(event.getX()-startX)>20||Math.abs(event.getY()-startY)>20) {
					handler.removeCallbacks(animateWaitRunnable);
					timerStart=0;
					if (!animate) setProgress(0);
				}
			}
		}
		if (wrap!=null) wrap.onTouchEvent(event);
		return true;
	}
}
