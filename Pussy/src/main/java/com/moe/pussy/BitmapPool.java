package com.moe.pussy;
import java.util.Map;
import java.util.HashMap;
import android.graphics.Bitmap;
import java.util.ArrayList;
import android.graphics.Canvas;
import java.util.List;

public class BitmapPool
{
	private volatile static Map<String,List<Bitmap>> map=new HashMap<String,List<Bitmap>>();
	private volatile static int maxSize=8;
	private volatile static BitmapPool bp;
	private BitmapPool()
	{
	}
	public static BitmapPool get()
	{
		if (bp == null)
			synchronized (BitmapPool.class)
			{
				if (bp == null)
					bp = new BitmapPool();
			}
		return bp;
	}

	public static Bitmap getBitmap(int w, int h, Bitmap.Config config)
	{
		synchronized (map)
		{
			List<Bitmap> list=map.get(w * h + config.name());
			if (list == null)
				map.put(w * h + config.name(), list = new ArrayList<Bitmap>());
			if (list.isEmpty())
				return Bitmap.createBitmap(w, h, config);
			Bitmap b= list.remove(0);
			b.reconfigure(w,h,config);
			return b;
		}
	}
	public static void recycle(Bitmap bitmap)
	{
		if (bitmap == null || bitmap.isRecycled())return;
		int w=bitmap.getWidth();
		int h=bitmap.getHeight();
		synchronized (map)
		{
			List<Bitmap> list=map.get(w * h + bitmap.getConfig().name());
			if (list == null)
			{
				//该尺寸图片没有申请记录，不保存该尺寸图片
				bitmap.recycle();

			}
			//map.put(w+"x"+h,list=new ArrayList<Bitmap>());
			else
			{
				if (list.size() >= maxSize||!bitmap.isMutable())
					bitmap.recycle();
				else
				{
					bitmap.eraseColor(0);//清除颜色
					list.add(bitmap);
				}
			}
		}
	}
	public static boolean isRecycled(Bitmap bitmap)
	{
		List<Bitmap> list=map.get(bitmap.getWidth() * bitmap.getHeight() + bitmap.getConfig().name());
		if (list == null)return false;
		return list.contains(bitmap);
	}
}
