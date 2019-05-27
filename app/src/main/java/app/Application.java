package app;
import android.graphics.Bitmap;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;
import java.io.File;
import com.moe.tinyimage.TinyImage;

public class Application extends android.app.Application implements Thread.UncaughtExceptionHandler
{

	@Override
	public void uncaughtException(Thread p1, Throwable p2)
	{
		
	}
	

	@Override
	public void onCreate()
	{
		
		super.onCreate();
		Thread.currentThread().setDefaultUncaughtExceptionHandler(this);
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

	@Override
	public void onTerminate()
	{
		// TODO: Implement this method
		super.onTerminate();
		TinyImage.get(this).clearMemory();
	}

	
}
