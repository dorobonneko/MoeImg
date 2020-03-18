package com.moe.pussy;
import android.graphics.Bitmap;

public class Resource
{
	String key;
	Bitmap bitmap;
	int count;
	private OnResourceListener listener;
	public Resource(String key,Bitmap bitmap){
		this.key=key;
		this.bitmap=bitmap;
	}
	public void acquire(){
		count++;
	}
	public void release(){
		if(--count==0)
			listener.onResourceRelease(this);
	}
	public void setOnResourceListener(OnResourceListener l){
		listener=l;
	}
	public interface OnResourceListener{
		void onResourceRelease(Resource res);
	}
}
