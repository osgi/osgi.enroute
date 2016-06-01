package osgi.enroute.web.server.provider;

import java.io.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;

import javax.servlet.http.*;

import osgi.enroute.web.server.cache.*;
import osgi.enroute.web.server.config.*;

public class ResponseWriter {
	static SimpleDateFormat	format							= new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",
			Locale.ENGLISH);

	private final WebServerConfig config;

	public ResponseWriter(WebServerConfig config) {
		this.config = config;
	}

	boolean writeResponse(HttpServletRequest rq, HttpServletResponse rsp, CacheFile c) throws Exception {
		rsp.setDateHeader("Last-Modified", c.time);
		rsp.setHeader("Etag", c.etag);
		rsp.setHeader("Content-MD5", c.md5);
		rsp.setHeader("Allow", "GET, HEAD");
		rsp.setHeader("Accept-Ranges", "bytes");

		long diff = 0;
		if (c.expiration != 0)
			diff = c.expiration - System.currentTimeMillis();
		else {
			diff = config.expiration();
			if (diff == 0)
				diff = 120000;
		}

		if (diff > 0) {
			rsp.setHeader("Cache-Control", "max-age=" + diff / 1000);
		}

		if (c.mime != null)
			rsp.setContentType(c.mime);

		if (c.is404)
			rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);

		Range range = new Range(rq.getHeader("Range"), c.file.length());
		long length = range.length();
		if (length >= Integer.MAX_VALUE)
			throw new IllegalArgumentException("Range to read is too high: " + length);

		rsp.setContentLength((int) range.length());

		if (config.expires() != 0) {
			Date expires = new Date(System.currentTimeMillis() + 60000 * config.expires());
			rsp.setHeader("Expires", format.format(expires));
		}

		String ifModifiedSince = rq.getHeader("If-Modified-Since");
		if (ifModifiedSince != null) {
			long time = 0;
			try {
				synchronized (format) {
					time = format.parse(ifModifiedSince).getTime();
				}
				if (time > c.time) {
					rsp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					return true;
				}
			}
			catch (Exception e) {
				// e.printStackTrace();
			}
		}

		String ifNoneMatch = rq.getHeader("If-None-Match");
		if (ifNoneMatch != null) {
			if (ifNoneMatch.indexOf(c.etag) >= 0) {
				rsp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return true;
			}
		}

		if (rq.getMethod().equalsIgnoreCase("GET")) {

			rsp.setContentLengthLong(range.length());
			OutputStream out = rsp.getOutputStream();

			try (FileInputStream file = new FileInputStream(c.file);) {
				FileChannel from = file.getChannel();
				WritableByteChannel to = Channels.newChannel(out);
				range.copy(from, to);
				from.close();
				to.close();
			}

			out.flush();
			out.close();
			rsp.getOutputStream().flush();
			rsp.getOutputStream().close();
		}

		return true;
	}
}
