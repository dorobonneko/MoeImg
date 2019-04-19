package com.moe.pussy;
import java.io.File;
import android.net.Uri;
import android.graphics.drawable.Drawable;
import java.lang.reflect.Method;
import android.content.Context;
import android.app.Application;
import java.util.ArrayList;
import java.util.List;

public class Pussy
{
	private long memoryCacheSize=16*1024*1024,diskCacheSize=128*1024*1024;
	private LruCache<Object,Drawable> memoryCache;
	private DiskCache mDiskCache;
	private List<RequestHandler> request=new ArrayList<>();
	private static Pussy mPussy;
	private static Context CURRENT;
	Downloader mDownloader;
	private Pussy()
	{
		mDiskCache=new DiskCache(diskCacheSize,new File(CURRENT.getCacheDir(),"Pussy_Image_Cache").getAbsolutePath());
		memoryCache=new LruCache<>(memoryCacheSize);
		mDownloader=new Downloader();
		request.add(new NetWorkRequestHandler());
	}
	static {
        try {
            Object activityThread = getActivityThread();
            Object app = activityThread.getClass().getMethod("getApplication").invoke(activityThread);
            CURRENT = (Application) app;
        } catch (Throwable e) {
            }
    }

    private static Object getActivityThread() {
        Object activityThread = null;
        try {
            Method method = Class.forName("android.app.ActivityThread").getMethod("currentActivityThread");
            method.setAccessible(true);
            activityThread = method.invoke(null);
        } catch (final Exception e) {
            
        }
        return activityThread;
    }
	public static Pussy getSingleInstance(){
		if(mPussy==null){
			synchronized(Pussy.class){
				if(mPussy==null)mPussy=new Pussy();
			}
		}
		return mPussy;
	}
	public Context getContext(){
		return CURRENT;
	}
	public static void setMemoryCacheSize(long memoryCacheSize)
	{
		getSingleInstance().memoryCacheSize = memoryCacheSize;
	}

	public static long getMemoryCacheSize()
	{
		return getSingleInstance().memoryCacheSize;
	}
	public static void setDiskCacheSize(long diskCacheSize)
	{
		getSingleInstance().diskCacheSize = diskCacheSize;
	}

	public static long getDiskCacheSize()
	{
		return getSingleInstance().diskCacheSize;
	}
	public static void clearMemoryCache(){
		if(getSingleInstance().memoryCache!=null)
			getSingleInstance().memoryCache.clear();
	}
	public static void clearDiskCache(){
		if(getSingleInstance().mDiskCache!=null)
			getSingleInstance().mDiskCache.clear();
	}
	public static void pause(){}
	public static void resume(){}
	public static Request.Builder load(String url){
		Request.Builder rb=new Request.Builder(url);
		rb.setPussy(getSingleInstance());
		return rb;
	}
	public static Request.Builder load(File file){
		Request.Builder rb=new Request.Builder(file);
		rb.setPussy(getSingleInstance());
		return rb;
	}
	public static Request.Builder load(Uri uri){
		Request.Builder rb=new Request.Builder(uri);
		rb.setPussy(getSingleInstance());
		return rb;
	}
	public static Request.Builder load(int res){
		Request.Builder rb=new Request.Builder(res);
		rb.setPussy(getSingleInstance());
		return rb;
	}
	public void onDestory(){
		if(mDiskCache!=null)
			mDiskCache.termToMemory();
		if(memoryCache!=null)
			mDiskCache.clear();
	}
	
}
