package com.moe.tinyimage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Utils
{
	public static String encode(String data)
	{
		try
		{
			return byte2HexStr(MessageDigest.getInstance("MD5").digest(data.getBytes()));
		}
		catch (NoSuchAlgorithmException e)
		{
			return Base64.getEncoder().encodeToString(data.getBytes());
		}
	}
	public static int calculateInSampleSize(int width, int height, float reqWidth, float reqHeight)
	{
		int inSampleSize = 1;
		if (height > reqHeight || width > reqWidth)
		{
			final int halfHeight = height;
			final int halfWidth = width;
			while ((halfHeight / inSampleSize) > reqHeight
				   && (halfWidth / inSampleSize) > reqWidth)
			{
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}
	public static String byte2HexStr(byte[] b)
	{
		String stmp = "";
		StringBuilder sb = new StringBuilder("");
		for (int n = 0; n < b.length; n++)
		{
			stmp = Integer.toHexString(b[n] & 0xFF);
			sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
		}
		return sb.toString();
	}
	public static Bitmap drawable2Bitmap(Drawable drawable) {
		if(drawable instanceof BitmapDrawable)
			return ((BitmapDrawable)drawable).getBitmap();
		Bitmap bitmap = Bitmap
			.createBitmap(
			drawable.getIntrinsicWidth(),
			drawable.getIntrinsicHeight(),
			drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
			: Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
						   drawable.getIntrinsicHeight());
		drawable.draw(canvas);
		return bitmap;
	}
}
