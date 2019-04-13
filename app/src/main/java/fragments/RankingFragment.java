package fragments;
import adapter.*;
import android.support.v7.widget.*;
import android.view.*;
import empty.*;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;

import activitys.DetailsActivity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import com.moe.moeimg.R;
import org.jsoup.select.Elements;
import utils.MoeImg;

public class RankingFragment extends Fragment implements RankAdapter.OnItemClickListener,SwipeRefreshLayout.OnRefreshListener,View.OnApplyWindowInsetsListener{
private List<Rank_Item> list;
	private RankAdapter mRankAdapter;
	private SwipeRefreshLayout refresh;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		return inflater.inflate(R.layout.refresh_list_view,container,false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onViewCreated(view, savedInstanceState);
		RecyclerView recyclerView=view.findViewById(R.id.recyclerView);
		recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
		recyclerView.setAdapter(mRankAdapter=new RankAdapter(list=new ArrayList<>()));
		mRankAdapter.setOnItemClickListener(this);
		refresh=view.findViewById(R.id.swipeRefreshLayout);
		refresh.setOnRefreshListener(this);
		recyclerView.setFitsSystemWindows(true);
		recyclerView.setOnApplyWindowInsetsListener(this);
		recyclerView.requestApplyInsets();
		refresh.setColorSchemeResources(R.color.logo);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onActivityCreated(savedInstanceState);
		load();
	}
	
	
	@Override
	public WindowInsets onApplyWindowInsets(View p1, WindowInsets p2)
	{
		p1.setPadding(0,0,0,p2.getSystemWindowInsetBottom());
		return p2;
	}

	@Override
	public void onRefresh()
	{
		load();
	}



	private void load(){
		if(!refresh.isRefreshing())
			refresh.setRefreshing(true);
		new Thread(){
			public void run(){
				try{
					Connection conn=Jsoup.connect(MoeImg.PREFIX.concat("/").concat(MoeImg.RANKING));
					Document doc=conn.get();
					Elements posts=doc.select(".wpp-ul-ranking > li");
					final List<Rank_Item> tempList=new ArrayList<>(posts.size());
					for(int i=0;i<posts.size();i++){
						Element post=posts.get(i);
						Rank_Item item=new Rank_Item();
						item.img=post.getElementsByTag("img").get(0).absUrl("src");
						Element thumb=post.getElementsByClass("thumb").get(0);
						Element a=thumb.child(0);
						item.title=a.attr("title");
						item.url=a.absUrl("href");
						item.pv=post.getElementsByClass("wpp-views").get(0).ownText();
						item.index=String.valueOf(i);
						tempList.add(item);
					}
					getView().post(new Runnable(){

							@Override
							public void run()
							{
								//PostFragment.this.title=title;
								int size=list.size();
								
								list.clear();
								mRankAdapter.notifyItemRangeRemoved(0,size);
								list.addAll(tempList);
								mRankAdapter.notifyItemRangeInserted(0,tempList.size());
								refresh.setRefreshing(false);
								
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
	public void onItemClick(RankAdapter pa, RankAdapter.ViewHolder vh)
	{
		Intent intent=new Intent(Intent.ACTION_VIEW);
		intent.setClass(getActivity(),DetailsActivity.class);
		intent.setData(Uri.parse(list.get(vh.getAdapterPosition()).url));
		startActivity(intent);
	}
}
