package com.renomad.minum.web;

import com.renomad.minum.Context;
import com.renomad.minum.utils.StringUtils;

import java.util.Map;

/**
 * This class represents the body of an HTML message.
 * See <a href="https://en.wikipedia.org/wiki/HTTP_message_body">Message Body on Wikipedia</a>
 *<br>
 * <pre>{@code
 * This could be a response from the web server:
 *
 * HTTP/1.1 200 OK
 * Date: Sun, 10 Oct 2010 23:26:07 GMT
 * Server: Apache/2.2.8 (Ubuntu) mod_ssl/2.2.8 OpenSSL/0.9.8g
 * Last-Modified: Sun, 26 Sep 2010 22:04:35 GMT
 * ETag: "45b6-834-49130cc1182c0"
 * Accept-Ranges: bytes
 * Content-Length: 12
 * Connection: close
 * Content-Type: text/html
 *
 * Hello world!
 * }</pre>
 * <p>
 *     The message body (or content) in this example is the text <pre>Hello world!</pre>.
 * </p>
 */
public final class Body {

    public static final byte[] EMPTY_BYTES = new byte[0];
    private final Map<String, byte[]> bodyMap;
    private final byte[] raw;
    private final Context context;
    private final Map<String, Headers> headers;

    public static Body EMPTY(Context context) {
        return new Body(Map.of(), EMPTY_BYTES, Map.of(), context);
    }

    /**
     * Make a hot bod
     * @param bodyMap a map of key-value pairs, presumably extracted from form data.  Empty
     *                if our body isn't one of the form data protocols we understand.
     * @param raw the raw bytes of this body
     * @param partitionHeaders if the body is of type form/multipart, each partition will have its own headers,
     *                         including content length and content type, and possibly more.
     */
    public Body(Map<String, byte[]> bodyMap, byte[] raw, Map<String, Headers> partitionHeaders, Context context) {
        this.bodyMap = bodyMap;
        this.raw = raw;
        this.context = context;
        this.headers = partitionHeaders;
    }

    /**
     * Return the body as a string, presuming
     * that the Request body data is organized
     * as key-value pairs.
     */
    public String asString(String key) {
        byte[] byteArray = bodyMap.get(key);
        if (byteArray == null) {
            return "";
        } else {
            return StringUtils.byteArrayToString(byteArray).trim();
        }

    }

    /**
     * Return the entire raw contents of the body of this
     * request, as a string. No processing involved other
     * than converting the bytes to a string.
     */
    public String asString() {
        return StringUtils.byteArrayToString(raw).trim();
    }

    /**
     * Return the bytes of this request body by its
     * key.  HTTP requests often
     * organize the data as key-value pairs,
     * and thus if you were expecting that organization,
     * this will get the value by its key.
     */
    public byte[] asBytes(String key) {
        return bodyMap.get(key);
    }

    /**
     * Returns the raw bytes of this HTTP message's body
     */
    public byte[] asBytes() {
        return this.raw;
    }

    /**
     * If the body is of type form/multipart, return the headers
     * for a particular partition.
     * <p>
     *     For example, given a partition with the name text1,
     *     as seen in the following example, the headers
     *     would be
     * </p>
     * <pre>
     * Content-Type: text/plain
     * Content-Disposition: form-data; name="text1"
     * </pre>
     * <pr>
     *     Here is the multipart data:
     * </pr>
     * <pre>
     * --i_am_a_boundary
     *  Content-Type: text/plain
     *  Content-Disposition: form-data; name="text1"
     *
     *  I am a value that is text
     *  --i_am_a_boundary
     *  Content-Type: application/octet-stream
     *  Content-Disposition: form-data; name="image_uploads"; filename="photo_preview.jpg"
     * </pre>
     */
    public Headers partitionHeaders(String partitionName) {
        return this.headers.get(partitionName);
    }
}
