package com.athena.auth.service;

public final class DeviceLabel {

    private DeviceLabel() {
    }

    public static String from(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown device";
        }
        String browser = userAgent.contains("Edg") ? "Edge"
                : userAgent.contains("OPR") || userAgent.contains("Opera") ? "Opera"
                : userAgent.contains("Chrome") ? "Chrome"
                : userAgent.contains("Firefox") ? "Firefox"
                : userAgent.contains("Safari") ? "Safari"
                : "Browser";
        String os = userAgent.contains("Windows") ? "Windows"
                : userAgent.contains("Mac OS X") || userAgent.contains("Macintosh") ? "macOS"
                : userAgent.contains("Android") ? "Android"
                : userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iOS") ? "iOS"
                : userAgent.contains("Linux") ? "Linux"
                : "";
        return os.isEmpty() ? browser : browser + " on " + os;
    }
}
