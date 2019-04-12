package widget;
import android.widget.FrameLayout;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class WaterFallLayout extends FrameLayout
{
	private int space=20;
	public WaterFallLayout(Context context){
		this(context,null);
	}
	public WaterFallLayout(Context context,AttributeSet attrs){
		super(context,attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int width=MeasureSpec.getSize(widthMeasureSpec);
		int height=MeasureSpec.getSize(heightMeasureSpec);
		int left=getPaddingStart(),top=getPaddingTop(),maxHeight=0;
		for(int i=0;i<getChildCount();i++){
			View child=getChildAt(i);
			child.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.AT_MOST),MeasureSpec.makeMeasureSpec(height,MeasureSpec.UNSPECIFIED));
			if(child.getVisibility()==GONE)continue;
			if(left+child.getMeasuredWidth()>width-getPaddingEnd()){
				top+=(maxHeight+space);
				maxHeight=0;
				left=0;
			}
			left+=(child.getMeasuredWidth()+space);
			maxHeight=Math.max(maxHeight,child.getMeasuredHeight());
		}
		setMeasuredDimension(width,top+maxHeight+getPaddingBottom());
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int right, int bottom)
	{
		int left=getPaddingStart(),top=getPaddingTop(),maxHeight=0;
		for(int i=0;i<getChildCount();i++){
			View child=getChildAt(i);
			if(child.getVisibility()==GONE)continue;
			if(left+child.getMeasuredWidth()>getMeasuredWidth()-getPaddingEnd()){
				top+=(maxHeight+space);
				maxHeight=0;
				left=0;
			}
			child.layout(left,top,left+=child.getMeasuredWidth(),top+child.getMeasuredHeight());
			left+=space;
			maxHeight=Math.max(maxHeight,child.getMeasuredHeight());
		}
	}
	
}
