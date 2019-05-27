package widget;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutParams;
import android.support.v7.widget.RecyclerView.State;
import android.support.v7.widget.RecyclerView.Recycler;
import android.view.View;
import android.graphics.Rect;
import java.util.ArrayList;

public class WaterFullLayoutManager extends RecyclerView.LayoutManager
{
	private int verticalScrollOffset,totalHeight;
	private ArrayList<Rect> rects=new ArrayList<>();
	@Override
	public RecyclerView.LayoutParams generateDefaultLayoutParams()
	{
		return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT,RecyclerView.LayoutParams.WRAP_CONTENT);
	}

	@Override
	public boolean canScrollVertically()
	{
		// TODO: Implement this method
		return true;
	}

	@Override
	public void onItemsChanged(RecyclerView recyclerView)
	{
		// TODO: Implement this method
		super.onItemsChanged(recyclerView);
		rects.clear();
		for(int i=0;i<getItemCount();i++)
			rects.add(new Rect());
		
	}

	@Override
	public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount)
	{
		// TODO: Implement this method
		super.onItemsAdded(recyclerView, positionStart, itemCount);
		for(int i=0;i<itemCount;i++)
		rects.add(positionStart,new Rect());
	}

	@Override
	public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount)
	{
		// TODO: Implement this method
		super.onItemsRemoved(recyclerView, positionStart, itemCount);
		for(int i=0;i<itemCount;i++)
		rects.remove(positionStart);
	}

	@Override
	public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount)
	{
		// TODO: Implement this method
		super.onItemsMoved(recyclerView, from, to, itemCount);
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state)
	{
		detachAndScrapAttachedViews(recycler);
		int left=0,top=0,max=0;
		for(int i=0;i<getItemCount();i++){
			Rect rect=rects.get(i);
			View child=recycler.getViewForPosition(i);
			measureChildWithMargins(child,0,0);
			int width=getDecoratedMeasuredWidth(child);
			int height=getDecoratedMeasuredHeight(child);
			Rect rec=new Rect();
			//减去ItemDectoration尺寸
			calculateItemDecorationsForChild(child,rec);
			//removeAndRecycleView(child,recycler);
			if(left+width>getWidth()){
				left=0;
				top+=max;
				max=0;
			}
			//计算尺寸
			left+=rec.left;
			rect.left=left;
			rect.top=top+rec.top;
			rect.right=(left+=(width+rec.right));
			rect.bottom=rect.top+height+rec.bottom;
			if(!(rect.top-verticalScrollOffset>getHeight()||rect.bottom-verticalScrollOffset<0)){
				layoutDecoratedWithMargins(child,rect.left,rect.top-verticalScrollOffset,rect.right,rect.bottom-verticalScrollOffset);
				addView(child);
				}else{
				removeAndRecycleView(child,recycler);
				}
			max=Math.max(max,rect.height());
		}
		totalHeight=top+max;
		//detachAndScrapAttachedViews(recycler);
		//removeAndRecycleAllViews(recycler);
		//layout(recycler,state);
	}
	/*private void layout(RecyclerView.Recycler recycler,RecyclerView.State state){
		//显示出现的item
		
		for(int i=0;i<getItemCount();i++){
			Rect rect=rects.get(i);
			if(!(rect.top-verticalScrollOffset>getHeight()||rect.bottom-verticalScrollOffset<0)){
				View child=recycler.getViewForPosition(i);
				addView(child);
				measureChildWithMargins(child,0,0);
				layoutDecoratedWithMargins(child,rect.left,rect.top-verticalScrollOffset,rect.right,rect.bottom-verticalScrollOffset);

			}else{
				try{
				removeAndRecycleViewAt(i,recycler);
				}catch(Exception e){}
			}
		}
	}*/
	@Override
	public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
		//列表向下滚动dy为正，列表向上滚动dy为负，这点与Android坐标系保持一致。
		//实际要滑动的距离
		int travel = dy;
		detachAndScrapAttachedViews(recycler);
		if (verticalScrollOffset + dy < 0) {
			travel = -verticalScrollOffset;
		} else if (verticalScrollOffset + dy > totalHeight - getVerticalSpace()) {//如果滑动到最底部
			travel = totalHeight - getVerticalSpace() - verticalScrollOffset;
		}

		//将竖直方向的偏移量+travel
		verticalScrollOffset += travel;

		// 调用该方法通知view在y方向上移动指定距离
		offsetChildrenVertical(-travel);
		onLayoutChildren(recycler,state);
		return travel;
	}

	private int getVerticalSpace() {
		//计算RecyclerView的可用高度，除去上下Padding值
		return getHeight() - getPaddingBottom() - getPaddingTop();
	}

	}
