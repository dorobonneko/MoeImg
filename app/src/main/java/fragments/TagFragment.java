package fragments;
import android.view.*;
import java.util.*;
import org.jsoup.*;

import activitys.PostActivity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.widget.TextView;
import com.moe.moeimg.R;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import utils.MoeImg;
import widget.WaterFallLayout;
import android.support.v7.widget.RecyclerView;
import widget.WaterFullLayoutManager;
import adapter.TagsAdapter;
import widget.ItemDecoration;

public class TagFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,TagsAdapter.OnClickListener
{
	private SwipeRefreshLayout refresh;
	private RecyclerView mRecyclerView;
	private List<String> list;
	private TagsAdapter mTagsAdapter;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.refresh_list_view,container,false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		refresh=view.findViewById(R.id.swipeRefreshLayout);
		mRecyclerView=view.findViewById(R.id.recyclerView);
		refresh.setOnRefreshListener(this);
		refresh.setColorSchemeResources(R.color.logo);
		mRecyclerView.addItemDecoration(new ItemDecoration(10));
		mRecyclerView.setLayoutManager(new WaterFullLayoutManager());
		mRecyclerView.setAdapter(mTagsAdapter=new TagsAdapter(list=new ArrayList<>()));
		mTagsAdapter.setOnClickListener(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onActivityCreated(savedInstanceState);
		refresh.setRefreshing(true);
		onRefresh();
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){

				@Override
				public void uncaughtException(Thread p1, Throwable p2)
				{
					StringBuffer sb=new StringBuffer();
					for(StackTraceElement e:p2.getStackTrace())
					sb.append(e.toString());
					return;
				}
			});
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
				
				refresh.post(new Runnable(){

							@Override
							public void run()
							{
								refresh.setRefreshing(false);
								list.clear();
								list.addAll(tags);
								mTagsAdapter.notifyDataSetChanged();
							}
						});
				}catch(Exception e){
					refresh.post(new Runnable(){

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
	public void onClick(int p1)
	{
		Intent intent=new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(MoeImg.PREFIX.concat("/").concat(MoeImg.TAG).concat("/").concat(list.get(p1))));
		intent.setClass(getActivity(),PostActivity.class);
		getView().getContext().startActivity(intent);
		
	}


	
}
