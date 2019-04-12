package fragments;
import android.app.Fragment;
import android.view.ViewGroup;
import android.view.View;
import android.os.Bundle;
import android.view.LayoutInflater;
import com.moe.moeimg.R;
import android.support.v4.widget.SwipeRefreshLayout;
import org.jsoup.Jsoup;
import utils.MoeImg;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import widget.WaterFallLayout;
import android.widget.TextView;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;
import android.util.TypedValue;
import android.content.Intent;
import activitys.PostActivity;
import android.net.Uri;

public class TagFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,View.OnClickListener
{
	private SwipeRefreshLayout refresh;
	private WaterFallLayout mWaterFallLayout;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.waterfalllayout,container,false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		refresh=view.findViewById(R.id.swipeRefreshLayout);
		mWaterFallLayout=view.findViewById(R.id.waterFallLayout);
		refresh.setOnRefreshListener(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onActivityCreated(savedInstanceState);
		refresh.setRefreshing(true);
		onRefresh();
	}

	@Override
	public void onRefresh()
	{
		new Thread(){
			public void run(){
				try{
				Connection conn=Jsoup.connect(MoeImg.PREFIX.concat("/").concat(MoeImg.TAGCLOUD));
				Document doc=conn.get();
				Elements a=doc.getElementsByAttributeValueStarting("class","tag-link-");
				final Set<String> tags=new LinkedHashSet<>(a.size());
				for(int i=0;i<a.size();i++)
				tags.add(a.get(i).ownText());
				final View[] views=new View[tags.size()];
				Iterator<String> iterator=tags.iterator();
				int i=0;
				while(iterator.hasNext()){
					TextView tv=new TextView(mWaterFallLayout.getContext());
					tv.setBackgroundResource(R.drawable.button);
					tv.setOnClickListener(TagFragment.this);
					tv.setText(iterator.next());
					tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,(int)(Math.random()*8)+10);
					views[i++]=tv;
				}
				getView().post(new Runnable(){

							@Override
							public void run()
							{
								refresh.setRefreshing(false);
								mWaterFallLayout.removeAllViews();
								for(View tag:views){
									/*TextView tv=new TextView(mWaterFallLayout.getContext());
									tv.setBackgroundResource(R.drawable.button);
									tv.setOnClickListener(TagFragment.this);
									tv.setText(tag);*/
									mWaterFallLayout.addView(tag);
								}
							}
						});
				}catch(Exception e){
					getView().post(new Runnable(){

							@Override
							public void run()
							{
								refresh.setRefreshing(false);
							}
						});
				}
			}
		}.start();
	}

	@Override
	public void onClick(View p1)
	{
		Intent intent=new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(MoeImg.PREFIX.concat("/").concat(MoeImg.TAG).concat("/").concat(((TextView)p1).getText().toString())));
		intent.setClass(p1.getContext(),PostActivity.class);
		p1.getContext().startActivity(intent);
		
	}


	
}
