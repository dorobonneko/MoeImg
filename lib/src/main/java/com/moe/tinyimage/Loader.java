package com.moe.tinyimage;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class Loader implements Runnable
{
	private List<Target> calls;
	private String url,key;
	private Pussy mPussy;
	private boolean preload;
	public Loader(Pussy pussy, String url)
	{
		this.mPussy = pussy;
		this.url = url;
		key = Utils.encode(url);
		calls=Collections.synchronizedList(new ArrayList<Target>());
	}

	public void preload()
	{
		preload=true;
		BitmapDecoder brd=mPussy.mDecodeCache.get(key);
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
			File file=new File(mPussy.cachePath, key);
			if(file.exists())
				file.setLastModified(System.currentTimeMillis());
			Bitmap image=target.getRequest().getPussy().mMemoryCache.get(target.getRequest().getKey());
			if(image!=null){
				target.onLoadSuccess(Pussy.TinyBitmapDrawable.create(target,image));
				return;
			}
			target.onLoadPrepared(target.getRequest().getPlaceHolder());
			BitmapDecoder brd=mPussy.mDecodeCache.get(key);
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
		if(mPussy.mDecodeCache.get(key)!=null)
			return;
		File file_src=new File(mPussy.cachePath, key);
		if (file_src.exists() && file_src.isFile())
		{
			onSuccess();
			return;
		}
		Uri uri=Uri.parse(url);
		Pussy.RequestHandler.Result res=null;
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
			for(Pussy.RequestHandler rh:mPussy.mRequestHandler.toArray(new Pussy.RequestHandler[0])){
				if(rh.canHandle(uri))
				{
					res = rh.load(uri, header);
					if(res!=null)break;
				}
			}
			if (res == null)throw new IOException();
			if (isCanceled())
				throw new IllegalStateException();
			if(res instanceof Pussy.RequestHandler.Image){

			}else if(res instanceof Pussy.RequestHandler.Response){
				onProcessResponse((Pussy.RequestHandler.Response)res,file,file_src);
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
	void onProcessImage(final Pussy.RequestHandler.Image image){
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
	void onProcessResponse(Pussy.RequestHandler.Response res,File temp_file,File src) throws Exception{

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
			byte[] buffer=new byte[10240];
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
	public void onPrepared(Target t, BitmapDecoder brd)
	{
		t.onResourceReady(new Pussy.BitmapCallback(mPussy,new File(mPussy.cachePath, key), t.getRequest().getKey(), brd));
	}
	public synchronized void onSuccess()
	{
		BitmapDecoder brd=mPussy.mDecodeCache.get(key);
		if (brd == null)
		{
			File cacheFile=new File(mPussy.cachePath, key);
			if (cacheFile.exists())
			{
				cacheFile.setLastModified(System.currentTimeMillis());
				try
				{
					brd = new BitmapDecoder(cacheFile.getAbsolutePath());
					mPussy.mDecodeCache.put(key,brd);
				}
				catch (Exception e)
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
		final BitmapDecoder final_brd=brd;
		synchronized(calls){
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
	}
	public void onFail(final Exception e){
		synchronized(calls){
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
}
	
