package com.patken.api.url_shortener.exception;

public class UrlNotFoundException extends RuntimeException{

    public UrlNotFoundException(String message){
        super(message);
    }
}
