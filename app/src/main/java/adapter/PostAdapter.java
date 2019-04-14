package adapter;

import android.view.*;
import android.widget.*;
import utils.*;

import activitys.PostActivity;
import adapter.PostAdapter;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import com.moe.moeimg.R;
import com.squareup.picasso.Picasso;
import empty.Post_Item;
import java.util.List;
import widget.WaterFallLayout;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder>
{
	private List<Post_Item> list;
	private OnItemClickListener oicl;
	private RoundTransform round;
	public PostAdapter(List<Post_Item> list){
		this.list=list;
	}
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup recyclerView, int viewType)
	{
		return new ViewHolder(LayoutInflater.from(recyclerView.getContext()).inflate(R.layout.list_item,recyclerView,false));
	}

	@Override
	public void onBindViewHolder(ViewHolder vh, int p2)
	{
		Post_Item item=list.get(vh.getAdapterPosition());
		if(round==null)
			round=new RoundTransform((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,10,vh.itemView.getResources().getDisplayMetrics()));
		Picasso.get().load(item.img).placeholder(R.drawable.logo).error(R.drawable.logo).noFade().fit().centerCrop(Gravity.TOP).transform(round).into(vh.img);
		vh.title.setText(item.title);
		vh.date.setText(item.date);
		vh.type.setText(item.type);
		String[] tags=item.tags;
		int i=0;
		for(;i<vh.mWaterFallLayout.getChildCount();i++){
			TextView tv=(TextView) vh.mWaterFallLayout.getChildAt(i);
			if(i<tags.length)
			tv.setText(tags[i]);
			else
			tv.setVisibility(View.GONE);
		}
		for(;i<tags.length;i++){
				TextView tv=new TextView(vh.mWaterFallLayout.getContext());
				tv.setOnClickListener(vh);
				tv.setBackgroundResource(R.drawable.ripple_button);
				tv.setText(tags[i]);
				vh.mWaterFallLayout.addView(tv);
			}
	}

	@Override
	public int getItemCount()
	{
		return list.size();
	}

	@Override
	public int getItemViewType(int position)
	{
		return position;
	}

	public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
		ImageView img;
		TextView title,type,date;
		WaterFallLayout mWaterFallLayout;
		ViewHolder(View v){
			super(v);
			img=v.findViewById(R.id.image);
			title=v.findViewById(R.id.title);
			type=v.findViewById(R.id.type);
			date=v.findViewById(R.id.date);
			mWaterFallLayout=v.findViewById(R.id.waterFallLayout);
			v.setOnClickListener(this);
		}

		@Override
		public void onClick(View p1)
		{
			if(p1==itemView){
			if(oicl!=null)
				oicl.OnItemClick(PostAdapter.this,this);
				}else{
					Intent intent=new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(MoeImg.PREFIX.concat("/").concat(MoeImg.TAG).concat("/").concat(list.get(getAdapterPosition()).tags[((ViewGroup)p1.getParent()).indexOfChild(p1)])));
					intent.setClass(p1.getContext(),PostActivity.class);
					p1.getContext().startActivity(intent);
				}
		}

		
	}
	public void setOnItemClickListener(OnItemClickListener l){
		oicl=l;
	}
	public interface OnItemClickListener{
		void OnItemClick(PostAdapter pa,ViewHolder vh);
	}
}
