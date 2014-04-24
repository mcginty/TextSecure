package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class MediaServer extends NanoHTTPD {
  private static final String TAG = "MediaServer";
  public  static final int    PORT = 8080;

  private final InputStream is;
  private final String      mimeType;
  private final String      nonce;

  public MediaServer(String hostname, InputStream is, String mimeType, String nonce) {
    super(hostname, PORT);
    this.is       = is;
    this.mimeType = mimeType;
    this.nonce    = nonce;
  }

  @Override
  public Response serve(IHTTPSession session) {
    Map<String, String> header = session.getHeaders();
    Map<String, String> parms = session.getParms();
    String uri = session.getUri();
    if (!("/" + nonce).equals(uri)) {
      return new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "Access denied");
    }

    Log.w(TAG, session.getMethod() + " '" + uri + "' ");

    Iterator<String> e = header.keySet().iterator();
    while (e.hasNext()) {
      String value = e.next();
      Log.w(TAG, "  HDR: '" + value + "' = '" + header.get(value) + "'");
    }
    e = parms.keySet().iterator();
    while (e.hasNext()) {
      String value = e.next();
      Log.w(TAG, "  PRM: '" + value + "' = '" + parms.get(value) + "'");
    }
    return serveStream(header);
  }

  /**
   * Serves file from homeDir and its' subdirectories (only). Uses only URI, ignores all headers and HTTP parameters.
   */
  Response serveStream(Map<String, String> header) {
    Response res;
    Map<String,String> resHeaders = new HashMap<String, String>();
    try {
      // Calculate etag
      final String etag = Integer.toHexString(is.hashCode());

      // Support (simple) skipping:
      long startFrom = 0;
      long endAt = -1;
      String range = header.get("range");
//      if (range != null) {
//        if (range.startsWith("bytes=")) {
//          range = range.substring("bytes=".length());
//          int minus = range.indexOf('-');
//          try {
//            if (minus > 0) {
//              startFrom = Long.parseLong(range.substring(0, minus));
//              endAt = Long.parseLong(range.substring(minus + 1));
//            }
//          } catch (NumberFormatException nfe) {
//            Log.w(TAG, nfe);
//          }
//        }
//      }

      // Change return code and add Content-Range header when skipping is requested
      long fileLen = is.available();
      if (range != null && startFrom >= 0) {
//        if (startFrom >= fileLen) {
          res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
          resHeaders.put("Content-Range", "bytes 0-0/" + fileLen);
          resHeaders.put("ETag", etag);
//        } else {
//          if (endAt < 0) {
//            endAt = fileLen - 1;
//          }
//          long newLen = endAt - startFrom + 1;
//          if (newLen < 0) {
//            newLen = 0;
//          }
//
//          final long dataLen = newLen;
////          FileInputStream fis = new FileInputStream(file) {
////            @Override
////            public int available() throws IOException {
////              return (int) dataLen;
////            }
////          };
//          is.skip(startFrom);
//
//          res = createResponse(Response.Status.PARTIAL_CONTENT, "video/mp4", is);
//          resHeaders.put("Content-Length", "" + dataLen);
//          resHeaders.put("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
//          resHeaders.put("ETag", etag);
//        }
      } else {
        if (etag.equals(header.get("if-none-match")))
          res = createResponse(Response.Status.NOT_MODIFIED, mimeType, "");
        else {
          res = createResponse(Response.Status.OK, mimeType, is);
          resHeaders.put("Content-Length", "" + fileLen);
          resHeaders.put("ETag", etag);
        }
      }
    } catch (IOException ioe) {
      res = createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
    }

    for (Map.Entry<String,String> resHeader : resHeaders.entrySet()) {
      res.addHeader(resHeader.getKey(), resHeader.getValue());
      Log.w(TAG, "  RSP: '" + resHeader.getKey() + "' = '" + resHeader.getValue() + "'");
    }

    Log.w(TAG, "  RES: " + res.getStatus().getDescription() + ", " + res.getMimeType());

    return res;
  }

  // Announce that the file server accepts partial content requests
  private Response createResponse(Response.Status status, String mimeType, InputStream message) {
    Response res = new Response(status, mimeType, message);
    res.addHeader("Accept-Ranges", "bytes");
    return res;
  }

  // Announce that the file server accepts partial content requests
  private Response createResponse(Response.Status status, String mimeType, String message) {
    Response res = new Response(status, mimeType, message);
    res.addHeader("Accept-Ranges", "bytes");
    return res;
  }


}
