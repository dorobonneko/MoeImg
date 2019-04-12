package utils;
import android.net.Uri;
import java.util.List;

public class MoeImg
{
	public static final String PREFIX="http://moeimg.net";
	public static final String CATEGORY="category";
	public static final String TAG="tag";
	public static final String TAGLIST="taglist";
	public static final String TAGCLOUD="tagcloud";
	public static String changePage(String url,int page){
		Uri uri=Uri.parse(url);
		StringBuilder sb=new StringBuilder();
		sb.append(uri.getScheme()).append("://").append(uri.getAuthority());
		
		try
		{
			Integer.parseInt(uri.getLastPathSegment());
			List<String> segs=uri.getPathSegments();
			for(String pathSeg:segs.subList(0,segs.size()-1)){
				sb.append("/").append(pathSeg);
			}
			sb.append("/");
			sb.append(page);
			sb.append("?");
			sb.append(uri.getQuery());
			return sb.toString();
		}
		catch (NumberFormatException e)
		{
			List<String> segs=uri.getPathSegments();
			for(String pathSeg:segs){
				sb.append("/").append(pathSeg);
			}
			sb.append("/page/");
			sb.append(page);
			sb.append("?");
			sb.append(uri.getQuery());
			return sb.toString();
		}
		
	}
	public static int getPage(String url){
		Uri uri=Uri.parse(url);
		try
		{
			return Integer.parseInt(uri.getLastPathSegment());
		}
		catch (NumberFormatException e)
		{}return 1;
	}
}
