package com.moe.pussy;
import android.graphics.drawable.Drawable;
import java.io.OutputStream;
import android.net.NetworkInfo;

public abstract class RequestHandler
{
	abstract boolean canHandleRequest(Request.Builder request);
	abstract void load(Pussy p,Request.Builder request,CacheDescriptor cd,Callback call );
	int getRetryCount() {
		return 0;
	}

	boolean shouldRetry(boolean airplaneMode,NetworkInfo info) {
		return false;
	}

	boolean supportsReplay() {
		return false;
	}
	public interface Callback{
		void onProgressChanged(int progress);
		void onSuccess();
		void onError(Throwable e);
	}
}
