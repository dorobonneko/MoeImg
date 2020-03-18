package com.moe.pussy;
import java.util.Map;
import java.util.HashMap;

public class ActiveResource implements Resource.OnResourceListener
{
	private Map<String,Resource> list=new HashMap<>();
	private Pussy pussy;
	public ActiveResource(Pussy pussy){
		this.pussy=pussy;
	}
	public void add(Resource res){
		res.setOnResourceListener(this);
		list.put(res.key,res);
	}
	public Resource get(String key){
		return list.get(key);
	}
	public Resource remove(String key){
		return list.remove(key);
	}
	@Override
	public void onResourceRelease(Resource res)
	{
		remove(res.key);
		pussy.getMemoryCache().put(res.key,res.bitmap);
	}

	
}
