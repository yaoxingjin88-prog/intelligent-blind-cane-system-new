package com.ruoyi.utils;

/**
 * 地理距离工具（Haversine）。
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoUtils() {
    }

    /** 两点球面距离，单位米 */
    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(latRad1) * Math.cos(latRad2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
