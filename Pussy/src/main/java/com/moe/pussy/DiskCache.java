package com.moe.pussy;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.io.EOFException;
import java.io.BufferedOutputStream;

public class DiskCache
{
	private String cachePath;
	private static DiskCache mDiskCache;
	private long size;
	private DiskCache(Pussy p){
		File path=p.getContext().getFileStreamPath("Pussy");
		cachePath=path.getAbsolutePath();
		if(!path.exists())
			path.mkdirs();
		this.size=PussyConfig.diskCacheSize;
	}
	public static DiskCache get(Pussy p){
		if(mDiskCache==null){
			mDiskCache=new DiskCache(p);
		}
		return mDiskCache;
	}
	public void trimToSize(){
		new Thread(){
			public void run()
			{
				synchronized (cachePath)
				{
					List<File> list=Arrays.asList(new File(cachePath).listFiles());
					long totalLength=0;
					for (File file:list)
						totalLength += file.length();
					if (totalLength < size)return;
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
						if (totalLength < size)
							break;
					}

				}
			}
		}.start();
	}
	public void clearCache(){
		File[] files=new File(cachePath).listFiles();
		if(files!=null)
			for(File f:files)
			f.delete();
	}
	/*public InputStream getInputStream(File cache,InputStream input) throws FileNotFoundException{
		return new CacheInputStream(input,cache);
	}*/
	public File getCache(String key){
		return getCache(key,false);
	}
	public File getCache(String key,boolean outFile){
		File cache=new File(cachePath,key);
		if(cache.exists()){
			cache.setLastModified(System.currentTimeMillis());
			return cache;
		}
		if(outFile)
			return cache;
		return null;
	}
	public File getDirty(String key){
		return new File(cachePath,key+".tmp");
	}
	public void invalidate(String key){
		getCache(key);
	}
	public class CacheInputStream extends InputStream
	{
		private InputStream in;
		private BufferedOutputStream out;
		private File file;
		public CacheInputStream(InputStream in,File out) throws FileNotFoundException{
			this.in=in;
			this.file=out;
			this.out=new BufferedOutputStream(new FileOutputStream(out,true),8192);
		}
		@Override
		public int read() throws IOException
		{
			int i=in.read();
			out.write(i);
			//out.flush();
			return i;
		}

		@Override
		public int available() throws IOException
		{
			return in.available();
		}

		@Override
		public void mark(int readlimit)
		{
			in.mark(readlimit);
		}

		@Override
		public boolean markSupported()
		{
			return in.markSupported();
		}

		@Override
		public long skip(long n) throws IOException
		{
			return in.skip(n);
		}

		@Override
		public void close() throws IOException
		{
			out.flush();
			out.close();
			in.close();
			
		}

		@Override
		public void reset() throws IOException
		{
			in.reset();
		}
		
	}
	public enum Cache{
		NONE,MASK;
	}
}
