package com.moe.pussy;
import android.graphics.Bitmap;
import java.io.File;

public class BitmapLoader implements Runnable
{
	private String key;
	private File file;
	private Decoder decoder;
	private Callback call;
	public BitmapLoader(Decoder decoder,String key,File file,Callback call){
		this.key=key;
		this.file=file;
		this.decoder=decoder;
		this.call=call;
	}
	@Override
	public void run()
	{
		call.onSuccess(key,decoder.decode(file),file);
	}

	
	public interface Callback{
		void onSuccess(String key,Bitmap bitmap,File file);
	}
}
