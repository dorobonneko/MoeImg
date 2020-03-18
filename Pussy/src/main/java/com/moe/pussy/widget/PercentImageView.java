package com.moe.pussy.widget;
import android.widget.ImageView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.content.res.TypedArray;
import com.moe.pussy.R;

public class PercentImageView extends ImageView
{
	private float percent;
	public PercentImageView(Context context){
		super(context);
	}
	public PercentImageView(Context context,AttributeSet attrs){
		super(context,attrs);
		init(attrs);
	}
	public PercentImageView(Context context,AttributeSet attrs,int style){
		super(context,attrs,style);
		init(attrs);
	}
	public PercentImageView(Context context,AttributeSet attrs,int style,int defStyle){
		super(context,attrs,style,defStyle);
		init(attrs);
	}
	private void init(AttributeSet attrs){
		TypedArray ta=getContext().obtainStyledAttributes(attrs,R.styleable.percent);
		percent=ta.getFraction(R.styleable.percent_percent,1,1,1);
		ta.recycle();
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if(getLayoutParams().width==ViewGroup.LayoutParams.WRAP_CONTENT){
			setMeasuredDimension((int)(getMeasuredHeight()*percent),getMeasuredHeight());
		}else{
			setMeasuredDimension(getMeasuredWidth(),(int)(getMeasuredWidth()*percent));
		}
	}
	
}
