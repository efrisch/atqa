package atqa.web;

import atqa.exceptions.ForbiddenUseException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static atqa.Constants.MAX_QUERY_STRING_KEYS_COUNT;
import static atqa.utils.Invariants.mustBeTrue;
import static atqa.utils.Invariants.mustNotBeNull;
import static atqa.utils.StringUtils.decode;

/**
 * This class holds data and methods for dealing with the
 * "start line" in an HTTP request.  For example,
 * GET /foo HTTP/1.1
 */
public record StartLine(
        Verb verb,
        PathDetails pathDetails,
        WebEngine.HttpVersion version,
        /*
         * The entire line of the start line
         */
        String rawValue) {

    /**
     * This is our regex for looking at a client's request
     * and determining what to send them.  For example,
     * if they send GET /sample.html HTTP/1.1, we send them sample.html
     * <p>
     * On the other hand if it's not a well-formed request, or
     * if we don't have that file, we reply with an error page
     */
    public static final String startLinePattern = "^(GET|POST) /(.*) HTTP/(1.1|1.0)$";
    public static final Pattern startLineRegex = Pattern.compile(startLinePattern);
    public static final StartLine empty = new StartLine(Verb.NONE, PathDetails.empty, WebEngine.HttpVersion.NONE, "");

    /**
     * Returns a map of the key-value pairs in the URL,
     * for example in {@code http://foo.com?name=alice} you
     * have a key of name and a value of alice.
     */
    public Map<String, String> queryString() {
        if (pathDetails == null || pathDetails.queryString == null || pathDetails.queryString.isEmpty()) {
            return new HashMap<>();
        } else {
            return new HashMap<>(pathDetails.queryString);
        }

    }

    /**
     * These are the HTTP Verbs we handle
     */
    public enum Verb {
        GET, POST, NONE
    }

    /**
     * Given the string value of a startline (like GET /hello HTTP/1.1)
     * validate and extract the values for our use.
     */
    public static StartLine extractStartLine(String value) {
        mustNotBeNull(value);
        Matcher m = StartLine.startLineRegex.matcher(value);
        // run the regex
        var doesMatch = m.matches();
        if (!doesMatch) {
            return StartLine.empty;
        }
        mustBeTrue(doesMatch, String.format("%s must match the startLinePattern: %s", value, startLinePattern));
        Verb verb = extractVerb(m.group(1));
        PathDetails pd = extractPathDetails(m.group(2));
        WebEngine.HttpVersion httpVersion = getHttpVersion(m.group(3));

        return new StartLine(verb, pd, httpVersion, value);
    }

    private static Verb extractVerb(String verbString) {
        return Verb.valueOf(verbString.toUpperCase(Locale.ROOT));
    }

    private static PathDetails extractPathDetails(String path) {
        PathDetails pd;
        int locationOfQueryBegin = path.indexOf("?");
        if (locationOfQueryBegin > 0) {
            // in this case, we found a question mark, suggesting that a query string exists
            String rawQueryString = path.substring(locationOfQueryBegin + 1);
            String isolatedPath = path.substring(0, locationOfQueryBegin);
            Map<String, String> queryString = extractMapFromQueryString(rawQueryString);
            pd = new PathDetails(isolatedPath, rawQueryString, queryString);
        } else {
            // in this case, no question mark was found, thus no query string
            pd = new PathDetails(path, null, null);
        }
        return pd;
    }

    /**
     * Some essential characteristics of the path portion of the start line
     */
    public record PathDetails (
        // the isolated path is found after removing the query string
        String isolatedPath,

        // the raw query is the string after a question mark (if it exists - it's optional)
        // if there is no query string, then we leave rawQuery as a null value
        String rawQueryString,

        // the query is a map of the keys -> values found in the query string
        Map<String, String> queryString
    ){
        public static final PathDetails empty = new PathDetails("", "", Map.of());
    }

    /**
     * Given a string containing the combined key-values in
     * a query string (e.g. foo=bar&name=alice), split that
     * into a map of the key to value (e.g. foo to bar, and name to alice)
     */
    private static Map<String, String> extractMapFromQueryString(String rawQueryString) {
        Map<String, String> queryStrings = new HashMap<>();
        StringTokenizer tokenizer = new StringTokenizer(rawQueryString, "&");
        // we'll only take less than MAX_QUERY_STRING_KEYS_COUNT
        for (int i = 0; tokenizer.hasMoreTokens() && i <= MAX_QUERY_STRING_KEYS_COUNT; i++) {
            if (i == MAX_QUERY_STRING_KEYS_COUNT) throw new ForbiddenUseException("User tried providing too many query string keys.  Current max: " + MAX_QUERY_STRING_KEYS_COUNT);
            // this should give us a key and value joined with an equal sign, e.g. foo=bar
            String currentKeyValue = tokenizer.nextToken();
            int equalSignLocation = currentKeyValue.indexOf("=");
            if (equalSignLocation <= 0) return Map.of();
            mustBeTrue(equalSignLocation > 0, "There must be an equals sign");
            String key = currentKeyValue.substring(0, equalSignLocation);
            String value = decode(currentKeyValue.substring(equalSignLocation + 1));
            queryStrings.put(key, value);
        }
        return queryStrings;
    }

    /**
     * Extract the HTTP version from the start line
     */
    private static WebEngine.HttpVersion getHttpVersion(String version) {
        if (version.equals("1.1")) {
            return WebEngine.HttpVersion.ONE_DOT_ONE;
        } else {
            return WebEngine.HttpVersion.ONE_DOT_ZERO;
        }
    }

}
