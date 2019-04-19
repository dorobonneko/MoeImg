package com.moe.pussy;
import android.graphics.drawable.Drawable;
import com.moe.pussy.Request.Builder;
import com.moe.pussy.Request;
import java.io.OutputStream;
import com.moe.pussy.RequestHandler.Callback;
import com.moe.pussy.Request;
import android.net.NetworkInfo;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import android.net.Uri;

public class NetWorkRequestHandler extends RequestHandler
{
	private Map<String,List<PussySocket>> map=new HashMap<>();
	@Override
	boolean canHandleRequest(Request.Builder request)
	{
		return request.getUri()!=null&&(request.getUri().getScheme().equalsIgnoreCase("http")||request.getUri().getScheme().equalsIgnoreCase("https"));
	}

	@Override
	void load(Pussy p, com.moe.pussy.Request.Builder request, CacheDescriptor cd, RequestHandler.Callback call)
	{
		Uri uri=request.getUri();
		List<PussySocket> queue=map.get(uri.getAuthority());
		
	}

	@Override
	int getRetryCount()
	{
		return 2;
	}

	@Override
	boolean shouldRetry(boolean airplaneMode, NetworkInfo info)
	{
		return info!=null&&info.isConnected();
	}

	@Override
	boolean supportsReplay()
	{
		return true;
	}


	

	
}
