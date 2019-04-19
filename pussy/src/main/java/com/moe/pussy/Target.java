package com.moe.pussy;
import android.graphics.drawable.Drawable;

public interface Target
{
	void onLoadPrepared(Request request,Drawable placeHolder);
	void onProgressChanged(int progress);
	void onLoadFail(Throwable e,Drawable error);
	void onLoadSuccess(Drawable img);
}
