package osgi.enroute.web.server.provider;

import java.io.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.servlet.http.*;

import osgi.enroute.web.server.cache.*;
import osgi.enroute.web.server.config.*;

public class ResponseWriter {
	static SimpleDateFormat	format							= new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",
			Locale.ENGLISH);

	static class Range {
		static String			BYTE_RANGE_SET_S				= "(\\d+)?\\s*-\\s*(\\d+)?";
		static Pattern			BYTE_RANGE_SET					= Pattern.compile(BYTE_RANGE_SET_S);
		static Pattern			BYTE_RANGE						= Pattern
				.compile("bytes\\s*=\\s*(\\d+)?\\s*-\\s*(\\d+)?(?:\\s*,\\s*(\\d+)\\s*-\\s*(\\d+)?)*\\s*");

		Range	next;
		long	start;
		long	end;

		public long length() {
			if (next == null)
				return end - start;

			return next.length() + end - start;
		}

		Range(String range, long length) {
			if (range != null) {
				if (!BYTE_RANGE.matcher(range).matches())
					throw new IllegalArgumentException("Bytes ranges does not match specification " + range);

				Matcher m = BYTE_RANGE_SET.matcher(range);
				m.find();
				init(m, length);
			} else {
				start = 0;
				end = length;
			}
		}

		private Range() {}

		void init(Matcher m, long length) {
			String s = m.group(1);
			String e = m.group(2);
			if (s == null && e == null)
				throw new IllegalArgumentException("Invalid range, both begin and end not specified: " + m.group(0));

			if (s == null) { // -n == l-n -> l
				start = length - Long.parseLong(e);
				end = length - 1;
			} else if (e == null) { // n- == n -> l
				start = Long.parseLong(s);
				end = length - 1;
			} else {
				start = Long.parseLong(s);
				end = Long.parseLong(e);
			}
			end++; // e is specified as inclusive, Java uses exclusive

			if (end > length)
				end = length;

			if (start < 0)
				start = 0;

			if (start >= end)
				throw new IllegalArgumentException("Invalid range, start higher than end " + m.group(0));

			if (m.find()) {
				next = new Range();
				next.init(m, length);
			}
		}

		void copy(FileChannel from, WritableByteChannel to) throws IOException {
			from.transferTo(start, end - start, to);
			if (next != null)
				next.copy(from, to);
		}
	}

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
