package com.moe.pussy;
import java.util.Map;
import java.util.HashMap;
import android.net.Uri;

public class Request
{
	private Pussy p;
	private String url,key,location;
	private Map<String,String> header=new HashMap<>();
	private String body;
	private boolean cancel;
	public Request(Pussy p,String url){
		this.p=p;
		this.url=url;
		userAgent(p.userAgent);
	}
	public Request(Pussy p,int res){
		this.p=p;
		this.url=Uri.fromParts("drawable",String.valueOf(res),null).toString();
	}

	public boolean isCancel()
	{
		return false;
	}
	public void cancel(){
		cancel=true;
	}
	public void setLocation(String location)
	{
		this.location=location;
	}

	public Request header(String... header){
		for(int i=0;i<header.length;i+=2){
			this.header.put(header[i],header[i+1]);
		}
		return this;
	}
	public String getKey(){
		if(key==null)
			key=Uid.fromString(url);
			return key;
	}
	public String getUrl(){
		return location==null?url:location;
	}
	public Map<String,String> getHeader(){
		return header;
	}
	public String getBody(){
		return body;
	}
	public Pussy getPussy(){
		return p;
	}
	public Request userAgent(String useragent){
		if(useragent==null)
			header.remove("UserAgent");
			else
		header.put("UserAgent",useragent);
		return this;
	}
	public Request referer(String referer){
		header.put("Referer",referer);
		return this;
	}
	public Request requestBody(String body){
		this.body=body;
		return this;
	}
	public Content execute(){
		return new Content(this);
	}
	
}
