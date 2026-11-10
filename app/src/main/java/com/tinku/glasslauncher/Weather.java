package com.tinku.glasslauncher;

import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;

/** Current weather from the free Open-Meteo service (no account or key needed). */
public class Weather {

    private static String get(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line);
            }
            r.close();
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }

    /** Returns text like "21\u00B0C Partly cloudy \u00B7 64%", or null when it could not be loaded. */
    public static String fetch(SharedPreferences prefs, String city, boolean fahrenheit) {
        try {
            String key = "wgeo:" + city.trim().toLowerCase();
            String geo = prefs.getString(key, null);
            if (geo == null) {
                String g = get("https://geocoding-api.open-meteo.com/v1/search?count=1&name="
                    + URLEncoder.encode(city.trim(), "UTF-8"));
                JSONArray res = new JSONObject(g).optJSONArray("results");
                if (res == null || res.length() == 0) return null;
                JSONObject o = res.getJSONObject(0);
                geo = o.getDouble("latitude") + "," + o.getDouble("longitude");
                prefs.edit().putString(key, geo).apply();
            }
            String[] ll = geo.split(",");
            String w = get("https://api.open-meteo.com/v1/forecast?latitude=" + ll[0]
                + "&longitude=" + ll[1]
                + "&current=temperature_2m,relative_humidity_2m,weather_code&timezone=auto"
                + (fahrenheit ? "&temperature_unit=fahrenheit" : ""));
            JSONObject cur = new JSONObject(w).getJSONObject("current");
            int t = (int) Math.round(cur.getDouble("temperature_2m"));
            int h = (int) Math.round(cur.optDouble("relative_humidity_2m", -1));
            String d = describe(cur.getInt("weather_code"));
            return t + (fahrenheit ? "\u00B0F " : "\u00B0C ") + d + (h >= 0 ? " \u00B7 " + h + "%" : "");
        } catch (Exception e) {
            return null;
        }
    }

    private static String describe(int code) {
        if (code == 0) return "Clear sky";
        if (code == 1) return "Mainly clear";
        if (code == 2) return "Partly cloudy";
        if (code == 3) return "Overcast";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Rain showers";
        if (code == 85 || code == 86) return "Snow showers";
        if (code >= 95) return "Thunderstorm";
        return "Cloudy";
    }
}
