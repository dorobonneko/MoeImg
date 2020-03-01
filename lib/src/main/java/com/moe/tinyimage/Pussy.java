package com.moe.tinyimage;
import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import com.moe.tinyimage.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import android.util.LruCache;
import android.widget.ImageView;
import java.lang.ref.SoftReference;
import javax.security.auth.callback.Callback;
import android.net.Uri;

public class Pussy
{
	private static Application mApplication;
	Context mContext;
	Resources.Theme theme;
	ThreadPoolExecutor mThreadPoolExecutor;
	private long memorySize=Runtime.getRuntime().maxMemory() / 2,diskCacheSize=256 * 1024 * 1024;
	Map<String,Loader> requestQueue=new ConcurrentHashMap<>();
	String cachePath;
	Handler mHandler=null;
	private ActivityLifecycle mActivityLifecycle=new ActivityLifecycle();
	List<RequestHandler> mRequestHandler;
	LruCache<String,BitmapDecoder> mDecodeCache=new LruCache<String,BitmapDecoder>(512){
		@Override
		protected void entryRemoved(boolean evicted, String key, final BitmapDecoder oldValue, BitmapDecoder newValue)
		{
			if (evicted)
				synchronized (oldValue)
				{
					oldValue.recycle();
				}
		}

	};
	LruCache<String,Bitmap> mMemoryCache=new LruCache<String,Bitmap>((int)memorySize){
		@Override
		protected void entryRemoved(boolean evicted, String key, final Bitmap oldValue, Bitmap newValue)
		{
			if (evicted)
			{
				synchronized (oldValue)
				{
					//if (oldValue instanceof BitmapDrawable)
					//	((BitmapDrawable)oldValue).getBitmap().recycle();
					//oldValue.setCallback(null);
					if(!oldValue.isRecycled())
						synchronized(oldValue){
					oldValue.recycle();
					}
				}
			}
		}

		@Override
		protected int sizeOf(String key, Bitmap value)
		{
			/*if (value instanceof BitmapDrawable)
			 return ((BitmapDrawable)value).getBitmap().getAllocationByteCount();
			 return value.getIntrinsicWidth() * value.getIntrinsicHeight();*/
			return value.getAllocationByteCount();
		}

	};
	private Pussy(Context context)
	{

		this.mContext = context.getApplicationContext();
		this.theme = context.getTheme();
		if (mApplication == null)
		{
			Application application=(Application) context.getApplicationContext();
			mApplication = application;
			application.registerActivityLifecycleCallbacks(mActivityLifecycle);
			application.registerComponentCallbacks(mActivityLifecycle);
		}
		mThreadPoolExecutor = new ThreadPoolExecutor(12, 64, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
		File cache = new File(mContext.getCacheDir(), "pussy_cache");
		cachePath = cache.getAbsolutePath();
		if (!cache.exists())
			cache.mkdirs();
		mRequestHandler = new ArrayList<>();
		mRequestHandler.add(new HttpRequestHandler());
	}

	public Context getContext()
	{
		// TODO: Implement this method
		return mContext;
	}
	private static Map<Context,Pussy> mPussy=new HashMap<>();
	public static Pussy $(Context context)
	{
		Pussy p=mPussy.get(context);
		if (p == null)
		{
			mPussy.put(context, p = new Pussy(context));
		}

		return p;
	}
	Map<String,Loader> requestQueue()
	{
		return requestQueue;
	}
	public void addRequestHandler(RequestHandler requestHandler)
	{
		this.mRequestHandler.add(requestHandler);
	}
	public void requestHandler(RequestHandler... requestHandler)
	{
		this.mRequestHandler.clear();
		this.mRequestHandler.addAll(Arrays.asList(requestHandler));
	}
	public Handler getHandler()
	{
		if (mHandler == null)
			synchronized (this)
			{
				if (mHandler == null)
					mHandler = new Handler(Looper.getMainLooper());

			}
		return mHandler;
	}
	public void trimDiskMemory()
	{
		new Thread(){
			public void run()
			{
				synchronized (cachePath)
				{
					List<File> list=Arrays.asList(new File(cachePath).listFiles());
					long totalLength=0;
					for (File file:list)
						totalLength += file.length();
					if (totalLength < diskCacheSize)return;
					Collections.sort(list, new Comparator<File>(){

							@Override
							public int compare(File p1, File p2)
							{
								return Long.compare(p1.lastModified(), p2.lastModified());
							}
						});
					for (File file:list)
					{
						totalLength -= file.length();
						file.delete();
						if (totalLength < diskCacheSize)
							break;
					}

				}
			}
		}.start();
	}
	public void trimMemory()
	{
		mMemoryCache.trimToSize((int)memorySize / 4);
		System.gc();
	}
	public void clearMemory()
	{
		mMemoryCache.trimToSize(0);
		mDecodeCache.trimToSize(0);
		System.gc();

	}
	public void clearDisk()
	{
		List<File> list=Arrays.asList(new File(cachePath).listFiles());
		for (File file:list)
			file.delete();
	}
	public void release()
	{
		cancelAll();
		mThreadPoolExecutor.shutdown();
		clearMemory();
		mContext = null;
		theme = null;
		mThreadPoolExecutor = null;
		requestQueue.clear();
		mRequestHandler.clear();

	}
	public void cancel(Target t)
	{
		Iterator<Map.Entry<String,Loader>> iterator=requestQueue.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<String,Loader> entry= iterator.next();
			entry.getValue().remove(t);
		}
	}
	public void cancel(String url)
	{
		String key=Utils.encode(url);
		Loader loader=requestQueue.get(key);
		if (loader == null)return;
		loader.cancel();
		loader.clear();
	}
	public void cancelAll(){
		Iterator<Loader> i=(Iterator<Loader>) requestQueue.values().iterator();
		while(i.hasNext()){
			Loader l=i.next();
			l.cancel();
			l.clear();
		}
	}
	public Request.Builder load(String url)
	{
		return new Request.Builder(this, url);
	}



