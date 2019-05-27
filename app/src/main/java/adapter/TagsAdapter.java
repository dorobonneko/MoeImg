package adapter;
import android.view.*;

import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.widget.TextView;
import com.moe.moeimg.R;
import java.util.List;

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.ViewHolder>
{
	private List<String> list;
	private OnClickListener ocl;
	public TagsAdapter(List<String> list){
		this.list=list;
	}
	@Override
	public TagsAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
	{
		// TODO: Implement this method
		return new ViewHolder(new TextView(p1.getContext()));
	}

	@Override
	public void onBindViewHolder(TagsAdapter.ViewHolder p1, int p2)
	{
		p1.text.setText(list.get(p1.getAdapterPosition()));
	}

	@Override
	public int getItemCount()
	{
		// TODO: Implement this method
		return list.size();
	}
	
	public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
		TextView text;
		public ViewHolder(View v){
			super(v);
			text=(TextView) v;
			text.setBackgroundResource(R.drawable.button);
			text.setOnClickListener(this);
			text.setTextSize(TypedValue.COMPLEX_UNIT_SP,(int)(Math.random()*8)+10);
			
		}

		@Override
		public void onClick(View p1)
		{
			if(ocl!=null)
			ocl.onClick(getAdapterPosition());
		}
		
	}

	@Override
	public int getItemViewType(int position)
	{
		// TODO: Implement this method
		return position;
	}
	
	public void setOnClickListener(OnClickListener l){
		ocl=l;
	}
	public interface OnClickListener{
		void onClick(int position);
	}
}
