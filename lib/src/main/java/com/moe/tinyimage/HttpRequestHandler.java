package com.moe.tinyimage;
import java.io.*;
import java.net.*;
import java.util.*;
import android.net.Uri;

public class HttpRequestHandler implements Pussy.RequestHandler
{

	@Override
	public boolean canHandle(Uri uri)
	{
		switch(uri.getScheme()){
			case "http":
			case "https":
				return true;
		}
		return false;
	}
	
	@Override
	public Pussy.RequestHandler.Response load(Uri uri, Map<String, String> header)
	{
		HttpURLConnection huc=null;
		String url=uri.toString();
		location:
		try
		{
			huc = (HttpURLConnection) new URL(url.toString()).openConnection();
			Iterator<Map.Entry<String,String>> iterator=header.entrySet().iterator();
			while (iterator.hasNext())
			{
				Map.Entry<String,String> entry=iterator.next();
				huc.setRequestProperty(entry.getKey(), entry.getValue());
			}
			int code=huc.getResponseCode();
			if(code==301||code==302){
				url=huc.getHeaderField("Location");
				break location;
			}
			Map<String,String> map=new HashMap<>();
			//map.put("Location", huc.getHeaderField("Location"));
			final HttpURLConnection close=huc;
			return new Response(huc.getResponseCode(), huc.getInputStream(), huc.getContentLength(), new Closeable(){

					@Override
					public void close() throws IOException
					{
						close.disconnect();
					}
				}, map);
		}
		catch (IOException e)
		{
			if (huc != null)huc.disconnect();
		}
		return null;
	}

	
}
