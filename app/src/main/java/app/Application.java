package app;
import android.graphics.Bitmap;
import okhttp3.Response;
import okhttp3.Request;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import okhttp3.Protocol;
import okhttp3.Headers;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okhttp3.MediaType;
import okio.Timeout;
import okio.InputStreamSource;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;
import okhttp3.OkHttpClient;
import okhttp3.ConnectionPool;
import java.io.File;
import okhttp3.internal.io.FileSystem;
import okhttp3.Cache;

public class Application extends android.app.Application
{

	@Override
	public void onCreate()
	{
		
		super.onCreate();
		/**final OkHttpClient okhttp=new OkHttpClient.Builder().connectionPool(new ConnectionPool(6,120,TimeUnit.SECONDS)).cache(new Cache(new File(getCacheDir(),"image_cache"),128l*1024l*1024l)).build();
		Picasso.setSingletonInstance(new Picasso.Builder(this).downloader(new Downloader(){
			private HttpURLConnection conn;
											 @Override
											 public Response load(Request p1) throws IOException
											 {
												 return okhttp.newCall(p1).execute();
												/*conn=(HttpURLConnection) p1.url().url().openConnection();
												for(int i=0;i<p1.headers().size();i++){
													conn.setRequestProperty(p1.headers().name(i),p1.headers().value(i));
												}
												conn.setRequestProperty("Conection","Keep-Alive");
												conn.setDoOutput(false);
												conn.setConnectTimeout(15000);
												conn.setReadTimeout(10000);
												Map<String,List<String>> map=conn.getHeaderFields();
												Headers.Builder headers=new Headers.Builder();
												Iterator<String> iterator=map.keySet().iterator();
												while(iterator.hasNext()){
													String key=iterator.next();
													for(String value:map.get(key))
													headers.add(key,value);
													
												}
												return new Response.Builder().code(conn.getResponseCode()).protocol(Protocol.HTTP_1_1).message(conn.getResponseMessage()).headers(headers.build()).body(ResponseBody.create(MediaType.parse(conn.getContentType()),conn.getContentLengthLong(),new okio.RealBufferedSource(new InputStreamSource(conn.getInputStream(),Timeout.NONE)))).build();
											 }

											 @Override
											 public void shutdown()
											 {
												 if(conn!=null)conn.disconnect();
											 }
								 }).memoryCache(new com.squareup.picasso.LruCache((int)Runtime.getRuntime().totalMemory()/2)).executor(new ThreadPoolExecutor(16,32,10,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(128,false))).build());
								 */
	}

	
}
