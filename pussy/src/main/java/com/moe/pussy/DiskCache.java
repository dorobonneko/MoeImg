package com.moe.pussy;
import java.util.LinkedHashMap;
import android.content.Context;
import java.util.Iterator;
import java.util.HashMap;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

public class  DiskCache<K extends String,V extends CacheDescriptor>
{
	private long memorySize;
	private HashMap<K,V> map=new HashMap<>();
	public File path;
	public DiskCache(long memorySize, String cachePath)
	{
		this.memorySize = memorySize;
		this.path =new File(cachePath);
		if(!this.path.exists())
			this.path.mkdirs();
	}

	public void clear()
	{
		new Thread(){
			public void run(){
				for(File file:path.listFiles())
				file.delete();
			}
		}.start();
	}
	public void setMemoryCache(long memorySize)
	{
		this.memorySize = memorySize;
		termToMemory();
	}

	public void termToMemory()
	{
		new Thread(){
			public void run(){
				long totalLength=0;
				for(File file:path.listFiles())
				totalLength+=file.length();
				if(totalLength>=memorySize){
					//清理
					List<File> list=Arrays.asList(path.listFiles());
					Collections.sort(list, new Comparator<File>(){

							@Override
							public int compare(File p1, File p2)
							{
								return Long.compare(p1.lastModified(),p2.lastModified());
							}
							
						
					});
					for(File file:list){
						totalLength-=file.length();
						file.delete();
						if(totalLength<memorySize/2)
							break;
					}
				}
			}
		}.start();
	}
	public V get(K k)
	{
		V v=map.get(k);
		if (v == null)
			synchronized (map)
			{
				v = map.get(k);
				if (v == null)
				{
					v = (V) new CacheDescriptor(new File(path, k));
					map.put(k, v);
				}
			}
		return v;
	}
}
