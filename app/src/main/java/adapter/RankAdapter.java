package adapter;
import android.view.*;
import android.widget.*;

import adapter.RankAdapter;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import com.moe.moeimg.R;
import empty.Rank_Item;
import java.util.List;
import widget.WaterFallLayout;
import com.moe.pussy.Pussy;
import com.moe.pussy.transformer.CropTransformer;
import com.moe.pussy.transformer.RoundTransformer;

public class RankAdapter extends RecyclerView.Adapter<RankAdapter.ViewHolder>
{
	private List<Rank_Item> list;
	private OnItemClickListener oicl;
	public RankAdapter(List<Rank_Item> list)
	{
		this.list = list;
	}
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup recyclerView, int viewType)
	{
		return new ViewHolder(LayoutInflater.from(recyclerView.getContext()).inflate(R.layout.list_item, recyclerView, false));
	}

	@Override
	public void onBindViewHolder(ViewHolder vh, int p2)
	{
		Rank_Item item=list.get(vh.getAdapterPosition());
		//Picasso.get().load(item.img).placeholder(R.drawable.logo).error(R.drawable.logo).noFade().fit().centerCrop(Gravity.TOP).transform(round).into(vh.img);
		Pussy.$(vh.itemView.getContext()).load(item.img).execute().placeHolder(R.drawable.logo).error(R.drawable.logo).transformer(new CropTransformer(Gravity.CENTER), new RoundTransformer((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, vh.itemView.getResources().getDisplayMetrics()))).into(vh.img);

		vh.title.setText(item.title);
		vh.type.setText(item.pv);
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

	public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
	{
		ImageView img;
		TextView title,type,date;
		WaterFallLayout mWaterFallLayout;
		ViewHolder(View v)
		{
			super(v);
			img = v.findViewById(R.id.image);
			title = v.findViewById(R.id.title);
			type = v.findViewById(R.id.type);
			date = v.findViewById(R.id.date);
			mWaterFallLayout = v.findViewById(R.id.waterFallLayout);
			v.setOnClickListener(this);
			mWaterFallLayout.setVisibility(View.GONE);
		}

		@Override
		public void onClick(View p1)
		{
			if (p1 == itemView)
			{
				if (oicl != null)
					oicl.onItemClick(RankAdapter.this, this);
			}
		}


	}
	public void setOnItemClickListener(OnItemClickListener l)
	{
		oicl = l;
	}
	public interface OnItemClickListener
	{
		void onItemClick(RankAdapter ra, ViewHolder vh);
	}
}
