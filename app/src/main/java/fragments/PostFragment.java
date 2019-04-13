package fragments;
import android.support.v7.widget.*;
import android.view.*;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;

import activitys.DetailsActivity;
import adapter.PostAdapter;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.widget.Toolbar;
import com.moe.moeimg.R;
import empty.Post_Item;
import org.jsoup.select.Elements;
import utils.MoeImg;

public class PostFragment extends Fragment implements View.OnApplyWindowInsetsListener,SwipeRefreshLayout.OnRefreshListener,PostAdapter.OnItemClickListener
{
	private List<Post_Item> list;
	private PostAdapter mPostAdapter;
	private SwipeRefreshLayout refresh;
	private String url,title;
	private int page=1;
	private boolean loadMore;
	private Scroll scroll=new Scroll();
	private boolean first=true;
	public void setUrl(String dataString)
	{
		this.url=dataString;
		page=MoeImg.getPage(dataString);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		// TODO: Implement this method
		super.onSaveInstanceState(outState);
		outState.putString("url",url);
		outState.putString("title",title);
		outState.putInt("page",page);
		outState.putBoolean("loadMore",loadMore);
		outState.putBoolean("first",first);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		if(savedInstanceState!=null){
		url=savedInstanceState.getString("url");
		title=savedInstanceState.getString("title");
		page=savedInstanceState.getInt("page");
		loadMore=savedInstanceState.getBoolean("loadMore");
		first=savedInstanceState.getBoolean("first");
		}
	}

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
		recyclerView.setAdapter(mPostAdapter=new PostAdapter(list=new ArrayList<>()));
		mPostAdapter.setOnItemClickListener(this);
		refresh=view.findViewById(R.id.swipeRefreshLayout);
		refresh.setOnRefreshListener(this);
		recyclerView.setFitsSystemWindows(true);
		recyclerView.setOnApplyWindowInsetsListener(this);
		recyclerView.requestApplyInsets();
		recyclerView.addOnScrollListener(scroll);
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
		page=1;
		loadMore=true;
		first=true;
		load();
	}



	private void load(){
		if(!refresh.isRefreshing())
			refresh.setRefreshing(true);
		new Thread(){
			public void run(){
				try{
					Connection conn=Jsoup.connect(MoeImg.changePage(url,page));
					Document doc=conn.get();
					final boolean pagenation=doc.getElementsByClass("pagenation").size()>0;
					try{
					title=doc.select(".bold").get(0).text();
					}catch(Exception e){
						try{
						title=doc.select(".result>h1").get(0).text();
						}catch(Exception ee){}
					}
					Elements posts=doc.select(".post");
					final List<Post_Item> tempList=new ArrayList<>(posts.size());
					for(int i=0;i<posts.size();i++){
						Element post=posts.get(i);
						if(post.select(".pc_ad").size()>0)continue;
						Post_Item item=new Post_Item();
						Element field=post.getElementsByAttributeValue("class","box list").get(0);
						Element link=field.child(0);
						item.url=link.absUrl("href");
						item.title=link.attr("title");
						item.img=field.getElementsByClass("thumb-outer").get(0).child(0).absUrl("src");
						item.type=post.getElementsByAttributeValue("rel","category tag").get(0).text();
						item.date=post.getElementsByClass("cal").get(0).text();
						Elements tagsE=post.getElementsByAttributeValue("rel","tag");
						String[] tags=new String[tagsE.size()];
						for(int n=0;n<tagsE.size();n++){
							tags[n]=tagsE.get(n).text();
						}
						item.tags=tags;
						tempList.add(item);
					}
					getView().post(new Runnable(){

							@Override
							public void run()
							{
								//PostFragment.this.title=title;
								int size=list.size();
								
								if(first){
								list.clear();
								mPostAdapter.notifyItemRangeRemoved(0,size);
								first=false;
								size=0;
								}
								list.addAll(tempList);
								loadMore=tempList.size()==10&&pagenation;
								mPostAdapter.notifyItemRangeInserted(size,tempList.size());
								refresh.setRefreshing(false);
								getActivity().getActionBar().setSubtitle(title);
							}
						});
				}catch(Exception e){
					getView().post(new Runnable(){

							@Override
							public void run()
							{
								loadMore=false;
								refresh.setRefreshing(false);
							}
						});
				}
			}
		}.start();
	}

	@Override
	public void OnItemClick(PostAdapter pa, PostAdapter.ViewHolder vh)
	{
		Intent intent=new Intent(Intent.ACTION_VIEW);
		intent.setClass(getActivity(),DetailsActivity.class);
		intent.setData(Uri.parse(list.get(vh.getAdapterPosition()).url));
		startActivity(intent);
	}

	@Override
	public void onHiddenChanged(boolean hidden)
	{
		// TODO: Implement this method
		super.onHiddenChanged(hidden);
		if(!hidden)
			getActivity().getActionBar().setSubtitle(title);
	}



	class Scroll extends RecyclerView.OnScrollListener
	{

		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy)
		{
			LinearLayoutManager llm=(LinearLayoutManager) recyclerView.getLayoutManager();
			if(loadMore&&!refresh.isRefreshing()&&llm.findLastVisibleItemPosition()>=list.size()-3){
				page++;
				refresh.setRefreshing(true);
				load();
			}
			
		}
		
	}
	
}
