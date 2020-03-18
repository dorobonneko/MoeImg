package com.moe.pussy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.SSLSocketFactory;
import com.moe.pussy.decode.BitmapDecoder;

public class PussyConfig
{
	static Decoder mDecoder=new BitmapDecoder();
	static String userAgent="pussy_1.0";
	static SSLSocketFactory mSSLSocketFactory;
	static int diskCacheSize=128*1024*1024;
}
