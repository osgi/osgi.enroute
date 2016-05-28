package osgi.enroute.web.server.cache;

import java.io.*;

import aQute.lib.io.*;
import aQute.libg.cryptography.*;
import osgi.enroute.web.server.exceptions.*;

public class Etag {
	static byte[] get(File f) throws NotFound404Exception, InternalServer500Exception {
		try {
			if (!f.isFile())
				throw new NotFound404Exception(new IllegalArgumentException("not a file (anymore?) " + f));
			Digester<MD5> digester = MD5.getDigester();
			IO.copy(f, digester);
			return digester.digest().digest();
		}
		catch (NotFound404Exception e ) {
			throw e;
		}
		catch (Exception e) {
			throw new InternalServer500Exception(e);
		}
	}
}
