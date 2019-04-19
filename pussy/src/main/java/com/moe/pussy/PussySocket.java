package com.moe.pussy;
import java.io.InputStream;
import java.util.Map;
import java.net.Socket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.io.OutputStream;
import android.net.Uri;

public class PussySocket
{
	private Socket mSocket;
	private boolean canUse=true;
	private String hostAndPort;
	public PussySocket(String hostAndPort) throws IOException, NumberFormatException{
		mSocket=new Socket();
		this.hostAndPort=hostAndPort;
		int index=hostAndPort.lastIndexOf(":");
		mSocket.setKeepAlive(true);
		mSocket.connect(new InetSocketAddress(index==-1?hostAndPort:hostAndPort.substring(0,index),index==-1?80:Integer.parseInt(hostAndPort.substring(index+1))));
	}
	public String getHostAndPort(){
		return hostAndPort;
	}
	public boolean canUse(){
		return canUse;
	}
	public Response execute(Request.Builder rr) throws IOException{
		canUse=false;
		Uri uri=rr.getUri();
		try
		{
			OutputStream output=mSocket.getOutputStream();
			output.write("GET ".concat(uri.getPath().concat(uri.getQuery() == null ?"": ("?").concat(uri.getQuery()))).concat("HTTP/1.1\r\n").getBytes());
			output.write("Host: ".concat(uri.getHost()).getBytes());
			output.write("Connection: Keep-Alive\r\n".getBytes());
			output.write("\r\n".getBytes());
			output.flush();
		}
		catch (IOException e)
		{
			canUse=true;
			throw e;
		}
		return new Response(this);
	}
	public class Response{
		private PussySocket ps;
		private int code;
		private String http_v;
		private String state_des;
		private Map<String,String> header;
		Response(PussySocket ps){
			this.ps=ps;
		}
		void readCode() throws IOException{
			InputStream input=getInputStream();
			char c=0;
			StringBuilder sb=new StringBuilder();
			while((c=(char)(input.read()&0xf))!='\n'){
				if(c=='\r')continue;
				sb.append(c);
			}
			int index=sb.indexOf(" ");
			http_v=sb.substring(0,index);
			
		}
		void readHeader(){}
		public int code() throws IOException{
			if(code==0)
				readCode();
			return code;
		}
		public Map<String,String> getHeader(){
			if(header==null)
				readHeader();
				return header;
		}
		public InputStream getInputStream() throws IOException{
			try
			{
				return ps.mSocket.getInputStream();
			}
			catch (IOException e)
			{
				close();
				throw e;
			}
		}
		public void close(){
			ps.canUse=true;
		}
	}
}
