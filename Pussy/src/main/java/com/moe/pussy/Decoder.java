package com.moe.pussy;
import java.io.InputStream;
import java.io.File;
import android.graphics.Bitmap;

public interface Decoder
{
	public Bitmap decode(File input);
}
