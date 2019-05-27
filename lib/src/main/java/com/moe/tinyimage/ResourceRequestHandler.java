package com.moe.tinyimage;
import com.moe.tinyimage.Pussy.RequestHandler.Response;
import android.net.Uri;
import java.util.Map;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public class ResourceRequestHandler implements Pussy.RequestHandler
{
	private Context mContext;
	private Resources.Theme theme;
	public ResourceRequestHandler(Context context,Resources.Theme theme){
		this.mContext=context.getApplicationContext();
		this.theme=theme;
	}
	@Override
	public boolean canHandle(Uri uri)
	{
		if(uri.getScheme().equals("res"))
			return true;
		return false;
	}

	@Override
	public Pussy.RequestHandler.Response load(Uri uri, Map<String, String> header)
	{
		int res=Integer.parseInt(uri.getLastPathSegment());
		Drawable d=mContext.getResources().getDrawable(res,theme);
		return null;
	}
	
}
