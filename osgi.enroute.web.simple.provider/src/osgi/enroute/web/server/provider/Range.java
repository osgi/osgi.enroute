package osgi.enroute.web.server.provider;

import java.io.*;
import java.nio.channels.*;
import java.util.regex.*;

public class Range {
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
