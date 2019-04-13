package activitys;

import android.app.Activity;
import android.os.Bundle;
import com.moe.moeimg.R;
import android.view.View;
import android.view.WindowInsets;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.widget.Toolbar;
import fragments.PostFragment;
import utils.MoeImg;
import android.net.Uri;
import android.app.Fragment;
import android.app.FragmentTransaction;
import fragments.TagFragment;
import android.view.Menu;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.content.Intent;
import android.widget.Spinner;
import android.content.res.TypedArray;

public class HomeActivity extends Activity implements View.OnApplyWindowInsetsListener,NavigationView.OnNavigationItemSelectedListener
{

	private NavigationView mNavigationView;
	private DrawerLayout mDrawerLayout;
	private Fragment current;
	private int id;
	private Dialog search_dialog;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mDrawerLayout=findViewById(R.id.drawerLayout);
		mDrawerLayout.setOnApplyWindowInsetsListener(this);
		mNavigationView=findViewById(R.id.navigationView);
		mDrawerLayout.requestApplyInsets();
		mNavigationView.setOnApplyWindowInsetsListener(null);
		mNavigationView.setNavigationItemSelectedListener(this);
		setActionBar((Toolbar)findViewById(R.id.toolbar));
		//getActionBar().setLogo(R.drawable.logo);
		if(savedInstanceState==null){
		mNavigationView.setCheckedItem(R.id.ero);
		onNavigationItemSelected(mNavigationView.getMenu().findItem(R.id.ero));
		}else{
			mNavigationView.setCheckedItem(savedInstanceState.getInt("select",R.id.ero));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		// TODO: Implement this method
		super.onSaveInstanceState(outState);
		outState.putInt("select",id);
	}

	@Override
	public WindowInsets onApplyWindowInsets(View p1, WindowInsets p2)
	{
		((View)findViewById(R.id.toolbar).getParent()).setPadding(0,p2.getSystemWindowInsetTop(),0,0);
		mDrawerLayout.setPadding(p2.getSystemWindowInsetLeft(),p2.getSystemWindowInsetTop(),p2.getSystemWindowInsetRight(),p2.getSystemWindowInsetBottom());
		
		//p2.consumeSystemWindowInsets();
		//p2.consumeStableInsets();
		return p2;
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem p1)
	{
		id=p1.getItemId();
		mDrawerLayout.closeDrawers();
		getActionBar().setSubtitle(p1.getTitle());
		switch(p1.getItemId()){
			case R.id.ero:
			case R.id.noero:{
				PostFragment ero=(PostFragment) getFragmentManager().findFragmentByTag(String.valueOf(p1.getItemId()));
				if(ero==null){
				ero=new PostFragment();
				ero.setUrl(Uri.withAppendedPath(Uri.withAppendedPath(Uri.parse(MoeImg.PREFIX),MoeImg.CATEGORY),p1.getTitle().toString()).toString());
				}FragmentTransaction ft=getFragmentManager().beginTransaction();
				if(current!=null)
					ft.hide(current);
				if(ero.isAdded())
					ft.show(ero).commit();
					else
					ft.add(R.id.fragment,ero,String.valueOf(p1.getItemId())).commit();
					current=ero;
				}break;
			case R.id.tags:
				TagFragment tag=(TagFragment) getFragmentManager().findFragmentByTag(String.valueOf(p1.getItemId()));
				if(tag==null)
					tag=new TagFragment();
				FragmentTransaction ft=getFragmentManager().beginTransaction();
				if(current!=null)
					ft.hide(current);
					if(tag.isAdded())
						ft.show(tag);
						else
						ft.add(R.id.fragment,tag,String.valueOf(p1.getItemId()));
						ft.commit();
					current=tag;
				break;
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu,menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId()){
			case R.id.search:
				if(search_dialog==null){
					search_dialog = new AlertDialog.Builder(this).setTitle("キーワード検索").setView(R.layout.search_view).setPositiveButton("検索", new DialogInterface.OnClickListener(){

							@Override
							public void onClick(DialogInterface p1, int p2)
							{
								String key=((EditText)search_dialog.findViewById(R.id.search_key)).getText().toString();
								Intent intent=new Intent(Intent.ACTION_VIEW);
								final TypedArray selectedValues = getResources().obtainTypedArray(R.array.cat);
								intent.setData(Uri.parse(MoeImg.PREFIX.concat("/?cat=").concat(selectedValues.getString(((Spinner)search_dialog.findViewById(R.id.search_cat)).getSelectedItemPosition())).concat("&s=").concat(key)));
								selectedValues.recycle();
								intent.setClass(HomeActivity.this,PostActivity.class);
								startActivity(intent);
								
							}
						}).create();
				}
				search_dialog.show();
				((EditText)search_dialog.findViewById(R.id.search_key)).setText(null);
				break;
		}
		return true;
	}

}
