package com.moe.tinyimage;

import android.content.*;
import java.io.*;
import javax.xml.transform.*;

import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import javax.security.auth.callback.Callback;
import com.moe.tinyimage.Pussy.RequestHandler.Response;
import java.util.Map;
import android.os.ParcelFormatException;
import android.os.ParcelFileDescriptor;

public class ContentStreamRequestHandler implements Pussy.RequestHandler
{
	final Context context;

	ContentStreamRequestHandler(Context context) {
		this.context = context;
	}

	@Override public boolean canHandle(Uri uri) {
		return uri != null &&( ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())||ContentResolver.SCHEME_FILE.equals(uri.getScheme()));
	}

	@Override
	public Pussy.RequestHandler.Response load(Uri uri, Map<String, String> header)
	{
		try
		{
			final InputStream pfd= context.getContentResolver().openInputStream(uri);
			return new Response(200, pfd, -1, new Closeable(){

					@Override
					public void close() throws IOException
					{
						pfd.close();
					}
				}, null);
		}
		catch (FileNotFoundException e)
		{}
		return null;
		}
	}
