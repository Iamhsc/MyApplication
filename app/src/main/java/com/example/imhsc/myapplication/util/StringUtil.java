package com.example.imhsc.myapplication.util;

/**
 * @author kk on 2018/1/16  13:36.
 *         Email:763744747@qq.com
 */

public class StringUtil {
    /**
     * 将字节数组转化成16进制字符串
     *
     * @param bytes
     * @return
     */
    public static String toHexStr(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {            return null;
        }
        StringBuilder iStringBuilder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            String vStr = Integer.toHexString(v);
            if (vStr.length() < 2) {
                iStringBuilder.append(0);
            }
            iStringBuilder.append(vStr);
        }
        return iStringBuilder.toString().toUpperCase();
    }

    /**
     * 将16位的short转换成byte数组
     * @param s
     * @return
     */
    public static byte[] shortToByteArray(int s) {
        byte[] targets = new byte[3];
        targets[2]=(byte)((s>>>16)&0xff);
        targets[1]=(byte)((s>>>8)&0xff);
        targets[0]=(byte)(s&0xff);

        return targets;
    }
}
