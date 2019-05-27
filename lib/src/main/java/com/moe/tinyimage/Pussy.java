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
import java.lang.annotation.Target;
import java.lang.ref.SoftReference;
import javax.security.auth.callback.Callback;
import android.net.Uri;

public class Pussy
{
	private static Pussy mPussy;
	private Context mContext;
	private Resources.Theme theme;
	private ThreadPoolExecutor mThreadPoolExecutor;
	private long memorySize=Runtime.getRuntime().maxMemory() / 2,diskCacheSize=128 * 1020 * 1020;
	private Map<String,Loader> requestQueue=new ConcurrentHashMap<>();
	private String cachePath;
	private Handler mHandler=null;
	private ActivityLifecycle mActivityLifecycle=new ActivityLifecycle();
	private List<RequestHandler> mRequestHandler;
	private LruCache<String,BitmapRegionDecoder> mDecodeCache=new LruCache<String,BitmapRegionDecoder>(64){
		@Override
		protected void entryRemoved(boolean evicted, String key, final BitmapRegionDecoder oldValue, BitmapRegionDecoder newValue)
		{
			if (evicted)
				synchronized (oldValue)
				{
					oldValue.recycle();
				}
		}

	};
	private LruCache<String,Bitmap> mMemoryCache=new LruCache<String,Bitmap>((int)memorySize){
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
					oldValue.recycle();
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
		Application application=(Application) context.getApplicationContext();
		application.registerActivityLifecycleCallbacks(mActivityLifecycle);
		application.registerComponentCallbacks(mActivityLifecycle);
		mThreadPoolExecutor = new ThreadPoolExecutor(6, 12, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
		File cache = new File(mContext.getCacheDir(), "pussy_cache");
		cachePath=cache.getAbsolutePath();
		if(!cache.exists())
		cache.mkdirs();
		mRequestHandler=new ArrayList<>();
		mRequestHandler.add(new HttpRequestHandler());
	}

	public Context getContext()
	{
		// TODO: Implement this method
		return mContext;
	}
	public static Pussy get(Context context)
	{
		if (mPussy == null)
			synchronized (Pussy.class)
			{
				if (mPussy == null)
					mPussy = new Pussy(context);
			}
		return mPussy;
	}
	Map<String,Loader> requestQueue(){
		return requestQueue;
	}
	public void addRequestHandler(RequestHandler requestHandler)
	{
		this.mRequestHandler.add(requestHandler);
	}
	public void requestHandler(RequestHandler... requestHandler){
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
		if(loader==null)return;
		loader.cancel();
		loader.clear();
	}
	public Request.Builder load(String url)
	{
		return new Request.Builder(this, url);
	}
	public static class Request
	{
		private Builder mBuilder;
		private String key,cacheKey;
		private boolean mCanceled;
		Request(Builder mBuilder)
		{
			this.mBuilder = mBuilder;
		}
		String getCacheKey()
		{
			if (cacheKey == null)
				cacheKey = Utils.encode(getUrl());
			return cacheKey;
		}
		public String getUrl()
		{
			return mBuilder.url;
		}
		public boolean isFade()
		{
			return mBuilder.fade;
		}

		public boolean isCanceled()
		{
			return mCanceled;
		}
		public void cancel()
		{
			mCanceled = true;
		}
		public Drawable getPlaceHolder()
		{
			return mBuilder.placeHolder == 0 ?mBuilder.placeHolderDrawable: getPussy().mContext.getResources().getDrawable(mBuilder.placeHolder, getPussy().theme);
		}
		public Drawable getError()
		{
			return mBuilder.error == 0 ?mBuilder.errorDrawable: getPussy().mContext.getResources().getDrawable(mBuilder.error, getPussy().theme);
		}
		public String getKey()
		{
			if (key == null)
			{
				StringBuilder sb=new StringBuilder();
				sb.append(mBuilder.url).append(isFade()).append(getPlaceHolder() == null).append(getError() == null).append(getConfig());
				TransForm[] tf=getTransForm();
				if (tf != null)
					for (TransForm trans:tf)
						sb.append(trans.key());
				key = Utils.encode(sb.toString());
			}
			return key;
		}
		public Bitmap.Config getConfig()
		{
			return mBuilder.config;
		}
		public TransForm[] getTransForm()
		{
			return mBuilder.mTransForm;
		}
		public Pussy getPussy()
		{
			return mBuilder.mPussy;
		}
		public static class Builder
		{
			private int placeHolder,error;
			private TransForm[] mTransForm;
			private Drawable placeHolderDrawable,errorDrawable;
			private Bitmap.Config config=Bitmap.Config.RGB_565;
			private boolean fade=true;

			private String url;
			private Pussy mPussy;
			public Builder(Pussy pussy, String url)
			{
				this.mPussy = pussy;
				this.url = url;
			}
			public void into(ImageView view)
			{
				Target t=(Pussy.Target) view.getTag();
				if (t == null)
					view.setTag(t = new ImageViewTarget(view));
				into(t);
			}
			public void into(Target target)
			{
				mPussy.cancel(target);
				target.setRequest(target.onHandleRequest(this));
				Loader loader=mPussy.requestQueue.get(target.getRequest().getCacheKey());
					if(loader==null)
						mPussy.requestQueue.put(target.getRequest().getCacheKey(),loader=new Loader(mPussy,url));
					loader.add(target);
				
			}
			public void preload(){
				Request request=new Request(this);
				Loader loader=mPussy.requestQueue.get(request.getCacheKey());
				if(loader==null)
					mPussy.requestQueue.put(request.getCacheKey(),loader=new Loader(mPussy,url));
				loader.preload();
			}
			public Builder noFade()
			{
				this.fade = false;
				return this;
			}
			public Builder config(Bitmap.Config config)
			{
				this.config = config;
				return this;
			}
			
			public Builder placeHolder(int width, int height, int color, float radius)
			{
				return placeHolder(new PlaceHolderDrawable(width, height, color, radius));
			}
			public Builder placeHolder(Drawable placeHolder)
			{
				this.placeHolder = 0;
				placeHolderDrawable = placeHolder;
				return this;
			}
			public Builder placeHolder(int resId)
			{
				placeHolderDrawable = null;
				placeHolder = resId;
				return this;
			}
			public Builder error(Drawable error)
			{
				this.error = 0;
				this.errorDrawable = error;
				return this;
			}
			public Builder error(int resId)
			{
				this.error = resId;
				return this;
			}
			public Builder transForm(TransForm... trans)
			{
				this.mTransForm = trans;
				return this;
			}
		}
	}
	public static abstract class Target
	{
		Request onHandleRequest(Request.Builder builder)
		{
			return new Request(builder);
		}
		private Request mRequest;
		private TinyImage mTinyImage;
		void setTinyImage(TinyImage tiny)
		{
			this.mTinyImage = tiny;
		}
		public void onResourceReady(final BitmapCallback bc)
		{
			final BitmapRegionDecoder brd=bc.getBitmapDecode();
			synchronized (getRequest().getKey())
			{
				Bitmap bitmap=getRequest().getPussy().mMemoryCache.get(getRequest().getKey());
				if (bitmap != null)
				{
					onLoadSuccess(Pussy.TinyBitmapDrawable.create(this,bc.getBitmap()));
				}
				else
				{
					new Thread(){
						public void run()
						{
							synchronized (getRequest().getKey())
							{
								BitmapFactory.Options bo=new BitmapFactory.Options();
								bo.inPreferredConfig = getRequest().getConfig();
								bo.inTargetDensity = getRequest().getPussy().mContext.getResources().getDisplayMetrics().densityDpi;
								bo.inScaled = true;
								bo.inDensity = 160;
								Bitmap bitmap= onTransForm(brd, bo, brd.getWidth(), brd.getHeight());
								bc.setBitmap(bitmap);

								getRequest().getPussy().getHandler().post(new Runnable(){

										@Override
										public void run()
										{
											if (getRequest().isCanceled())return;
											onLoadSuccess(Pussy.TinyBitmapDrawable.create(Target.this,bc.getBitmap()));
										}
									});
							}
						}
					}.start();
				}
			}
		}
		abstract public void onLoadSuccess(Drawable d);
		abstract public void onLoadFailed(Exception e, Drawable d);
		abstract public void onLoadPrepared(Drawable d);
		abstract public void onProgressChanged(int progress);
		abstract public void onLoadCleared();
		void setRequest(Request request)
		{
			this.mRequest = request;
		}
		public Request getRequest()
		{
			return mRequest;
		}
		Bitmap onTransForm(BitmapRegionDecoder brd, BitmapFactory.Options options, int width, int height)
		{
			if (getRequest().getTransForm() == null)return brd.decodeRegion(new Rect(0, 0, brd.getWidth(), brd.getHeight()), options);
			Bitmap bitmap=null;
			for (TransForm tf:getRequest().getTransForm())
			{
				if (tf.canDecode())
					bitmap = tf.onTransForm(brd, options, width, height);
				else
				{
					if (bitmap == null)bitmap = brd.decodeRegion(new Rect(0, 0, brd.getWidth(), brd.getHeight()), options);
					bitmap = tf.onTransForm(bitmap, width, height);
				}
			}
			return bitmap;
		}
		public Bitmap onTransForm(Bitmap bitmap,int width,int height){
			if (getRequest().getTransForm() == null)
				return bitmap;
			for (TransForm tf:getRequest().getTransForm())
			{
				bitmap = tf.onTransForm(bitmap, width, height);
			}
			return bitmap;
		}
		public void onResourceReady(Drawable drawable){
			onLoadSuccess(TinyBitmapDrawable.create(this,onTransForm(Utils.drawable2Bitmap(drawable),drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight())));
		}
	}
	private static class Loader implements Runnable
	{
		private List<Target> calls=new ArrayList<>();
		private String url,key;
		private Pussy mPussy;
		private boolean preload;
		public Loader(Pussy pussy, String url)
		{
			this.mPussy = pussy;
			this.url = url;
			key = Utils.encode(url);
		}

		public void preload()
		{
			preload=true;
			BitmapRegionDecoder brd=mPussy.mDecodeCache.get(key);
			if (brd == null)
			{
				mPussy.mThreadPoolExecutor.execute(this);
			}
			}
		public void cancel(){
			preload=false;
		}
		public void clear(){
			synchronized(calls){
			Iterator<Target> targets=calls.iterator();
			while(targets.hasNext()){
				targets.next().getRequest().cancel();
				targets.remove();
			}
			}
		}
		public void add(Target target)
		{
			synchronized (calls)
			{
				if (!calls.contains(target))
					calls.add(target);
				Bitmap image=target.getRequest().getPussy().mMemoryCache.get(target.getRequest().getKey());
				if(image!=null){
					target.onLoadSuccess(Pussy.TinyBitmapDrawable.create(target,image));
					return;
				}
				target.onLoadPrepared(target.getRequest().getPlaceHolder());
				BitmapRegionDecoder brd=mPussy.mDecodeCache.get(key);
				if (brd != null)
				{
					onPrepared(target, brd);
				}
				else
				{
					mPussy.mThreadPoolExecutor.execute(this);
				}
			}
		}
		public void remove(Target target)
		{
			synchronized (calls)
			{
				int index=calls.indexOf(target);
				if(index!=-1){
					calls.remove(index).getRequest().cancel();
				}
			}
		}
		public boolean contains(Target t)
		{
			synchronized (calls)
			{
				return calls.contains(t);
			}
		}
		public boolean isCanceled()
		{
			return calls.isEmpty()&&!preload;
		}
		@Override
		public synchronized void run()
		{
			if(mPussy.mDecodeCache.get(key)!=null)return;
			File file_src=new File(mPussy.cachePath, key);
			if (file_src.exists() && file_src.isFile())
			{
				onSuccess();
				return;
			}
			Uri uri=Uri.parse(url);
			RequestHandler.Result res=null;
			OutputStream output=null;
			InputStream input=null;
			//int error=0;
			File file=new File(mPussy.cachePath, key.concat(".tmp"));
			
			try
			{
				if (isCanceled())
					throw new IllegalStateException();
				Map<String,String> header=new HashMap<String,String>();
				header.put("Range", "bytes=".concat(String.valueOf(file.length()).concat("-")));
				header.put("Connection", "Keep-Alive");
				header.put("User-Agent", "TinyImage:version=1");
				for(RequestHandler rh:mPussy.mRequestHandler.toArray(new RequestHandler[0])){
					if(rh.canHandle(uri))
					{
						res = rh.load(uri, header);
						if(res!=null)break;
					}
				}
				if (res == null)throw new IOException();
				if (isCanceled())
					throw new IllegalStateException();
					if(res instanceof RequestHandler.Image){
						
					}else if(res instanceof RequestHandler.Response){
						onProcessResponse((RequestHandler.Response)res,file,file_src);
					}
				
			}
			catch (Exception e)
			{
				if (e instanceof IllegalStateException)
				{
					return;
				}
				//error++;
				//if (error >= 2)
				onFail(e);
				//else
				//break out;
			}
			finally
			{
				try
				{
					if (input != null)input.close();
				}
				catch (IOException e)
				{}
				try
				{
					if (output != null)output.close();
				}
				catch (IOException e)
				{}
				
			}
		}
		void onProcessImage(final RequestHandler.Image image){
			if(image==null||(image.getBitmap()==null&&image.getDrawable()==null))return;
			final Iterator<Target> iterator=calls.iterator();
			while(iterator.hasNext()){
				final Target t=iterator.next();
				mPussy.getHandler().post(new Runnable(){
						public void run(){
							t.onResourceReady(image.getBitmap()==null?image.getDrawable():new BitmapDrawable(image.getBitmap()));
						}
					});
				}
		}
		void onProcessResponse(RequestHandler.Response res,File temp_file,File src) throws Exception{
			
			OutputStream output=null;
			InputStream input=null;
			try{
			switch (res.code())
			{
				case 200:
					output = new FileOutputStream(temp_file, false);
					break;
				case 206:
					output = new FileOutputStream(temp_file, true);
					break;
				case 301:
				case 302:
					//url = res.header("Location");
					//break out;
				default:throw new IOException(String.valueOf(res.code()));
			}
			input = res.inputStream();
			if (isCanceled())
				throw new IllegalStateException();
			byte[] buffer=new byte[8192];
			int len=-1;
			while ((len = input.read(buffer)) != -1)
			{
				output.write(buffer, 0, len);
				output.flush();
				if (isCanceled())
					throw new IllegalStateException();
			}
			temp_file.renameTo(src);
			onSuccess();
			}catch(Exception e){
				throw e;
			}finally{
				res.close();
				if(input!=null)input.close();
				if(output!=null)
					output.close();
			}
		}
		public void onPrepared(Target t, BitmapRegionDecoder brd)
		{
			t.onResourceReady(new BitmapCallback(mPussy,new File(mPussy.cachePath, key), t.getRequest().getKey(), brd));
		}
		public synchronized void onSuccess()
		{
			BitmapRegionDecoder brd=mPussy.mDecodeCache.get(key);
			if (brd == null)
			{
				File cacheFile=new File(mPussy.cachePath, key);
				if (cacheFile.exists())
				{
					try
					{
						brd = BitmapRegionDecoder.newInstance(cacheFile.getAbsolutePath(), false);
						mPussy.mDecodeCache.put(key,brd);
					}
					catch (IOException e)
					{
						onFail(e);
						return;
					}
				}
				else
				{
					mPussy.mThreadPoolExecutor.execute(this);
					return;
				}
			}
			final BitmapRegionDecoder final_brd=brd;
			final Iterator<Target> iterator=calls.iterator();
			while(iterator.hasNext()){
				final Target t=iterator.next();
				mPussy.getHandler().post(new Runnable(){
					public void run(){
				onPrepared(t,final_brd);
				}
				});
			}
		}
		public void onFail(final Exception e){
			Iterator<Target> iterator=calls.iterator();
			while(iterator.hasNext()){
				final Target t=iterator.next();
				mPussy.getHandler().post(new Runnable(){
					public void run(){
				t.onLoadFailed(e,t.getRequest().getError());
				}
				});
			}
		}
	}
	public static class Utils
	{
		public static String encode(String data)
		{
			try
			{
				return byte2HexStr(MessageDigest.getInstance("MD5").digest(data.getBytes()));
			}
			catch (NoSuchAlgorithmException e)
			{
				return Base64.getEncoder().encodeToString(data.getBytes());
			}
		}
		public static int calculateInSampleSize(int width, int height, float reqWidth, float reqHeight)
		{
			int inSampleSize = 1;
			if (height > reqHeight || width > reqWidth)
			{
				final int halfHeight = height;
				final int halfWidth = width;
				while ((halfHeight / inSampleSize) > reqHeight
					   && (halfWidth / inSampleSize) > reqWidth)
				{
					inSampleSize *= 2;
				}
			}
			return inSampleSize;
		}
		public static String byte2HexStr(byte[] b)
		{
			String stmp = "";
			StringBuilder sb = new StringBuilder("");
			for (int n = 0; n < b.length; n++)
			{
				stmp = Integer.toHexString(b[n] & 0xFF);
				sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
			}
			return sb.toString();
		}
		public static Bitmap drawable2Bitmap(Drawable drawable) {
			if(drawable instanceof BitmapDrawable)
				return ((BitmapDrawable)drawable).getBitmap();
			Bitmap bitmap = Bitmap
				.createBitmap(
				drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight(),
				drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
				: Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
							   drawable.getIntrinsicHeight());
			drawable.draw(canvas);
			return bitmap;
		}
	}
	public interface TransForm
	{
		boolean canDecode();
		Bitmap onTransForm(Bitmap source, int w, int h);
		Bitmap onTransForm(BitmapRegionDecoder brd, BitmapFactory.Options options, int w, int h);
		public String key();


	}
	public static class TinyBitmapDrawable extends BitmapDrawable implements Animatable
	{
		private boolean fade;
		private Animator fadeAnimator;
		private Target t;
		public TinyBitmapDrawable(Target t,Context context, Bitmap bitmap)
		{
			this(t,context, bitmap, true);
		}
		
		public TinyBitmapDrawable(Target t,Context context, Bitmap bitmap, boolean fade)
		{
			super(context.getResources(), bitmap);
			this.t=t;
			this.fade = fade;
			if (fade)
			{
				fadeAnimator = ObjectAnimator.ofInt(this, "Alpha", 0, 255);
				fadeAnimator.setDuration(300);
			}
		}
		public static TinyBitmapDrawable create(Target t, Bitmap bitmap)
		{
			return new TinyBitmapDrawable(t,t.getRequest().getPussy().getContext(), bitmap, t.getRequest().isFade());
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
				fadeAnimator.cancel();
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
			if (getBitmap().isRecycled())
			{
				if (getCallback() != null)
				{
					//刷新链接
					t.getRequest().getPussy().requestQueue().get(t.getRequest().getCacheKey()).add(t);
					
				}
			}
			else
				super.draw(canvas);
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
			clearMemory();
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
			trimMemory();
		}

		@Override
		public void onActivitySaveInstanceState(Activity p1, Bundle p2)
		{
			// TODO: Implement this method
		}

		@Override
		public void onActivityDestroyed(Activity p1)
		{
			trimDiskMemory();
		}
	}
	public static class BitmapCallback
	{
		private File cacheFile;
		private SoftReference<BitmapRegionDecoder> brd;
		private SoftReference<Bitmap> drawable;
		private String builderKey;
		private Pussy mPussy;
		public BitmapCallback(Pussy pussy,File cacheFile, String key, BitmapRegionDecoder brd)
		{
			this.mPussy=pussy;
			this.cacheFile = cacheFile;
			this.builderKey = key;
			this.brd = new SoftReference<BitmapRegionDecoder>(brd);
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
		public BitmapRegionDecoder getBitmapDecode()
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
	
	
public static interface RequestHandler{
	boolean canHandle(Uri uri);
	Result load(Uri uri, Map<String,String> header);
	interface Result{}
	class Image implements Result{
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
