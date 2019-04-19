package adapter;
import android.view.*;
import android.widget.*;
import empty.*;

import android.support.v7.widget.RecyclerView;
import com.moe.moeimg.R;
import java.util.List;
import java.util.ArrayList;
import android.content.Intent;
import android.net.Uri;
import android.view.ContextMenu.ContextMenuInfo;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import com.moe.tinyimage.TinyImage;

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
			TinyImage.get(vh.itemView.getContext()).load(((Image_Item)list.get(vh.getAdapterPosition())).url,ih.img).placeHolder(R.drawable.logo).error(R.drawable.logo).commit();
			//Picasso.get().load(((Image_Item)list.get(vh.getAdapterPosition())).url).placeholder(R.drawable.logo).error(R.drawable.logo).into(ih.img);
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
		return 0;
	}
	
	public class ImageHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener{
		ImageView img;
		TextView index;
		ImageHolder(View v){
			super(v);
			ViewGroup vg=(ViewGroup) v;
			index=(TextView) vg.getChildAt(0);
			img=(ImageView) vg.getChildAt(1);
			img.setOnLongClickListener(this);
		}

		@Override
		public boolean onLongClick(final View view)
		{
			new AlertDialog.Builder(view.getContext()).setItems(R.array.search_image, new DialogInterface.OnClickListener(){

						@Override
						public void onClick(DialogInterface p1, int p2)
						{
							String[] urls=view.getResources().getStringArray(R.array.search_url);
							String url=String.format(urls[p2],((Image_Item)list.get(getAdapterPosition())).url);
							Intent intent=new Intent(Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.parse(url),p2<urls.length-1?"text/html":"image/*");
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							try{itemView.getContext().startActivity(intent);}catch(Exception e){}
							
						}
					}).create().show();
			return true;
		}

		
	}
	public class ReplyHolder extends RecyclerView.ViewHolder{
		ReplyHolder(View v){
			super(v);
		}
	}
}
