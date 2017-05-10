package osgi.enroute.web.server.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import aQute.lib.io.IO;
import osgi.enroute.web.server.cache.CacheFile;
import osgi.enroute.web.server.config.WebServerConfig;

public class ResponseWriter {
	static SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

	static class Range {
		static String	BYTE_RANGE_SET_S	= "(\\d+)?\\s*-\\s*(\\d+)?";
		static Pattern	BYTE_RANGE_SET		= Pattern.compile(BYTE_RANGE_SET_S);
		static Pattern	BYTE_RANGE			= Pattern
				.compile("bytes\\s*=\\s*(\\d+)?\\s*-\\s*(\\d+)?(?:\\s*,\\s*(\\d+)\\s*-\\s*(\\d+)?)*\\s*");

		Range			next;
		long			start;
		long			end;

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

			try (OutputStream out = rsp.getOutputStream();) {

				Encoding encoding = getEncoding(rq.getHeader("Accept-Encoding"));
				if (encoding != Encoding.IDENTITY && isCompressableMime(rsp.getContentType()) && range.length() == c.file.length()
						&& c.file.length() > 300) {

					File source;
					switch (encoding) {
						case GZIP :
							source = setCompressor(c, rsp, "gzip", GZIPOutputStream::new);
							break;

						case DEFLATE :
						default :
							source = setCompressor(c, rsp, "deflate", DeflaterOutputStream::new);
							break;
					}
					rsp.setContentLengthLong(source.length());
					IO.copy(source, out);
					out.close();
				} else {

					rsp.setContentLengthLong(range.length());

					try (FileInputStream file = new FileInputStream(c.file);) {
						FileChannel from = file.getChannel();
						WritableByteChannel to = Channels.newChannel(out);
						range.copy(from, to);
						from.close();
						to.close();
					}
				}
			}
		}

		return true;
	}

	interface FE<P, R> {
		R get(P p) throws Exception;
	}

	private File setCompressor(CacheFile c, HttpServletResponse rsp, String type, FE<OutputStream,OutputStream> gen)
			throws Exception {
		rsp.setHeader("Content-Encoding", type);
		File f = new File(c.file.getParentFile(), c.file.getName() + "__" + type + "__");
		if (f.isFile() && f.lastModified() >= c.file.lastModified()) {
			return f;
		}

		File t = IO.createTempFile(c.file.getParentFile(), "tmp", "." + type);
		try (FileOutputStream fout = new FileOutputStream(t)) {
			try (OutputStream out = gen.get(fout);) {
				IO.copy(c.file, out);
				out.flush();
			}
		}
		Files.move(t.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
		t.renameTo(f);
		return f;
	}

	enum Encoding {
		GZIP, DEFLATE, IDENTITY;
	}

	private Encoding getEncoding(String encoding) {
		if (encoding != null) {
			String[] encodings = encoding.trim().split("\\s*,\\s*");

			for (String e : encodings) {
				e = clean(e);
				switch (e.toLowerCase()) {
					case "gzip" :
						return Encoding.GZIP;
					case "deflate" :
						return Encoding.DEFLATE;
				}
			}
		}
		return Encoding.IDENTITY;
	}

	private String clean(String e) {
		int n = e.indexOf(';');
		if (n < 0)
			return e;

		return e.substring(0, n).trim();
	}

	private boolean isCompressableMime(String mime) {
		if (mime == null)
			return false;

		// if (excludedMimeTypes.contains(mime))
		// return false;
		//
		// if (additionalMimeTypes.contains(mime))
		// return true;

		if (mime.startsWith("text/"))
			return true;

		if (mime.startsWith("application/")) {
			if (mime.endsWith("json"))
				return true;

			if (mime.endsWith("javascript"))
				return true;

			if (mime.endsWith("xml"))
				return true;

			if (mime.endsWith("yaml"))
				return true;
			if (mime.endsWith("opentype"))
				return true;
			if (mime.endsWith("fontobject"))
				return true;
			if (mime.endsWith("truetype"))
				return true;
			if (mime.endsWith("fontobject"))
				return true;

		} else if (mime.startsWith("image/")) {
			if (mime.endsWith("bmp"))
				return true;
			if (mime.endsWith("svg+xml"))
				return true;
			if (mime.endsWith("svg"))
				return true;
		} else if (mime.startsWith("font/")) {
			if (mime.endsWith("eot"))
				return true;
			if (mime.endsWith("opentype"))
				return true;
			if (mime.endsWith("truetype"))
				return true;
			if (mime.endsWith("otf"))
				return true;
		}
		return false;
	}
}
