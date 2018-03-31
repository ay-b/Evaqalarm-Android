package com.speind.evaqalarm;

import com.google.android.gms.maps.SupportMapFragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class EvaqAlarmSupportMapFragment extends SupportMapFragment {
	  private View mOriginalContentView;
	  private TouchableWrapper mTouchView;   

	  private OnTouchListener onTouchListener=null;
	  
	  public class TouchableWrapper extends FrameLayout {

		  public TouchableWrapper(Context context) {
		    super(context);
		  }

		  @Override
		  public boolean dispatchTouchEvent(MotionEvent event) {
			if (onTouchListener!=null) onTouchListener.onTouch(this, event);
		    return mOriginalContentView.dispatchTouchEvent(event);
		  }
		}
	  
	  @Override
	  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
	    mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);    
	    mTouchView = new TouchableWrapper(getActivity());
	    mTouchView.addView(mOriginalContentView);
	    return mTouchView;
	  }

	  @Override
	  public View getView() {
	    return mOriginalContentView;
	  }
	  
	  public void setOnTouchListener(OnTouchListener otl) {
		  onTouchListener=otl;
	  }
	}
