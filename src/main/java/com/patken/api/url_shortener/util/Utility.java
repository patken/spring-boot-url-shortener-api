package com.patken.api.url_shortener.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class Utility {

    private Utility(){}

    private static final String CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String buildShortenUrl(){
        long currentTimestamp = System.currentTimeMillis();
        var stringBuilder = new StringBuilder();
        var random = new Random();
        for (int i = 0; i < 10; i++){
            if(i % 2 == 0){
                var ind = random.nextInt(CHARACTERS.length());
                stringBuilder.append(CHARACTERS.charAt(ind));
            } else {
                stringBuilder.append((currentTimestamp % 10));
                currentTimestamp /= 10;
            }
        }
        return stringBuilder.toString();
    }
}
