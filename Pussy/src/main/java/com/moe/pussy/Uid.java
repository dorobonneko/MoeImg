package com.moe.pussy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Uid
{
	public static String fromString(String str){
		try
		{
			MessageDigest md=MessageDigest.getInstance("MD5");
			return byteToHex(md.digest(str.getBytes()));
		}
		catch (NoSuchAlgorithmException e)
		{}
		return null;
	}
	public static String byteToHex(byte[] bytes){
        String strHex = ;
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < bytes.length; n++) {
            strHex = Integer.toHexString(bytes[n] & 0xFF);
            sb.append((strHex.length() == 1) ? "0" + strHex : strHex); // 每个字节由两个字符表示，位数不够，高位补0
        }
        return sb.toString().trim();
    }
}
