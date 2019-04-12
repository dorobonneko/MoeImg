package activitys;
import android.app.Activity;
import android.os.Bundle;
import org.jsoup.Jsoup;
import utils.MoeImg;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.util.List;
import empty.Image_Item;
import com.moe.moeimg.R;
import android.support.v7.widget.RecyclerView;
import adapter.DetailsAdapter;
import android.support.v7.widget.LinearLayoutManager;
import java.util.ArrayList;
import empty.Item;
import android.support.v4.widget.SwipeRefreshLayout;
import android.widget.Toolbar;
import android.view.View;
import android.view.WindowInsets;
import android.view.ViewGroup;
public class DetailsActivity extends Activity implements View.OnApplyWindowInsetsListener,SwipeRefreshLayout.OnRefreshListener
{
	private List<Item> list;
	private DetailsAdapter mDetailsAdapter;
	private SwipeRefreshLayout refresh;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_view);
		getLayoutInflater().inflate(R.layout.refresh_list_view,(ViewGroup)findViewById(R.id.content),true);
		RecyclerView recyclerView=findViewById(R.id.recyclerView);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setAdapter(mDetailsAdapter=new DetailsAdapter(list=new ArrayList<>()));
		refresh=findViewById(R.id.swipeRefreshLayout);
		refresh.setOnRefreshListener(this);
		Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
		setActionBar(toolbar);
		View parent=(View)toolbar.getParent();
		parent.setFitsSystemWindows(true);
		parent.setOnApplyWindowInsetsListener(this);
		parent.requestApplyInsets();
		load();
	}

	@Override
	public WindowInsets onApplyWindowInsets(View p1, WindowInsets p2)
	{
		p1.setPadding(0,p2.getSystemWindowInsetTop(),0,0);
		RecyclerView recyclerView=findViewById(R.id.recyclerView);
		recyclerView.setPadding(0,0,0,p2.getSystemWindowInsetBottom());
		recyclerView.setClipToPadding(false);
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
				Connection conn=Jsoup.connect(getIntent().getDataString());
				Document doc=conn.get();
				final String title=doc.select("h1.title").get(0).text();
				Elements images=doc.getElementsByClass("thumbnail_image");
				final List<Item> tempList=new ArrayList<>(images.size());
				for(int i=0;i<images.size();i++){
					Image_Item item=new Image_Item();
					item.url=images.get(i).attr("abs:src");
					item.index=Integer.parseInt(images.get(i).attr("alt"));
					tempList.add(item);
				}
					runOnUiThread(new Runnable(){

							@Override
							public void run()
							{
								int size=list.size();
								list.clear();
								mDetailsAdapter.notifyItemRangeRemoved(0,size);
								list.addAll(tempList);
								mDetailsAdapter.notifyItemRangeInserted(0,list.size());
								refresh.setRefreshing(false);
								getActionBar().setSubtitle(title);
							}
						});
				}catch(Exception e){
					runOnUiThread(new Runnable(){

							@Override
							public void run()
							{
								int size=list.size();
								list.clear();
								mDetailsAdapter.notifyItemRangeRemoved(0,size);
								refresh.setRefreshing(false);
							}
						});
				}
			}
		}.start();
	}


}
