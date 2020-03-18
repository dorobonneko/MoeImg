package com.moe.pussy.handle;
import com.moe.pussy.RequestHandler;
import com.moe.pussy.RequestHandler.Response;
import com.moe.pussy.Request;
import android.net.Uri;
import java.io.InputStream;
import com.moe.pussy.PussyDrawable;
import java.net.URLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.util.Iterator;
import java.util.Map;
import java.io.File;
import com.moe.pussy.DiskCache;
import java.io.FileInputStream;
import java.util.concurrent.ThreadPoolExecutor;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.concurrent.CancellationException;
import com.moe.pussy.net.ChunkedInputStream;
import java.net.SocketTimeoutException;

public class HttpRequestHandler implements RequestHandler
{

	@Override
	public boolean canHandle(Request request)
	{
		switch (Uri.parse(request.getUrl()).getScheme())
		{
			case "http":
			case "https":
				return true;
		}
		return false;
	}

	@Override
	public void onHandle(ThreadPoolExecutor pool, final Request request, final Callback call)
	{
		pool.execute(new Runnable(){
				public void run()
				{
					try
					{
						Response res=onHandle(request);
						if (res == null)
							throw new NullPointerException();
						call.onSuccess(res);
					}
					catch (Exception e)
					{
						call.onError(e);
					}

					{}
				}});
	}
	private Response onHandle(Request request) throws IOException
	{
		HttpResponse hrs=new HttpResponse();
		HttpURLConnection huc=null;
		OutputStream output=null;
		InputStream input=null;
		DiskCache dc=request.getPussy().getDiskCache();
		File tmp=dc.getDirty(request.getKey());
		boolean chunked=false;
		try
		{
			huc = (HttpURLConnection) new URL(request.getUrl()).openConnection();
			if (huc instanceof HttpsURLConnection)
			{
				SSLSocketFactory ssf=request.getPussy().getSSLSocketFactory();
				if (ssf != null)
					((HttpsURLConnection)huc).setSSLSocketFactory(ssf);
			}
			Iterator<Map.Entry<String,String>> header_i=request.getHeader().entrySet().iterator();
			while (header_i.hasNext())
			{
				Map.Entry<String,String> entry=header_i.next();
				huc.setRequestProperty(entry.getKey(), entry.getValue());
			}
			huc.setRequestProperty("Connection", "close");
			huc.setRequestProperty("Range", "bytes=".concat(tmp.exists() ?String.valueOf(tmp.length()): "0").concat("-"));
			huc.setFollowRedirects(true);
			if (request.isCancel())throw new CancellationException();
			int code=huc.getResponseCode();
			if (request.isCancel())throw new CancellationException();
			String range=huc.getHeaderField("Content-Range");
			long length=huc.getContentLengthLong();
			if (range != null)
				length = Long.parseLong(range.substring(range.indexOf("/") + 1));

			if (code == 416 && (huc.getContentLengthLong() == 0 || tmp.length() == length))
			{
				//huc.disconnect();
				tmp.renameTo(dc.getCache(request.getKey(), true));
				hrs.set(dc.getCache(request.getKey()));
				return hrs;
			}
			if (code == 301 || code == 302)
			{
				String location=huc.getHeaderField("Location");
				if (location != null)
				{
					request.setLocation(location);
					return onHandle(request);
				}
			}
			input = huc.getInputStream();
			if (tmp.length() > 0 && code == 200)
				huc.getInputStream().skip(tmp.length());
			output = new FileOutputStream(tmp, true);
			int len=-1;
			if ("chunked".equalsIgnoreCase(huc.getHeaderField("Transfer-Encoding")))
			{
				chunked = true;
				while ((len = input.read()) != -1)
				{
					output.write(len);
					if (request.isCancel())
						throw new CancellationException();
					//output.flush();
				}
			}
			else
			{
				byte[] buff=new byte[1024 * 12];
				
				while ((len = input.read(buff)) != -1)
				{
					output.write(buff, 0, len);
					if (request.isCancel())
						throw new CancellationException();
					//output.flush();
				}
			}
			output.flush();
			output.close();
			input.close();
			if (tmp.length() == length || chunked)
				tmp.renameTo(dc.getCache(request.getKey(), true));
			if (request.isCancel())
				throw new CancellationException();
			hrs.set(dc.getCache(request.getKey()));
		}
		catch (SocketTimeoutException e)
		{

		}
		catch (CancellationException e)
		{}
		catch (IOException e)
		{
			throw e;
		}
		finally
		{
			if (huc != null)
				huc.disconnect();
		}
		return hrs;
	}
	class HttpResponse extends Response
	{
		private File in;
		public void set(File in)
		{
			this.in = in;
		}
		@Override
		public File get()
		{
			return in;
		}




	}
}
