package org.apache.roller.weblogger.business.translation;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Shared HTTP and JSON utility for translation providers.
 *
 * Encapsulates:
 * - POST-JSON HTTP calls (preventing code duplication across providers)
 * - JSON string encoding (single source of truth)
 * - JSON string-value parsing (escape-safe, used when parsing API responses)
 */
public final class HttpHelper {

    private static final Log log = LogFactory.getLog(HttpHelper.class);
    private static final int TIMEOUT_MS = 10_000;

    private HttpHelper() {
        // Utility class — not instantiatable
    }

    /**
     * Sends a JSON GET request to the given URL.
     *
     * @param urlString Target URL
     * @param headers   Optional additional header key-value pairs
     * @return Response body as a String
     * @throws IOException if the connection fails or returns a non-2xx status
     */
    public static String getJson(String urlString, String... headers) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            for (int i = 0; i + 1 < headers.length; i += 2) {
                conn.setRequestProperty(headers[i], headers[i + 1]);
            }

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                String error = readStream(conn.getErrorStream());
                log.error("HTTP " + status + " from " + urlString + ": " + error);
                throw new IOException("HTTP " + status + ": " + error);
            }

            return readStream(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Sends a JSON POST request to the given URL.
     *
     * @param urlString   Target URL
     * @param requestBody JSON string to send as the request body
     * @param headers     Optional additional header key-value pairs (flat array:
     *                    key, value, key, value...)
     * @return Response body as a String
     * @throws IOException if the connection fails or returns a non-2xx status
     */
    public static String postJson(String urlString, String requestBody, String... headers) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            // Apply additional headers (key, value pairs)
            for (int i = 0; i + 1 < headers.length; i += 2) {
                conn.setRequestProperty(headers[i], headers[i + 1]);
            }

            byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
            }

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                String error = readStream(conn.getErrorStream());
                log.error("HTTP " + status + " from " + urlString + ": " + error);
                throw new IOException("HTTP " + status + ": " + error);
            }

            return readStream(conn.getInputStream());

        } finally {
            conn.disconnect();
        }
    }

    /**
     * Encodes a Java string as a JSON string literal (with surrounding quotes).
     * Handles null, backslash, double-quote, and common whitespace escapes.
     * Single source of truth — use this in all providers instead of private copies.
     *
     * Example: jsonString("hello\nworld") → "\"hello\\nworld\""
     */
    public static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    /**
     * Basic unescaping for JSON string values.
     * Handles \\uXXXX, \n, \r, \t, \\, \", etc.
     */
    public static String unescapeJsonString(String s) {
        if (s == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"':
                        sb.append('"');
                        i++;
                        break;
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case 'n':
                        sb.append('\n');
                        i++;
                        break;
                    case 'r':
                        sb.append('\r');
                        i++;
                        break;
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    case 'u':
                        if (i + 5 < s.length()) {
                            try {
                                String hex = s.substring(i + 2, i + 6);
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 5;
                            } catch (NumberFormatException e) {
                                sb.append(ch);
                            }
                        } else {
                            sb.append(ch);
                        }
                        break;
                    default:
                        sb.append(ch);
                        break;
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Finds the closing quote of a JSON string value, correctly skipping
     * backslash-escaped characters (e.g. \").
     *
     * @param s    The JSON response string
     * @param from Index of the first character INSIDE the string value (after
     *             opening quote)
     * @return Index of the closing quote, or -1 if not found
     */
    public static int findStringEnd(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++; // skip the escaped character
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    // -------------------------------------------------------------------------

    private static String readStream(java.io.InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
