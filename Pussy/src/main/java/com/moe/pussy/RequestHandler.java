package com.moe.pussy;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import android.graphics.Bitmap;
import java.util.concurrent.ThreadPoolExecutor;

public interface RequestHandler
{
	public boolean canHandle(Request request);
	public void onHandle(ThreadPoolExecutor pool,Request request,Callback callback);
	public class Response{
		public File get() {return null;}
		public void close(){}
		public Bitmap getBitmap(){return null;}
	}
	public interface Callback{
		void onSuccess(Response response);
		void onError(Throwable e);
	}
}
