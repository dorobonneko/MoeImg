package com.moe.pussy.handle;
import com.moe.pussy.Request;
import com.moe.pussy.RequestHandler;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import com.moe.pussy.RequestHandler.Response;
import java.util.concurrent.ThreadPoolExecutor;

public class HandleThread implements Runnable,RequestHandler.Callback
{
	private RequestHandler.Response response;
	private Request request;
	private List<Callback> calls=new CopyOnWriteArrayList<>();
	private boolean success;
	private ThreadPoolExecutor pool;
	private int error;
	public HandleThread(Request request,ThreadPoolExecutor pool){
		this.request=request;
		this.pool=pool;
		pool.execute(this);
	}

	public void cancel()
	{
		request.cancel();
	}
	public void addCallback(Callback call){
		if(!success)
		calls.add(call);
		else
			call.onResponse(response);
	}
	public void removeCallback(Callback call){
		calls.remove(call);
	}
	@Override
	public void run()
	{
		if(success)return;
		RequestHandler h=request.getPussy().getDispatcher().getHandler(request);
		if(h!=null)
			h.onHandle(pool,request,this);
		
	}

	@Override
	public void onSuccess(RequestHandler.Response response)
	{
		this.response=response;
		success=true;
		for(Callback call:calls){
			call.onResponse(response);
		}
	}

	@Override
	public void onError(Throwable e)
	{
		error++;
		if(error<3)
			pool.execute(this);
	}


	
	public interface Callback{
		void onResponse(RequestHandler.Response response);
	}
}
