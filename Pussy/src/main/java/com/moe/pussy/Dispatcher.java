package com.moe.pussy;
import java.util.List;
import java.util.ArrayList;
import com.moe.pussy.handle.ResourceRequestHandler;
import android.content.Context;
import com.moe.pussy.handle.HttpRequestHandler;

public class Dispatcher
{
	private List<RequestHandler> handlers=new ArrayList<>();
	public Dispatcher(){}
	public static Dispatcher getDefault(Context context){
		Dispatcher d=new Dispatcher();
		d.add(new HttpRequestHandler());
		d.add(new ResourceRequestHandler(context));
		return d;
	}
	public void add(RequestHandler handler){
		handlers.add(handler);
	}
	public RequestHandler getHandler(Request request){
		for(RequestHandler h:handlers){
			if(h.canHandle(request))
				return h;
		}
		return null;
	}
}
