package com.moe.tinyimage;
import java.util.Map;
import java.util.HashMap;
import android.content.Context;
import java.util.concurrent.ThreadPoolExecutor;
import android.os.Handler;
import android.os.Looper;
import android.graphics.BitmapRegionDecoder;
import android.util.LruCache;
import android.graphics.Bitmap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.File;
import android.widget.ImageView;
import java.util.Iterator;
import java.security.NoSuchAlgorithmException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;
import android.graphics.drawable.Drawable;
import android.graphics.BitmapFactory;
import android.view.ViewTreeObserver;
import android.graphics.Rect;
import android.view.ViewGroup;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Matrix;
import android.graphics.Canvas;
import android.view.Gravity;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.BitmapShader;
import android.graphics.RectF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;
import android.graphics.ColorFilter;
import android.app.Application;
import android.os.Bundle;
import android.app.Activity;
import android.content.ComponentCallbacks;
import android.content.res.Configuration;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import android.graphics.PixelFormat;
import android.os.Message;
import java.io.Closeable;
import com.moe.tinyimage.TinyImage.Downloader.Response;
import java.net.URL;
import java.net.HttpURLConnection;
import java.lang.ref.SoftReference;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.DrawFilter;
import android.graphics.BitmapFactory.Options;

public class TinyImage
{
	//private Map<ImageView,Builder> map=new HashMap<>();
	private long memorySize=Runtime.getRuntime().maxMemory() / 2,diskCacheSize=128 * 1020 * 1020;
	private Map<String,Callback> requestQueue=new HashMap<>();
	//线程池，同时处理6条数据，最大128个任务
	private SoftReference<Context> mContext;
	private static TinyImage mTinyImage;
	private ThreadPoolExecutor mThreadPoolExecutor;
	//private OkHttpClient mOkHttpClient;
	private String cachePath;
	private Handler mHandler=new Handler(Looper.getMainLooper());
	private ActivityLifecycle mActivityLifecycle=new ActivityLifecycle();
	private Downloader mDownloader;
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
	private LruCache<String,Drawable> mMemoryCache=new LruCache<String,Drawable>((int)memorySize){
		@Override
		protected void entryRemoved(boolean evicted, String key, final Drawable oldValue, Drawable newValue)
		{
			if (evicted){
				synchronized(oldValue){
				if(oldValue instanceof BitmapDrawable)
				((BitmapDrawable)oldValue).getBitmap().recycle();
				//oldValue.setCallback(null);
				}
			}
		}

		@Override
		protected int sizeOf(String key, Drawable value)
		{
			if(value instanceof BitmapDrawable)
				return ((BitmapDrawable)value).getBitmap().getAllocationByteCount();
			return value.getIntrinsicWidth()*value.getIntrinsicHeight();
		}

	};

