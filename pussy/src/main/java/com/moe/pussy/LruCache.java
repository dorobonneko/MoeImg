package com.moe.pussy;
import android.graphics.drawable.Drawable;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

public class  LruCache<K extends Object,V extends Drawable>
{
	private long memorySize,currentSize;
	private Callback call;
	private LinkedHashMap<K,V> list=new LinkedHashMap<>();
	public LruCache(long memorySize){
		this.memorySize=memorySize;
	}
	public void setMemoryCache(long memorySize){
		this.memorySize=memorySize;
		termToMemory();
	}
	public V get(K k){
		synchronized(list){
		V v=list.remove(k);
		if(v!=null)
			list.put(k,v);
		return v;
		}
	}
	public void set(K k,V v){
		synchronized(list){
			currentSize+=sizeOf(k,v,list.put(k,v));
			if(currentSize>=memorySize)
				termToMemory();
		}
	}
	/*算法
	新数据的大小减去旧数据的大小
	*/
	public long sizeOf(K k,V newV,V oldV){
		long newSize = 0,oldSize = 0;
		if(newV!=null)
		newSize=newV.getIntrinsicWidth()*newV.getIntrinsicHeight();
		if(oldV!=null)
		oldSize=oldV.getIntrinsicWidth()*oldV.getIntrinsicHeight();
		return newSize-oldSize;
	}
	public void termToMemory(){
		synchronized(list){
			Iterator<K> iterator=list.keySet().iterator();
			while(iterator.hasNext()){
				recycler(iterator.next());
				if(currentSize<memorySize/2)
					break;
			}
		}
	}
	public void recycler(K k){
		synchronized(list){
		V v=list.remove(k);
		if(v!=null){
			Drawable old=v;
			currentSize+=sizeOf(k,null,v);
			if(old instanceof BitmapDrawable)
				((BitmapDrawable)old).getBitmap().recycle();
			old.setCallback(null);
			if(call!=null)
				call.onRecycler(k);
		}
		}
	}
	public void clear(){
		synchronized(list){
			Iterator<K> iterator=list.keySet().iterator();
			while(iterator.hasNext()){
				recycler(iterator.next());
			}
		}
	}
	public void setCallback(Callback call){
		this.call=call;
	}
	public interface Callback<K extends Object>{
		void onRecycler(K k);
	}
}
