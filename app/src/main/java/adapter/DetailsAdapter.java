package adapter;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;
import empty.Item;
import empty.Image_Item;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.moe.moeimg.R;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import com.bumptech.glide.request.RequestOptions;

public class DetailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
	private List<Item> list;
	public DetailsAdapter(List<Item> list){
		this.list=list;
	}
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup recyclerView, int viewType)
	{
		if(list.get(viewType) instanceof Image_Item)
				return new ImageHolder(LayoutInflater.from(recyclerView.getContext()).inflate(R.layout.image_item,recyclerView,false));
		return null;
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder vh, int p2)
	{
		if(vh instanceof ImageHolder){
			ImageHolder ih=(ImageHolder)vh;
			ih.index.setText(String.valueOf(((Image_Item)list.get(vh.getAdapterPosition())).index));
			Glide.with(vh.itemView.getContext()).load(((Image_Item)list.get(vh.getAdapterPosition())).url).apply(new RequestOptions().placeholder(R.drawable.logo).error(R.drawable.logo)).into(ih.img);
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
	
	public class ImageHolder extends RecyclerView.ViewHolder{
		ImageView img;
		TextView index;
		ImageHolder(View v){
			super(v);
			ViewGroup vg=(ViewGroup) v;
			index=(TextView) vg.getChildAt(0);
			img=(ImageView) vg.getChildAt(1);
		}
	}
	public class ReplyHolder extends RecyclerView.ViewHolder{
		ReplyHolder(View v){
			super(v);
		}
	}
}