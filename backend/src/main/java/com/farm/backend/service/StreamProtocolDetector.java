package com.farm.backend.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

@Service
public class StreamProtocolDetector {

    private static final Pattern RTSP_PATTERN = Pattern.compile("^rtsp://.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTTP_PATTERN = Pattern.compile("^http://.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTTPS_PATTERN = Pattern.compile("^https://.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEBCAM_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern MJPEG_PATTERN = Pattern.compile(".*mjpeg.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONVIF_PATTERN = Pattern.compile(".*onvif.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCAL_STRING_PATTERN = Pattern.compile("^(local|webcam|droidcam)$", Pattern.CASE_INSENSITIVE);

    public String detectProtocol(String url) {
        if (url == null) return "UNKNOWN";
        url = url.trim();

        if (WEBCAM_PATTERN.matcher(url).matches() || LOCAL_STRING_PATTERN.matcher(url).matches()) {
            return "LOCAL";
        } else if (RTSP_PATTERN.matcher(url).matches()) {
            return "RTSP";
        } else if (HTTPS_PATTERN.matcher(url).matches()) {
            return "HTTPS";
        } else if (HTTP_PATTERN.matcher(url).matches() || MJPEG_PATTERN.matcher(url).matches() || ONVIF_PATTERN.matcher(url).matches()) {
            return "HTTP";
        }

        return "UNKNOWN";
    }
}