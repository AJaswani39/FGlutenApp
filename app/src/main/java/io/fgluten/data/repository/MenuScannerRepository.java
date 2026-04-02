package io.fgluten.data.repository;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fgluten.data.Restaurant;

public class MenuScannerRepository {
    private static final String TAG = "MenuScannerRepo";
    private static final int MENU_MAX_BYTES = 200_000;
    private static final String USER_AGENT = "FGlutenApp/1.0";
    
    private final Map<String, List<String>> robotsDisallowCache = new HashMap<>();

    public String fetchWebsiteForPlace(String placeId, String apiKey) {
        if (TextUtils.isEmpty(placeId) || TextUtils.isEmpty(apiKey)) {
            return null;
        }
        HttpURLConnection connection = null;
        try {
            String encodedPlaceId = placeId;
            try {
                encodedPlaceId = URLEncoder.encode(placeId, "UTF-8");
            } catch (Exception ignored) { }
            String urlStr = "https://maps.googleapis.com/maps/api/place/details/json"
                    + "?place_id=" + encodedPlaceId
                    + "&fields=website"
                    + "&key=" + apiKey;
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject root = new JSONObject(sb.toString());
            JSONObject result = root.optJSONObject("result");
            if (result != null) {
                String website = result.optString("website", null);
                if (!TextUtils.isEmpty(website)) {
                    return website;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "fetchWebsiteForPlace failed", e);
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    public String fetchHtml(String urlStr) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            if (!isAllowedByRobots(urlStr)) {
                return null;
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            String contentType = connection.getHeaderField("Content-Type");
            if (contentType != null && !contentType.toLowerCase().contains("text")) {
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[2048];
            int total = 0;
            int read;
            while ((read = reader.read(buffer)) != -1 && total < MENU_MAX_BYTES) {
                sb.append(buffer, 0, read);
                total += read;
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public String findMenuLink(String html, String baseUrl) {
        if (TextUtils.isEmpty(html)) return null;
        Pattern menuPattern = Pattern.compile("href\\s*=\\s*\"([^\"]*menu[^\"]*)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = menuPattern.matcher(html);
        while (matcher.find()) {
            String link = matcher.group(1);
            try {
                URI base = new URI(baseUrl);
                URI resolved = base.resolve(link);
                return resolved.toString();
            } catch (Exception ignored) { }
        }
        return null;
    }

    public List<String> extractGfEvidence(String html) {
        List<String> results = new ArrayList<>();
        if (TextUtils.isEmpty(html)) return results;
        String noTags = html.replaceAll("(?s)<script.*?>.*?</script>", " ")
                .replaceAll("(?s)<style.*?>.*?</style>", " ")
                .replaceAll("<[^>]+>", " ");
        String[] lines = noTags.split("\n");
        Pattern gfPattern = Pattern.compile("(?i)(gluten[-\\s]?free|\\bgf\\b|celiac|coeliac|no gluten)");
        for (String rawLine : lines) {
            String line = rawLine.trim().replaceAll("\\s{2,}", " ");
            if (line.length() < 4) continue;
            Matcher m = gfPattern.matcher(line);
            if (m.find()) {
                String snippet = line.length() > 140 ? line.substring(0, 140) : line;
                if (!results.contains(snippet)) {
                    results.add(snippet);
                    if (results.size() >= 8) break;
                }
            }
        }
        return results;
    }

    public String extractRawMenuText(String html) {
        if (html == null) return null;
        try {
            String cleaned = html.replaceAll("(?s)<(script|style)[^>]*>.*?</\\1>", " ");
            cleaned = cleaned.replaceAll("(?s)<[^>]*>", " ");
            return cleaned.replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAllowedByRobots(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String host = url.getHost();
            String path = url.getPath();
            List<String> disallows = robotsDisallowCache.get(host);
            if (disallows == null) {
                disallows = fetchRobots(host, url.getProtocol());
                robotsDisallowCache.put(host, disallows);
            }
            for (String rule : disallows) {
                if (path.startsWith(rule)) return false;
            }
        } catch (Exception e) {
            return true; // fail open
        }
        return true;
    }

    private List<String> fetchRobots(String host, String scheme) {
        List<String> disallows = new ArrayList<>();
        HttpURLConnection connection = null;
        try {
            URL robotsUrl = new URL(scheme + "://" + host + "/robots.txt");
            connection = (HttpURLConnection) robotsUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return disallows;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            boolean inStarSection = false;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.toLowerCase().startsWith("user-agent:")) {
                    inStarSection = "*".equals(line.substring("user-agent:".length()).trim());
                } else if (inStarSection && line.toLowerCase().startsWith("disallow:")) {
                    String rule = line.substring("disallow:".length()).trim();
                    if (!rule.isEmpty()) disallows.add(rule);
                }
            }
            reader.close();
        } catch (Exception ignored) {
        } finally {
            if (connection != null) connection.disconnect();
        }
        return disallows;
    }
}
