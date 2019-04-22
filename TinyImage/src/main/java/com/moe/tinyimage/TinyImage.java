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

public class TinyImage
{
	//private Map<ImageView,Builder> map=new HashMap<>();
	private long memorySize=64,diskCacheSize=128 * 1020 * 1020;
	private Map<String,Callback> netMap=new HashMap<>();
	//线程池，同时处理6条数据，最大128个任务
	private Context context;
	private static TinyImage mTinyImage;
	private ThreadPoolExecutor mThreadPoolExecutor;
	//private OkHttpClient mOkHttpClient;
	private String cachePath;
	private Handler mHandler=new Handler(Looper.getMainLooper());
	private ActivityLifecycle mActivityLifecycle=new ActivityLifecycle();
	private Downloader mDownloader;
	private LruCache<String,BitmapRegionDecoder> mLruCache=new LruCache<String,BitmapRegionDecoder>((int)memorySize * 2){
		@Override
		protected void entryRemoved(boolean evicted, String key, final BitmapRegionDecoder oldValue, BitmapRegionDecoder newValue)
		{
			if (evicted)
				oldValue.recycle();
		}

	};
	private LruCache<String,Bitmap> decode=new LruCache<String,Bitmap>((int)memorySize){
		@Override
		protected void entryRemoved(boolean evicted, String key, final Bitmap oldValue, Bitmap newValue)
		{
			if (evicted)
				oldValue.recycle();
		}



	};
	