	public interface TransForm
	{
		boolean canDecode();
		Bitmap onTransForm(Bitmap source, int w, int h);
		Bitmap onTransForm(BitmapDecoder brd, BitmapFactory.Options options, int w, int h);
		public String key();


	}
	public static class TinyBitmapDrawable extends BitmapDrawable implements Animatable
	{
		private Anim fadeAnimator;
		private Target t;

		public TinyBitmapDrawable(Target t, Context context, Bitmap bitmap, Anim anim)
		{
			super(context.getResources(), bitmap);
			this.t = t;
			fadeAnimator = anim;

		}
		public boolean isAnim()
		{
			return fadeAnimator != null;
		}
		public static Drawable create(Target t, Bitmap bitmap)
		{
			if(t.getRequest().getPussy().getContext()==null)return null;
			Drawable d= new TinyBitmapDrawable(t, t.getRequest().getPussy().getContext(), bitmap, t.getRequest().getAnim());
			if (t.getRequest().getAnim() != null)
				return t.getRequest().getAnim().setDrawable(d);
			return d;
		}

		@Override
		public void start()
		{
			if (fadeAnimator != null)
				fadeAnimator.start();
		}

		@Override
		public void stop()
		{
			if (fadeAnimator != null)
				fadeAnimator.stop();
		}

		@Override
		public boolean isRunning()
		{
			if (fadeAnimator != null)return fadeAnimator.isRunning();
			return false;
		}




		@Override
		public void draw(Canvas canvas)
		{
			Bitmap bitmap=getBitmap();
			if (bitmap == null || bitmap.isRecycled())
			{
				if (getCallback() != null)
				{
					//刷新链接
					t.getRequest().getPussy().requestQueue().get(t.getRequest().getCacheKey()).add(t);

				}
			}
			else
				synchronized(bitmap){
					if(!bitmap.isRecycled())
				try{super.draw(canvas);}catch(Exception e){}
				}
		}

	}
	static class PlaceHolderDrawable extends Drawable
	{
		private int width,height,color;
		private float radius;
		private Paint paint;
		public PlaceHolderDrawable(int width, int height, int color, float radius)
		{
			this.width = width;
			this.height = height;
			this.color = color;
			this.radius = radius;
			paint = new Paint();
			paint.setColor(color);
		}
		@Override
		public void draw(Canvas p1)
		{
			p1.drawRoundRect(new RectF(getBounds()), radius, radius, paint);
		}

		@Override
		public void setAlpha(int p1)
		{
			paint.setAlpha(p1);
		}

