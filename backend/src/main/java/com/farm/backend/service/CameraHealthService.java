package com.farm.backend.service;

import org.springframework.stereotype.Service;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class CameraHealthService {

    public boolean isReachable(String source) {
        try {
            URL url = new URL(source);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(2000);
            conn.connect();

            return conn.getResponseCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }
}