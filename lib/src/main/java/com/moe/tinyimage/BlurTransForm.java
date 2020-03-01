package com.moe.tinyimage;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.Element;
import android.renderscript.Allocation;
import android.content.Context;

public class BlurTransForm implements Pussy.TransForm
{
	private int level;
	private Context c;
	public BlurTransForm(Context c,int level){
		this.level=level;
		this.c=c.getApplicationContext();
	}
	@Override
	public boolean canDecode()
	{
		return false;
	}
	@Override
	public Bitmap onTransForm(Bitmap source, int w, int h)
	{
		Bitmap out_bitmap=Bitmap.createBitmap(source.getWidth(),source.getHeight(),Bitmap.Config.ARGB_8888);
		RenderScript rs=RenderScript.create(c);
		ScriptIntrinsicBlur sib=ScriptIntrinsicBlur.create(rs,Element.U8_4(rs));
		Allocation in=Allocation.createFromBitmap(rs,source);
		Allocation out=Allocation.createFromBitmap(rs,out_bitmap);
		sib.setRadius(level);
		sib.setInput(in);
		sib.forEach(out);
		out.copyTo(out_bitmap);
		rs.destroy();
		if(out_bitmap!=source)
			source.recycle();
		return out_bitmap;
	}

	@Override
	public Bitmap onTransForm(BitmapDecoder brd, BitmapFactory.Options options, int w, int h)
	{
		return null;
	}

	@Override
	public String key()
	{
		return "tiny&blur".concat(String.valueOf(level));
	}
	
}
