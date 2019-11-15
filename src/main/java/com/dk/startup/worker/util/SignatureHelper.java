package com.dk.startup.worker.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignatureHelper {
    public static Pattern reg = Pattern.compile("[^\"\\:,\\{\\}\\[\\]]");
    /**
     * 签名
     * @param content
     * @param appSecret
     * @return
     */
    public static String sign(String content,String appSecret) {
        StringBuilder signStringBuilder = new StringBuilder();
        Matcher m = reg.matcher(content);
        while(m.find()) {
            signStringBuilder.append(m.group());
        }
        String signString = signStringBuilder.toString();
        return DigestUtils.md5Hex(appSecret + signString);
    }

    /**
     * 验签
     * @param content
     * @param appSecret
     * @param sign
     * @return
     */
    public static boolean verifySignature(String content,String appSecret,String sign){
        return sign(content,appSecret).equals(sign);
    }
}
