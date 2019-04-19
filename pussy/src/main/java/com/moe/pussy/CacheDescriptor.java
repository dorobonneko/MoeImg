package com.moe.pussy;
import java.io.File;
import java.util.Comparator;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileInputStream;

public class CacheDescriptor implements Comparator<CacheDescriptor>
{
	private File file;
	CacheDescriptor(File file){
		this.file=file;
	}
	public String key(){
		return file.getName();
	}
	public long getLenght(){
		return file.length();
	}
	public String getPath(){
		return file.getAbsolutePath();
	}
	public long getLastModified(){
		return file.lastModified();
	}
	public void updateModified(){
		file.setLastModified(System.currentTimeMillis());
	}
	public File getFile(){
		return file;
	}
	public boolean delete(){
		return file.delete();
	}
	public boolean exists(){
		return file.exists();
	}
	@Override
	public int compare(CacheDescriptor p1, CacheDescriptor p2)
	{
		return Long.compare(p1.getLastModified(),p2.getLastModified());
	}
	public OutputStream getOutputStream(boolean append){
		try
		{
			return new FileOutputStream(file, append);
		}
		catch (FileNotFoundException e)
		{}
		return null;
	}
	public InputStream getInputStream() throws FileNotFoundException{
		return new FileInputStream(file);
	}
}