		@Override
		public void setColorFilter(ColorFilter p1)
		{
			paint.setColorFilter(p1);
		}

		@Override
		public int getOpacity()
		{
			// TODO: Implement this method
			return PixelFormat.RGBA_8888;
		}

		@Override
		public int getIntrinsicWidth()
		{
			// TODO: Implement this method
			return width;
		}

		@Override
		public int getIntrinsicHeight()
		{
			// TODO: Implement this method
			return width;
		}
	}
	class ActivityLifecycle implements Application.ActivityLifecycleCallbacks,ComponentCallbacks
	{

		@Override
		public void onConfigurationChanged(Configuration p1)
		{
			// TODO: Implement this method
		}

		@Override
		public void onLowMemory()
		{
			for (Pussy p:mPussy.values())
				p.clearMemory();
		}


		@Override
		public void onActivityCreated(Activity p1, Bundle p2)
		{
			// TODO: Implement this method
		}

		@Override
		public void onActivityStarted(Activity p1)
		{
			// TODO: Implement this method
		}

		@Override
		public void onActivityResumed(Activity p1)
		{
			// TODO: Implement this method
		}

		@Override
		public void onActivityPaused(Activity p1)
		{
			// TODO: Implement this method
		}

		@Override
		public void onActivityStopped(Activity p1)
		{
			/*Pussy p=mPussy.get(p1);
			if (p != null)
				p.trimMemory();*/
		}

		@Override
		public void onActivitySaveInstanceState(Activity p1, Bundle p2)
		{
			Pussy p=mPussy.get(p1);
			if (p != null)
				p.trimMemory();
		}

		@Override
		public void onActivityDestroyed(Activity p1)
		{
			Pussy p=mPussy.remove(p1);
			if (p != null){
				p.trimDiskMemory();
				p.release();
				}

		}
	}
	public static class BitmapCallback
	{
		private File cacheFile;
		private SoftReference<BitmapDecoder> brd;
		private SoftReference<Bitmap> drawable;
		private String builderKey;
		private Pussy mPussy;
		public BitmapCallback(Pussy pussy, File cacheFile, String key, BitmapDecoder brd)
		{
			this.mPussy = pussy;
			this.cacheFile = cacheFile;
			this.builderKey = key;
			this.brd = new SoftReference<BitmapDecoder>(brd);
		}
		public File getCacheFile()
		{
			return cacheFile;
		}
		public void setBitmap(Bitmap bitmap)
		{
			this.drawable = new SoftReference<Bitmap>(bitmap);
			mPussy.mMemoryCache.put(builderKey, bitmap);
		}
		public BitmapDecoder getBitmapDecode()
		{
			return brd.get();
		}
		public Bitmap getBitmap()
		{
			if (drawable == null)
			{
				Bitmap d=mPussy.mMemoryCache.get(builderKey);
				if (d != null)
				{
					drawable = new SoftReference<Bitmap>(d);
					return d;
				}
				return null;
			}
			return drawable.get();
		}
	}


	public static interface RequestHandler
	{
		boolean canHandle(Uri uri);
		Result load(Uri uri, Map<String,String> header);
		interface Result
		{}
		class Image implements Result
		{
			private Bitmap mBitmap;
			private Drawable mDrawable;


			public void setMBitmap(Bitmap mBitmap)
			{
				this.mBitmap = mBitmap;
			}

			public Bitmap getBitmap()
			{
				return mBitmap;
			}

			public void setMDrawable(Drawable mDrawable)
			{
				this.mDrawable = mDrawable;
			}

			public Drawable getDrawable()
			{
				return mDrawable;
			}}
		class Response implements Result
		{
			private int code;
			private InputStream input;
			private long length;
			private Closeable close;
			private Map<String,String> map;
			public Response(int code, InputStream is, long length, Closeable close, Map<String,String> map)
			{
				this.code = code;
				this.input = is;
				this.length = length;
				this.close = close;
				this.map = map;
			}
			public int code()
			{
				return code;
			}
			public InputStream inputStream()
			{
				return input;
			}
			public long length()
			{
				return length;
			}
			public void close()
			{
				try
				{
					if (close != null)close.close();
				}
				catch (IOException e)
				{}
			}
			public String header(String key)
			{
				if (map == null) return null;
				return map.get(key);
			}
		}
	}
}
