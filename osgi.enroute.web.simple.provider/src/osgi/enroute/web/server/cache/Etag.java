package osgi.enroute.web.server.cache;

import java.io.*;

import aQute.lib.io.*;
import aQute.libg.cryptography.*;

public class Etag {
	static byte[] get(File f) throws Exception {
		if (!f.isFile())
			throw new IllegalArgumentException("not a file (anymore?) " + f);
		Digester<MD5> digester = MD5.getDigester();
		IO.copy(f, digester);
		return digester.digest().digest();
	}
}