	private TinyImage(Context context)
	{
		this.mContext = new SoftReference<Context>(context.getApplicationContext());
		Application mApplication=((Application)this.mContext.get());
		mApplication.registerActivityLifecycleCallbacks(mActivityLifecycle);
		mApplication.registerComponentCallbacks(mActivityLifecycle);
		mThreadPoolExecutor = new ThreadPoolExecutor(32, 64, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
		//mOkHttpClient = new OkHttpClient.Builder().connectionPool(new ConnectionPool(6, 5, TimeUnit.MINUTES)).followRedirects(true).followSslRedirects(true).build();
		File cache=new File(context.getCacheDir(), "tiny_image_cache");
		if (!cache.exists())cache.mkdirs();
		cachePath = cache.getAbsolutePath();
	}
	public static TinyImage get(Context context)
	{
		if (mTinyImage == null)
			synchronized (TinyImage.class)
			{
				if (mTinyImage == null)mTinyImage = new TinyImage(context);
			}
		return mTinyImage;
	}
	public void downloader(Downloader mDownloader)
	{
		this.mDownloader = mDownloader;
	}
	public Builder load(String url, ImageView view)
	{
		cancel(url);
		if (mDownloader == null)mDownloader = new UrlDownloader();
		String key=Utils.encode(url);
		Callback call=requestQueue.get(key);
		Target t=(TinyImage.Target) view.getTag();
		if (t == null)
			view.setTag(t = new ImageViewTarget(view));
		t.setKey(key);
		t.setTinyImage(this);
		Builder b=new Builder(url, key);
		if (call == null)
		{
			call = new Callback();
			requestQueue.put(key, call); 
		}
		call.setBuilder(b);
		call.setTarget(t);
		t.setBuilder(b);
		return b;
	}
	public Builder load(String url,Target target){
		cancel(url);
		if (mDownloader == null)mDownloader = new UrlDownloader();
		String key=Utils.encode(url);
		Callback call=requestQueue.get(key);
		target.setKey(key);
		target.setTinyImage(this);
		Builder b=new Builder(url, key);
		if (call == null)
		{
			call = new Callback();
			requestQueue.put(key, call); 
		}
		call.setBuilder(b);
		call.setTarget(target);
		target.setBuilder(b);
		return b;
	}
	public void cancel(String url){
		Callback call=requestQueue.get(Utils.encode(url));
		if(call!=null)
			call.cancel();
	}
	public void cancel(Target target){
		Iterator<Callback> iterator=requestQueue.values().iterator();{
			while (iterator.hasNext())
			{
				Callback call=iterator.next();
				if (call.getTarget() instanceof ImageViewTarget)
				{
					if (((ImageViewTarget)call.getTarget()) == target)
						call.cancel();
				}
			}

		}
	}
	public void cancel(ImageView view)
	{
		Iterator<Callback> iterator=requestQueue.values().iterator();{
			while (iterator.hasNext())
			{
				Callback call=iterator.next();
				if (call.getTarget() instanceof ImageViewTarget)
				{
					if (((ImageViewTarget)call.getTarget()).getView() == view)
						call.cancel();
				}
			}

		}
	}
	public void invalidate(ImageView view)
	{
		Iterator<Callback> iterator=requestQueue.values().iterator();{
			while (iterator.hasNext())
			{
				Callback call=iterator.next();
				if (call.getTarget() instanceof ImageViewTarget)
				{
					if (((ImageViewTarget)call.getTarget()).getView() == view)
						call.getBuilder().commit();
				}
			}

		}
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
								// TODO: Implement this method
								return Long.compare(p1.lastModified(), p2.lastModified());
							}
						});
					for (File file:list)
					{
						totalLength -= file.length();
						file.delete();
						if (totalLength < diskCacheSize / 2)
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
	public class Builder
	{
		private String url,key;
		private int placeHolder,error;
		private TransForm[] mTransForm;
		private String builderKey;
		private Drawable placeHolderDrawable,errorDrawable;
		private Bitmap.Config config=Bitmap.Config.RGB_565;
		public Builder(String url, String key)
		{
			this.url = url;
			this.key = key;
		}
		/*public ImageView getView(){
		 return view;
		 }*/
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
		public String getKey()
		{
			return key;
		}
		public String getBuilderKey()
		{
			if (builderKey == null)
			{
				StringBuilder sb=new StringBuilder();
				sb.append(url).append(placeHolder).append(error);
				if (mTransForm != null)
				{
					for (TransForm tf:mTransForm)
						sb.append("TransForm:").append(tf.key()).append(" ");
				}
				builderKey = Utils.encode(sb.toString());
			}
			return builderKey;
		}
		public void commit()
		{
			invalidate(null);
		}
		void invalidate(Target target)
		{
			Callback call=requestQueue.get(key);
			if (target != null)
			{
				call.setTarget(target);
				target.setBuilder(this);
			}
			BitmapRegionDecoder brd=mDecodeCache.get(getKey());
			if (brd != null)
			{
				call.getTarget().onResourceReady(new BitmapCallback(new File(cachePath,getKey()),getBuilderKey(), brd));
				return;
			}

			call.getTarget().onLoadPrepared(placeHolder == 0 ?placeHolderDrawable: mContext.get().getResources().getDrawable(placeHolder));

			Execute execute=new Execute(url, cachePath, call);
			call.setFuture(mThreadPoolExecutor.submit(execute));
			execute.setFuture(call.mFuture);
		}
		public String getUrl()
		{
			return url;
		}

		public Builder transForm(TransForm... trans)
		{
			this.mTransForm = trans;
			return this;
		}
	}
	class Execute implements Runnable
	{
		private String url,cacheDir,key;
		private Callback mCallback;
		private Future mFuture;
		public Execute(String url, String cacheDir, Callback call)
		{
			this.url = url;
			this.cacheDir = cacheDir;
			this.mCallback = call;
			key = call.getBuilder().getKey();
		}
		void setFuture(Future future)
		{
			this.mFuture = future;
		}
		boolean isCancelled()
		{
			if (mFuture != null)
				return mFuture.isCancelled();
			return false;
		}
		public String getKey()
		{
			return key;
		}
		@Override
		public void run()
		{
			synchronized (mCallback)
			{
				File file_src=new File(cacheDir, key);
				if (file_src.exists()&&file_src.isFile())
				{
					mCallback.onSuccess();
					return;
				}
				File file=new File(cacheDir, key.concat(".tmp"));
				Response res=null;
				OutputStream output=null;
				InputStream input=null;
				//int error=0;
				out:
				try
				{
					if (isCancelled())
						throw new IllegalStateException();
					Map<String,String> header=new HashMap<String,String>();
					header.put("Range", "bytes=".concat(String.valueOf(file.length()).concat("-")));
					header.put("Connection", "Keep-Alive");
					header.put("User-Agent","TinyImage:version=1");
					res = mDownloader.load(url, header);
					if (res == null)throw new IOException();
					if (isCancelled())
						throw new IllegalStateException();
					switch (res.code())
					{
						case 200:
							output = new FileOutputStream(file, false);
							break;
						case 206:
							output = new FileOutputStream(file, true);
							break;
						case 301:
						case 302:
							url = res.header("Location");
							break out;
						default:throw new IOException(String.valueOf(res.code()));
					}
					input = res.inputStream();
					if (isCancelled())
						throw new IllegalStateException();
					byte[] buffer=new byte[8192];
					int len=-1;
					while ((len = input.read(buffer)) != -1)
					{
						output.write(buffer, 0, len);
						output.flush();
						if (isCancelled())
							throw new IllegalStateException();
					}
					file.renameTo(file_src);
					mCallback.onSuccess();
				}
				catch (Exception e)
				{
					if (e instanceof IllegalStateException)
					{
						mCallback.onFail(e);
						return;
					}
					//error++;
					//if (error >= 2)
					mCallback.onFail(e);
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
					if (res != null)res.close();
				}
			}
		}


	}
	class Callback
	{
		private Builder mBuilder;
		private Target mTarget;
		private Future mFuture;
		public void setFuture(Future future)
		{
			this.mFuture = future;
		}
		public void setBuilder(Builder builder)
		{
			this.mBuilder = builder;
		}
		public void cancel()
		{
			synchronized(this){
			this.mTarget = null;
			if (mFuture != null)
				mFuture.cancel(true);
			mFuture = null;
			}
		}
		public void setTarget(Target target)
		{
			this.mTarget = target;
		}
		public Target getTarget()
		{
			return mTarget;
		}

		public Builder getBuilder()
		{
			return mBuilder;
		}
		void onSuccess()
		{
			//图片处理
			//内存中查询
			//闪存中查询
			synchronized(this){
			if (getTarget() == null)return;
			BitmapRegionDecoder b=mDecodeCache.get(getBuilder().getKey());
			if (b == null)
			{
				File file=new File(cachePath, mBuilder.getKey());
				file.setLastModified(System.currentTimeMillis());
				try
				{
					mDecodeCache.put(getBuilder().getKey(), (b = BitmapRegionDecoder.newInstance(file.getAbsolutePath(), false)));
				}
				catch (IOException e)
				{
					file.delete();

					mHandler.post(new Runnable(){
							public void run()
							{
								mBuilder.commit();
							}
						});
					//onFail(e);
					return;
				}
			}
			final BitmapRegionDecoder brd=b;
			if (getTarget() != null)
				mHandler.post(new Runnable(){
						public void run()
						{
							synchronized (brd)
							{
								if (getTarget() != null)
									getTarget().onResourceReady(new BitmapCallback(new File(cachePath,getBuilder().getKey()),getBuilder().getBuilderKey(), brd));
							}
						}
					});
					}
		}
		void onFail(Throwable e)
		{
			synchronized(this){
			if (getTarget() != null)
				mHandler.post(new Runnable(){
						public void run()
						{
							if (getTarget() != null)
								getTarget().onLoadFailed(mBuilder.error == 0 ?mBuilder.errorDrawable: mContext.get().getResources().getDrawable(mBuilder.error));
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
	}
	public static abstract class Target
	{
		private Builder mBuilder;
		private String key;
		private TinyImage mTinyImage;
		void setTinyImage(TinyImage tiny){
			this.mTinyImage=tiny;
		}
		public void onResourceReady(final BitmapCallback bc)
		{
			final BitmapRegionDecoder brd=bc.getBitmap();
			synchronized (bc)
			{
				Drawable bitmap=mTinyImage.mMemoryCache.get(getBuilder().getBuilderKey());
				if (bitmap != null)
				{
					onLoadSuccess(bitmap);
				}
				else
				{
					new Thread(){
						public void run()
						{
							synchronized (brd)
							{
								BitmapFactory.Options bo=new BitmapFactory.Options();
								bo.inPreferredConfig = getBuilder().config;
								bo.inTargetDensity=mTinyImage.mContext.get().getResources().getDisplayMetrics().densityDpi;
								bo.inScaled=true;
								bo.inDensity=160;
								Bitmap bitmap= onTransForm(brd,bo, brd.getWidth(), brd.getHeight());
								bc.setDrawable(new TinyBitmapDrawable(mTinyImage.mContext.get(), bitmap));
								mTinyImage.mHandler.post(new Runnable(){

										@Override
										public void run()
										{
											onLoadSuccess(bc.getDrawable());
										}
									});
							}
						}
					}.start();
				}
			}
		}
		abstract public void onLoadSuccess(Drawable d);
		abstract public void onLoadFailed(Drawable d);
		abstract public void onLoadPrepared(Drawable d);
		abstract public void onProgressChanged(int progress);
		abstract public void onLoadCleared();
		void setKey(String key)
		{
			this.key = key;
		}
		public String getKey()
		{
			return key;
		}
		void setBuilder(Builder builder)
		{
			this.mBuilder = builder;
		}
		public Builder getBuilder()
		{
			return mBuilder;
		}
		Bitmap onTransForm(BitmapRegionDecoder brd,BitmapFactory.Options options, int width, int height)
		{
			if (mBuilder == null || mBuilder.mTransForm == null)return brd.decodeRegion(new Rect(0,0,brd.getWidth(),brd.getHeight()),options);
			Bitmap bitmap=null;
			for (TransForm tf:mBuilder.mTransForm)
			{
				if(tf.canDecode())
					bitmap=tf.onTransForm(brd,options,width,height);
				else{
					if(bitmap==null)bitmap=brd.decodeRegion(new Rect(0,0,brd.getWidth(),brd.getHeight()),options);
						bitmap = tf.onTransForm(bitmap, width, height);
						}
			}
			return bitmap;
		}
	}
	class ImageViewTarget extends Target implements ViewTreeObserver.OnPreDrawListener
	{
		private SoftReference<ImageView> view;
		private BitmapFactory.Options options;
		private BitmapCallback brd;
		private int width,height;
		public ImageViewTarget(ImageView view)
		{
			this.view =new SoftReference<ImageView>(view);
			//view.getViewTreeObserver().addOnPreDrawListener(this);
			options = new BitmapFactory.Options();
			options.inTargetDensity=mContext.get().getResources().getDisplayMetrics().densityDpi;
			options.inScaled=true;
			options.inDensity=160;
			options.inDither=true;
		}

		@Override
		public boolean onPreDraw()
		{
			if (brd != null)
				onResourceReady(brd);
			getView().getViewTreeObserver().removeOnPreDrawListener(this);
			return true;
		}

		
		
		@Override
		public void onResourceReady(final BitmapCallback bc)
		{
			final BitmapRegionDecoder brd=bc.getBitmap();
			synchronized (brd)
			{
				if (brd == null || brd.isRecycled())
				{
					getBuilder().invalidate(this);
					return;
				}
				Drawable bitmap = bc.getDrawable();
				if (bitmap == null)
				{
					new Thread(){
						public void run()
						{
							width = getView().getWidth();
							height = getView().getHeight();
							if (width != 0 || height != 0)
							{
								synchronized (brd)
								{
									if (brd.isRecycled())
									{
										getBuilder().invalidate(ImageViewTarget.this);
										return;
									}
									options.inSampleSize = Utils.calculateInSampleSize(brd.getWidth(), brd.getHeight(), width, height);
									Bitmap buffer=null;
									if (getBuilder().mTransForm != null)
									{
										ViewGroup.LayoutParams params=getView().getLayoutParams();
										int width=-2,height=-2;
										if (params.width != -2)
											width = ImageViewTarget.this.width;
										if (params.height != -2)
											height = ImageViewTarget.this.height;
										buffer = onTransForm(brd,options, width, height);
									}else{
										buffer=(brd.decodeRegion(new Rect(0, 0, brd.getWidth(), brd.getHeight()), options));
										
									}
									bc.setDrawable(new TinyBitmapDrawable(mContext.get(), buffer));
									
									mHandler.post(new Runnable(){
											public void run()
											{
												onLoadSuccess(bc.getDrawable());
											}});
								}
							}
							else
							{
								ImageViewTarget.this.brd = bc;
								getView().getViewTreeObserver().addOnPreDrawListener(ImageViewTarget.this);
								//view.requestLayout();
							}
						}}.start();

				}
				else
				{
					getView().setImageDrawable(bitmap);
				}
			}
		}

		@Override
		public void onLoadSuccess(Drawable d)
		{
			getView().setImageDrawable(d);
		}


		@Override
		public void onLoadFailed(Drawable d)
		{
			getView().setImageDrawable(d);
		}

		@Override
		public void onLoadPrepared(Drawable d)
		{
			getView().setImageDrawable(d);
		}

		@Override
		public void onProgressChanged(int progress)
		{
			// TODO: Implement this method
		}

		@Override
		public void onLoadCleared()
		{
			getView().setImageDrawable(null);
			//view.getViewTreeObserver().addOnScrollChangedListener(this);

		}
		public ImageView getView()
		{
			return view.get();
		}
	}
	public interface TransForm
	{
		boolean canDecode();
		Bitmap onTransForm(Bitmap source, int w, int h);
		Bitmap onTransForm(BitmapRegionDecoder brd,BitmapFactory.Options options,int w,int h);
		public String key();


	}
	public static class CropTransForm implements TransForm
	{
		private int gravity;
		public CropTransForm(int gravity)
		{
			this.gravity = gravity;
		}

		@Override
		public String key()
		{
			return "tiny&Crop";
		}

		@Override
		public boolean canDecode()
		{
			return true;
		}

		@Override
		public Bitmap onTransForm(BitmapRegionDecoder source,BitmapFactory.Options options, int w, int h)
		{
			float scale=1;
			int displayWidth=0,displayHeight=0,image_width=source.getWidth(),image_height=source.getHeight();
			if (w == -2)
			{
				//用高度计算
				scale = (float) h / (float) source.getHeight();
				displayHeight = h;
				displayWidth = (int)(source.getWidth() * scale);
			}
			else if (h == -2)
			{
				//用宽度计算
				scale = (float) w / (float) source.getWidth();
				displayWidth = w;
				displayHeight = (int)(source.getHeight() * scale);
			}
			else if (w == -2 && h == -2)
			{
				return source.decodeRegion(new Rect(0,0,source.getWidth(),source.getHeight()),options);
			}
			else
			{
				if (source.getWidth() * h > w * source.getHeight())
				{
					scale = (float) h / (float) source.getHeight();
				}
				else
				{
					scale = (float) w / (float) source.getWidth();
				}
				displayWidth = w;
				displayHeight = h;
			}
			Rect rect=new Rect(0,0,(int)(displayWidth/scale),(int)(displayHeight/scale));
			switch (gravity)
			{
				case Gravity.TOP:
					try{
					return source.decodeRegion(rect,options);
					}catch(Exception e){}
			}
			return null;
		}




		@Override
		public Bitmap onTransForm(Bitmap source, int w, int h)
		{
			return null;
		}
	}
	public static class RoundTransform implements TransForm
	{
		private int radius;//圆角值

		public RoundTransform(int radius)
		{
			this.radius = radius;
		}

		@Override
		public String key()
		{
			// TODO: Implement this method
			return "tiny&Round";
		}

		@Override
		public boolean canDecode()
		{
			return false;
		}

		@Override
		public Bitmap onTransForm(BitmapRegionDecoder brd, BitmapFactory.Options options, int w, int h)
		{
			return null;
		}

		@Override
		public Bitmap onTransForm(Bitmap source, int w, int h)
		{
			int width = source.getWidth();
			int height = source.getHeight();
			//画板
			Bitmap bitmap = Bitmap.createBitmap(width, height, source.getConfig());
			Paint paint = new Paint();
			Canvas canvas = new Canvas(bitmap);//创建同尺寸的画布
			paint.setAntiAlias(true);//画笔抗锯齿
			paint.setDither(true);
			//paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
			//画圆角背景
			RectF rectF = new RectF(new Rect(0, 0, width, height));//赋值
			canvas.drawRoundRect(rectF, radius, radius, paint);//画圆角矩形
			//
			paint.setFilterBitmap(true);
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
			canvas.drawBitmap(source, 0, 0, paint);
			source.recycle();//释放

			return bitmap;
		}
	}
	public static class TinyBitmapDrawable extends BitmapDrawable
	{
		public TinyBitmapDrawable(Context context, Bitmap bitmap)
		{
			super(context.getResources(), bitmap);
		}
		public static TinyBitmapDrawable create(Context context, Bitmap bitmap)
		{
			return new TinyBitmapDrawable(context, bitmap);
		}

		@Override
		public void draw(Canvas canvas)
		{
			if (getBitmap().isRecycled())
			{
				if(getCallback()!=null){
				ImageViewTarget target=(TinyImage.ImageViewTarget) ((ImageView)getCallback()).getTag();
				Builder builder=target.getBuilder();
				if (builder != null)builder.invalidate(target);
				}
			}
			else
				super.draw(canvas);
		}

	}
	class PlaceHolderDrawable extends Drawable
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
	public class BitmapCallback
	{
		private File cacheFile;
		private SoftReference<BitmapRegionDecoder> brd;
		private SoftReference<Drawable> drawable;
		private String builderKey;
		public BitmapCallback(File cacheFile,String key, BitmapRegionDecoder brd)
		{
			this.cacheFile = cacheFile;
			this.builderKey=key;
			this.brd = new SoftReference<BitmapRegionDecoder>(brd);
		}
		public File getCacheFile(){
			return cacheFile;
		}
		public void setDrawable(Drawable bitmap)
		{
			this.drawable=new SoftReference<Drawable>(bitmap);
			mMemoryCache.put(builderKey, bitmap);
		}
		public BitmapRegionDecoder getBitmap()
		{
			return brd.get();
		}
		public Drawable getDrawable(){
			if(drawable==null){
				Drawable d=mMemoryCache.get(builderKey);
				if(d!=null){
					drawable=new SoftReference<Drawable>(d);
					return d;
				}
				return null;
			}
			return drawable.get();
		}
	}
	interface Downloader
	{

		Response load(String url, Map<String,String> header);
		class Response
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
	class UrlDownloader implements Downloader
	{

		@Override
		public com.moe.tinyimage.TinyImage.Downloader.Response load(String url, Map<String, String> header)
		{
			HttpURLConnection huc=null;
			try
			{
				huc = (HttpURLConnection) new URL(url).openConnection();
				Iterator<Map.Entry<String,String>> iterator=header.entrySet().iterator();
				while (iterator.hasNext())
				{
					Map.Entry<String,String> entry=iterator.next();
					huc.setRequestProperty(entry.getKey(), entry.getValue());
				}
				Map<String,String> map=new HashMap<>();
				map.put("Location", huc.getHeaderField("Location"));
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
}
