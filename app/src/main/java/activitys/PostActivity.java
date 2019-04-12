package activitys;
import android.support.v7.widget.*;
import android.view.*;
import empty.*;
import java.util.*;
import org.jsoup.*;

import adapter.DetailsAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.widget.Toolbar;
import com.moe.moeimg.R;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import adapter.PostAdapter;
import adapter.PostAdapter.ViewHolder;
import android.content.Intent;
import android.net.Uri;
import fragments.PostFragment;

public class PostActivity extends Activity implements View.OnApplyWindowInsetsListener
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_view);
		Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
		setActionBar(toolbar);
		View parent=(View) toolbar.getParent();
		parent.setFitsSystemWindows(true);
		parent.setOnApplyWindowInsetsListener(this);
		parent.requestApplyInsets();
		PostFragment pf=null;
		if(savedInstanceState==null){
		pf=new PostFragment();
		pf.setUrl(getIntent().getDataString());
		}else
		pf=(PostFragment) getFragmentManager().findFragmentByTag("post");
		if(pf.isAdded())
			getFragmentManager().beginTransaction().show(pf).commit();
			else
		getFragmentManager().beginTransaction().add(R.id.content,pf,"post").commit();
	}

	@Override
	public WindowInsets onApplyWindowInsets(View p1, WindowInsets p2)
	{
		p1.setPadding(p2.getSystemWindowInsetLeft(),p2.getSystemWindowInsetTop(),p2.getSystemWindowInsetRight(),0);
		return p2;
	}

	
	
}
