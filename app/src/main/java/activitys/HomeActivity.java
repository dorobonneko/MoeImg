package activitys;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import fragments.*;

import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import com.moe.moeimg.R;
import utils.MoeImg;
import java.util.Date;

public class HomeActivity extends BaseActivity implements View.OnApplyWindowInsetsListener,NavigationView.OnNavigationItemSelectedListener
{

	private NavigationView mNavigationView;
	private DrawerLayout mDrawerLayout;
	private Fragment current;
	private int id;
	private Dialog search_dialog;
	private int exit=65536;
	private DatePickerDialog dpd;
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
			case R.id.tags:{
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
				}break;
			case R.id.ranking:
			{
				RankingFragment ranking=(RankingFragment) getFragmentManager().findFragmentByTag(String.valueOf(p1.getItemId()));
				if(ranking==null)
					ranking=new RankingFragment();
				FragmentTransaction ft=getFragmentManager().beginTransaction();
				if(current!=null)
					ft.hide(current);
				if(ranking.isAdded())
					ft.show(ranking);
				else
					ft.add(R.id.fragment,ranking,String.valueOf(p1.getItemId()));
				ft.commit();
				current=ranking;
			}
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
			case R.id.calendar:
				if(dpd==null){
					dpd=new DatePickerDialog(this);
					dpd.setOnDateSetListener(new DatePickerDialog.OnDateSetListener(){

							@Override
							public void onDateSet(DatePicker p1, int p2, int p3, int p4)
							{
								Intent intent=new Intent(Intent.ACTION_VIEW);
								intent.setData(Uri.parse(MoeImg.PREFIX.concat("/").concat(String.valueOf(p2)).concat("/").concat(String.valueOf(p3+1)).concat("/").concat(String.valueOf(p4))));
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intent.setClass(getApplicationContext(),PostActivity.class);
								startActivity(intent);
							}
						});
					}dpd.show();
					dpd.getDatePicker().setMinDate(Date.parse("2013/5/1"));
					dpd.getDatePicker().setMaxDate(System.currentTimeMillis());
					break;
		}
		return true;
	}

	@Override
	public void finish()
	{
		if(id!=R.id.ero){
			mNavigationView.setCheckedItem(R.id.ero);
			onNavigationItemSelected(mNavigationView.getMenu().findItem(R.id.ero));
		}else{
			if(mNavigationView.getHandler().hasMessages(exit))
				super.finish();
				else{
			Toast.makeText(this,R.string.exit_toast_msg,Toast.LENGTH_SHORT).show();
			mNavigationView.getHandler().sendEmptyMessageDelayed(exit,2500);
			}
		}
	}
	class Exit implements Runnable
	{
		@Override
		public void run()
		{
			
		}
	}
}