	private TinyImage(Context context)
	{
		this.context = context.getApplicationContext();
		Application mApplication=((Application)this.context.getApplicationContext());
		mApplication.registerActivityLifecycleCallbacks(mActivityLifecycle);
		mApplication.registerComponentCallbacks(mActivityLifecycle);
		mThreadPoolExecutor = new ThreadPoolExecutor(12, 12, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
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
	public void downloader(Downloader mDownloader){
		this.mDownloader=mDownloader;
	}
	public Builder load(String url, ImageView view)
	{
		if(mDownloader==null)mDownloader=new UrlDownloader();
		String key=Utils.getKey(url);
		Callback call=netMap.get(key);
		Target t=(TinyImage.Target) view.getTag();
		if (t == null)
			view.setTag(t = new ImageViewTarget(view));
		t.setKey(key);
		Builder b=new Builder(url, key);
		if (call == null)
		{
			call = new Callback();
			netMap.put(key, call); 
		}
		call.setBuilder(b);
		cancel(view);
		call.setTarget(t);
		t.setBuilder(b);
		return b;
	}
	public void cancel(ImageView view)
	{
		Iterator<Callback> iterator=netMap.values().iterator();{
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
		Iterator<Callback> iterator=netMap.values().iterator();{
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
		decode.trimToSize((int)memorySize / 4);
	}
	public void clearMemory(){
		decode.trimToSize(0);
		mLruCache.trimToSize(0);
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
				try
				{
					builderKey = Utils.byte2HexStr(MessageDigest.getInstance("MD5").digest(sb.toString().getBytes())).concat(".").concat(key);
				}
				catch (NoSuchAlgorithmException e)
				{}
			}
			return builderKey;
		}
		public void commit()
		{
			invalidate(null);
		}
		void invalidate(Target target)
		{
			Callback call=netMap.get(key);
			if (target != null)
			{
				call.setTarget(target);
				target.setBuilder(this);
			}
			BitmapRegionDecoder brd=mLruCache.get(key);
			if (brd != null)
			{
				call.getTarget().onResourceReady(new BitmapCallback(getBuilderKey(),brd));
				return;
			}

			call.getTarget().onLoadPrepared(placeHolder == 0 ?placeHolderDrawable: context.getResources().getDrawable(placeHolder));

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
				if (file_src.isFile())
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
					header.put("Connection","Keep-Alive");
					res=mDownloader.load(url,header);
					if(res==null)throw new IOException();
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
							url=res.header("Location");
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
			this.mTarget = null;
			if (mFuture != null)
				mFuture.cancel(true);
			mFuture = null;
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
			if (getTarget() == null)return;
			BitmapRegionDecoder b=mLruCache.get(mBuilder.getKey());
			if (b == null)
			{
				File file=new File(cachePath, mBuilder.getKey());
				file.setLastModified(System.currentTimeMillis());
				try
				{
					mLruCache.put(mBuilder.getKey(), b = BitmapRegionDecoder.newInstance(file.getAbsolutePath(), false));
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
							if (getTarget() != null)
								getTarget().onResourceReady(new BitmapCallback(getBuilder().getBuilderKey(),brd));
						}
					});
		}
		void onFail(Throwable e)
		{
			if (getTarget() != null)
				mHandler.post(new Runnable(){
						public void run()
						{
							if (getTarget() != null)
								getTarget().onLoadFailed(mBuilder.error == 0 ?mBuilder.errorDrawable: context.getResources().getDrawable(mBuilder.error));
						}
					});
		}
	}
	public static class Utils
	{
		public static String getKey(String url)
		{
			try
			{
				return byte2HexStr(MessageDigest.getInstance("MD5").digest(url.getBytes()));
			}
			catch (NoSuchAlgorithmException e)
			{
				return Base64.getEncoder().encodeToString(url.getBytes());
			}
		}
		public static int calculateInSampleSize(int width, int height, float reqWidth, float reqHeight)
		{
			int inSampleSize = 1;
			if (height > reqHeight || width > reqWidth)
			{
				final int halfHeight = height / 2;
				final int halfWidth = width / 2;
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
	abstract class Target
	{
		private Builder mBuilder;
		private String key;
		void onResourceReady(final BitmapCallback bc)
		{
			final BitmapRegionDecoder brd=bc.getBitmap();
			Bitmap bitmap=decode.get(getBuilder().getBuilderKey());
			if (bitmap != null)
			{
				onLoadSuccess(new TinyBitmapDrawable(context, bitmap));
			}
			else
			{
				new Thread(){
					public void run()
					{
						BitmapFactory.Options bo=new BitmapFactory.Options();
						bo.inPreferredConfig = getBuilder().config;
						Bitmap bitmap=brd.decodeRegion(new Rect(0, 0, brd.getWidth(), brd.getHeight()), bo);
						bitmap = onTransForm(bitmap, bitmap.getWidth(), bitmap.getHeight());
						bc.setBitmap(bitmap);
						final Bitmap final_buffer=bitmap;
						mHandler.post(new Runnable(){

								@Override
								public void run()
								{
									onLoadSuccess(new TinyBitmapDrawable(context, final_buffer));
								}
							});
					}
				}.start();
			}
		}
		abstract void onLoadSuccess(Drawable d);
		abstract void onLoadFailed(Drawable d);
		abstract void onLoadPrepared(Drawable d);
		abstract void onProgressChanged(int progress);
		abstract void onLoadCleared();
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
		Bitmap onTransForm(Bitmap bitmap, int width, int height)
		{
			if (mBuilder == null || mBuilder.mTransForm == null)return bitmap;
			for (TransForm tf:mBuilder.mTransForm)
			{
				bitmap = tf.onTransForm(bitmap, width, height);
			}
			return bitmap;
		}
	}
	class ImageViewTarget extends Target implements ViewTreeObserver.OnScrollChangedListener,ViewTreeObserver.OnPreDrawListener
	{
		private ImageView view;
		private int width,height;
		private BitmapFactory.Options options;
		private BitmapCallback brd;
		public ImageViewTarget(ImageView view)
		{
			this.view = view;
			view.getViewTreeObserver().addOnPreDrawListener(this);
			options = new BitmapFactory.Options();
		}

		@Override
		public boolean onPreDraw()
		{

			width = view.getWidth();
			height = view.getHeight();
			if (brd != null)
				onResourceReady(brd);
			view.getViewTreeObserver().removeOnPreDrawListener(this);
			return false;
		}

		@Override
		public void onScrollChanged()
		{
			if (brd != null)
			{
				if (!brd.getBitmap().isRecycled())
				{
					onResourceReady(brd);
				}
				else if (isVisiable())
				{
					netMap.get(getKey()).onSuccess();
					view.getViewTreeObserver().removeOnScrollChangedListener(this);
				}
			}
		}


		public boolean isVisiable()
		{
			Rect rect = new Rect();
			view.getGlobalVisibleRect(rect);
			return rect.height() > 0;
		}
		@Override
		public void onResourceReady(final BitmapCallback bc)
		{
			final BitmapRegionDecoder brd=bc.getBitmap();
			if (brd == null || brd.isRecycled())
				return;
			ImageViewTarget.this.brd = bc;
			Bitmap bitmap=null;
			synchronized (decode)
			{
				bitmap = decode.get(getBuilder().getBuilderKey());
			}
			if (bitmap == null || bitmap.isRecycled())
			{
				new Thread(){
					public void run()
					{
						width = view.getWidth();
						height = view.getHeight();
						if (width != 0 || height != 0)
						{
							options.inSampleSize = Utils.calculateInSampleSize(brd.getWidth(), brd.getHeight(), width, height);
							Bitmap buffer=(brd.decodeRegion(new Rect(0, 0, brd.getWidth(), brd.getHeight()), options));
							if (getBuilder().mTransForm != null)
							{
								ViewGroup.LayoutParams params=view.getLayoutParams();
								int width=-2,height=-2;
								if (params.width != -2)
									width = ImageViewTarget.this.width;
								if (params.height != -2)
									height = ImageViewTarget.this.height;
								buffer = onTransForm(buffer.copy(buffer.getConfig(), false), width, height);
							}
							bc.setBitmap(buffer);
							final Bitmap bit=buffer;
							getView().post(new Runnable(){
									public void run()
									{
										getView().setImageDrawable(TinyBitmapDrawable.create(context, bit));
									}});
						}
						else
						{
							view.getViewTreeObserver().addOnPreDrawListener(ImageViewTarget.this);
							//view.requestLayout();
						}
					}}.start();

			}
			else
			{
				getView().setImageDrawable(TinyBitmapDrawable.create(context, bitmap));
			}
		}

		@Override
		void onLoadSuccess(Drawable d)
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
			return view;
		}
	}
	public interface TransForm
	{
		abstract Bitmap onTransForm(Bitmap source, int w, int h);

		abstract public String key();


	}
	public static class CropTransForm implements TransForm
	{
		int gravity;
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
		public Bitmap onTransForm(Bitmap source, int w, int h)
		{
			float scale=1;
			int displayWidth=0,displayHeight=0;
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
				return source;
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
			Bitmap buffer=Bitmap.createBitmap(displayWidth, displayHeight, Bitmap.Config.ARGB_8888);
			Canvas canvas=new Canvas(buffer);
			Matrix matrix=new Matrix();
			matrix.setScale(scale, scale);
			switch (gravity)
			{
				case Gravity.TOP:
					canvas.drawBitmap(source, matrix, null);
					break;
			}
			source.recycle();
			return buffer;
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
		public Bitmap onTransForm(Bitmap source, int w, int h)
		{
			int width = source.getWidth();
			int height = source.getHeight();
			//画板
			Bitmap bitmap = Bitmap.createBitmap(width, height, source.getConfig());
			Paint paint = new Paint();
			Canvas canvas = new Canvas(bitmap);//创建同尺寸的画布
			paint.setAntiAlias(true);//画笔抗锯齿
			paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
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
		private Context context;
		public TinyBitmapDrawable(Context context, Bitmap bitmap)
		{
			super(context.getResources(), bitmap);
			this.context = context;
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
				ImageViewTarget target=(TinyImage.ImageViewTarget) ((ImageView)getCallback()).getTag();
				Builder builder=target.getBuilder();
				if (builder != null)builder.invalidate(target);
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
			trimMemory();
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
			// TODO: Implement this method
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
	class BitmapCallback
	{
		private String key;
		private BitmapRegionDecoder brd;
		public BitmapCallback(String key, BitmapRegionDecoder brd)
		{
			this.key = key;
			this.brd = brd;
		}
		public void setBitmap(Bitmap bitmap)
		{
			decode.put(key, bitmap);
		}
		public BitmapRegionDecoder getBitmap()
		{
			return brd;
		}
	}
	interface Downloader{
		
		Response load(String url,Map<String,String> header);
		class Response{
			private int code;
			private InputStream input;
			private long length;
			private Closeable close;
			private Map<String,String> map;
			public Response(int code,InputStream is,long length,Closeable close,Map<String,String> map){
				this.code=code;
				this.input=is;
				this.length=length;
				this.close=close;
				this.map=map;
			}
			public int code(){
				return code;
			}
			public InputStream inputStream(){
				return input;
			}
			public long length(){
				return length;
			}
			public void close(){
				try
				{
					if (close != null)close.close();
				}
				catch (IOException e)
				{}
			}
			public String header(String key){
				if(map==null) return null;
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
				huc= (HttpURLConnection) new URL(url).openConnection();
				Iterator<Map.Entry<String,String>> iterator=header.entrySet().iterator();
				while(iterator.hasNext()){
					Map.Entry<String,String> entry=iterator.next();
					huc.setRequestProperty(entry.getKey(),entry.getValue());
				}
				Map<String,String> map=new HashMap<>();
				map.put("Location",huc.getHeaderField("Location"));
				final HttpURLConnection close=huc;
				return new Response(huc.getResponseCode(),huc.getInputStream(),huc.getContentLength(),new Closeable(){

					@Override
					public void close() throws IOException
					{
						close.disconnect();
					}
				},map);
			}
			catch (IOException e)
			{
				if(huc!=null)huc.disconnect();
			}
			return null;
		}
		
		
	}
}
