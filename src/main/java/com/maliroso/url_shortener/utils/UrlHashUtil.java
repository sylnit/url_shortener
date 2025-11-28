package com.maliroso.url_shortener.utils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

public class UrlHashUtil {
    private static final int hashLength = 6;
    private static final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random random = new SecureRandom();;


    public static String generateMd5Hash(String url){
        return randomBase62();
    }

    private static String randomBase62(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < hashLength; i++){
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    public static Instant calculateExpiresAt(){
        //Assume that short url codes are valid for 7 days
        Duration sevenDays = Duration.ofDays(7);

        Instant now = Instant.now();

        return now.plus(sevenDays);
    }
}
