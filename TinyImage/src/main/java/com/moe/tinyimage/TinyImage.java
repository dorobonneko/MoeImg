package com.moe.tinyimage;
import java.util.Map;
import java.util.HashMap;
import android.content.Context;
import java.util.concurrent.ThreadPoolExecutor;
import okhttp3.OkHttpClient;
import android.os.Handler;
import android.os.Looper;
import android.graphics.BitmapRegionDecoder;
import android.util.LruCache;
import android.graphics.Bitmap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import okhttp3.ConnectionPool;
import java.io.File;
import android.widget.ImageView;
import java.util.Iterator;
import java.security.NoSuchAlgorithmException;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
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

public class TinyImage
{
	//private Map<ImageView,Builder> map=new HashMap<>();
	private Map<String,Callback> netMap=new HashMap<>();
	//线程池，同时处理6条数据，最大128个任务
	private Context context;
	private static TinyImage mTinyImage;
	private ThreadPoolExecutor mThreadPoolExecutor;
	private OkHttpClient mOkHttpClient;
	private String cachePath;
	private Handler mHandler=new Handler(Looper.getMainLooper());
	private LruCache<String,BitmapRegionDecoder> mLruCache=new LruCache<String,BitmapRegionDecoder>(64){

		@Override
		protected int sizeOf(String key, BitmapRegionDecoder value)
		{
			// TODO: Implement this method
			return 1;
		}

		@Override
		protected void entryRemoved(boolean evicted, String key, final BitmapRegionDecoder oldValue, BitmapRegionDecoder newValue)
		{
			if (evicted)
			{oldValue.recycle();
				/*synchronized (netMap)
				 {
				 //清除imageview的图片
				 Callback call=netMap.get(key);
				 if (call != null)
				 {
				 final Target target=call.getTarget();
				 if (target != null)
				 {
				 mHandler.post(new Runnable(){
				 public void run(){target.onLoadCleared();oldValue.recycle();}});

				 }
				 }
				 }*/
			}
			super.entryRemoved(evicted, key, oldValue, newValue);
		}

	};
	private LruCache<String,Bitmap> decode=new LruCache<String,Bitmap>(64){

		@Override
		protected int sizeOf(String key, Bitmap value)
		{
			// TODO: Implement this method
			return 1;
		}

		@Override
		protected void entryRemoved(boolean evicted, String key, final Bitmap oldValue, Bitmap newValue)
		{
			if (evicted)
			{oldValue.recycle();
				/*Callback call=netMap.get(key.substring(key.lastIndexOf(".")));
				 if(call!=null){
				 final Target t=call.getTarget();
				 if(t!=null)
				 mHandler.post(new Runnable(){
				 public void run(){t.onLoadCleared();oldValue.recycle();}});
				 }*/

			}
			super.entryRemoved(evicted, key, oldValue, newValue);
		}



	};
	private TinyImage(Context context)
	{
		this.context = context.getApplicationContext();
		mThreadPoolExecutor = new ThreadPoolExecutor(12, 12, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
		mOkHttpClient = new OkHttpClient.Builder().connectionPool(new ConnectionPool(6, 5, TimeUnit.MINUTES)).followRedirects(true).followSslRedirects(true).build();
		File cache=new File(context.getCacheDir(), "net");
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
	public Builder load(String url, ImageView view)
	{
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
	public class Builder
	{
		private String url,key;
		private int placeHolder,error;
		private TransForm mTransForm;
		private String builderKey;
		public Builder(String url, String key)
		{
			this.url = url;
			this.key = key;
		}
		/*public ImageView getView(){
		 return view;
		 }*/
		public Builder placeHolder(int resId)
		{
			placeHolder = resId;
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
				sb.append(url).append(placeHolder).append(error).append(mTransForm);
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
				call.getTarget().onLoadSuccess(brd);
				return;
			}

			if (placeHolder != 0)
				call.getTarget().onLoadPrepared(context.getResources().getDrawable(placeHolder));
			Execute execute=new Execute(url, cachePath, call);
			call.setFuture(mThreadPoolExecutor.submit(execute));
			execute.setFuture(call.mFuture);
		}
		public String getUrl()
		{
			return url;
		}

		public Builder transForm(TransForm trans)
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
				Request req=new Request.Builder().url(url).addHeader("Range", "bytes=".concat(String.valueOf(file.length()).concat("-"))).build();
				Call call=null;
				Response res=null;
				OutputStream output=null;
				InputStream input=null;
				int error=0;
				out:
				try
				{
					if (isCancelled())
						throw new IllegalStateException();
					res = (call = mOkHttpClient.newCall(req)).execute();
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
							req = new Request.Builder().url(res.header("Location")).addHeader("Range", "bytes=".concat(String.valueOf(file.length()).concat("-"))).build();
							break out;
						default:throw new IOException(String.valueOf(res.code()));
					}
					input = res.body().byteStream();
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
					error++;
					if (error >= 2)
						mCallback.onFail(e);
					else
						break out;
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
					if (call != null)call.cancel();
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
							getTarget().onLoadSuccess(brd);
						}
					});
		}
		void onFail(Throwable e)
		{
			if (getTarget() != null)
				mHandler.post(new Runnable(){
						public void run()
						{
							getTarget().onLoadFailed(mBuilder.error != 0 ?context.getResources().getDrawable(mBuilder.error): null);
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
	interface Target
	{
		void onLoadSuccess(BitmapRegionDecoder brd);
		void onLoadFailed(Drawable d);
		void onLoadPrepared(Drawable d);
		void onProgressChanged(int progress);
		public void onLoadCleared();
		void setKey(String key);
		void setBuilder(Builder builder);
	}
	class ImageViewTarget implements Target,ViewTreeObserver.OnScrollChangedListener,ViewTreeObserver.OnPreDrawListener
	{
		private ImageView view;
		private int width,height;
		private BitmapFactory.Options options;
		private BitmapRegionDecoder brd;
		private String key;
		private Builder mBuilder;
		public ImageViewTarget(ImageView view)
		{
			this.view = view;
			view.getViewTreeObserver().addOnPreDrawListener(this);
			options = new BitmapFactory.Options();

		}
		public void setBuilder(Builder builder)
		{
			this.mBuilder = builder;
		}
		public void setKey(String key)
		{
			this.key = key;
		}

		@Override
		public boolean onPreDraw()
		{

			width = view.getWidth();
			height = view.getHeight();
			if (brd != null)
				onLoadSuccess(brd);
			view.getViewTreeObserver().removeOnPreDrawListener(this);
			return false;
		}

		@Override
		public void onScrollChanged()
		{
			if (brd != null)
			{
				if (!brd.isRecycled())
				{
					onLoadSuccess(brd);
				}
				else if (isVisiable())
				{
					netMap.get(key).onSuccess();
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
		public void onLoadSuccess(final BitmapRegionDecoder brd)
		{
			if (brd == null || brd.isRecycled())
				return;
			ImageViewTarget.this.brd = brd;
			Bitmap bitmap=null;
			synchronized (decode)
			{
				bitmap = decode.get(mBuilder.getBuilderKey());
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
							if (mBuilder.mTransForm != null)
							{
								ViewGroup.LayoutParams params=view.getLayoutParams();
								int width=-2,height=-2;
								if (params.width != -2)
									width = ImageViewTarget.this.width;
								if (params.height != -2)
									height = ImageViewTarget.this.height;
								buffer = mBuilder.mTransForm.transForm(buffer.copy(Bitmap.Config.ARGB_8888, false), width, height);
							}
							synchronized (decode)
							{
								decode.put(mBuilder.getBuilderKey(), buffer);
								;}
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
	public static abstract class TransForm
	{
		TransForm trans;
		public TransForm(TransForm trans)
		{
			this.trans = trans;
		}
		final Bitmap transForm(Bitmap source, int viewWidth, int viewHeight)
		{
			if (trans != null)source = trans.transForm(source, viewWidth, viewHeight);
			return onTransForm(source, viewWidth, viewHeight);
		}
		abstract Bitmap onTransForm(Bitmap source, int w, int h);

		@Override
		public String toString()
		{
			if (trans != null)return trans.toString();
			return "interface";
		}


	}
	public static class CropTransForm extends TransForm
	{
		int gravity;
		public CropTransForm(int gravity)
		{
			super(null);
			this.gravity = gravity;
		}
		public CropTransForm(int gravity, TransForm trans)
		{
			super(trans);
			this.gravity = gravity;
		}

		@Override
		Bitmap onTransForm(Bitmap source, int w, int h)
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

		@Override
		public String toString()
		{
			// TODO: Implement this method
			return super.toString().concat(":crop");
		}


	}
	public static class RoundTransform extends TransForm
	{
		private int radius;//圆角值

		public RoundTransform(int radius)
		{
			super(null);
			this.radius = radius;
		}
		public RoundTransform(int radius, TransForm trans)
		{
			super(trans);
			this.radius = radius;
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

		@Override
		public String toString()
		{
			// TODO: Implement this method
			return super.toString().concat("round");
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
				Builder builder=target.mBuilder;
				if (builder != null)builder.invalidate(target);
			}
			else
				super.draw(canvas);
		}

	}
}
