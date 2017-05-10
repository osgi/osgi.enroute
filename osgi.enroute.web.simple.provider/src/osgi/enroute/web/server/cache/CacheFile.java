package osgi.enroute.web.server.cache;

import java.io.File;
import java.util.concurrent.Future;

import org.osgi.framework.Bundle;

import aQute.lib.base64.Base64;
import aQute.lib.hex.Hex;
import osgi.enroute.web.server.cache.CacheFileFactory.Etag;
import osgi.enroute.web.server.cache.CacheFileFactory.Mimes;

/**
 * Requires some setting up, so should only be instantiated by the CacheFactory.
 */
public class CacheFile {
	public long				time;
	public String			etag;
	public String			md5;
	public File				file;
	public Bundle			bundle;
	public String			mime;
	public long				expiration;
	public boolean			publc;
	private Future<File>	future;
	public boolean			is404;

	CacheFile() {}

	CacheFile(Future<File> future) {
		this.future = future;
	}

	// Should be called on caches so that we can do any work outside the locks.
	public boolean isSynched() throws Exception {
		if (future == null)
			return file != null;

		try {
			file = future.get();
			byte[] etag = Etag.get(file);
			this.etag = Hex.toHexString(etag);
			md5 = Base64.encodeBase64(etag);
			int n = file.getAbsolutePath().lastIndexOf('.');
			if (n > 0) {
				String ext = file.getAbsolutePath().substring(n + 1);
				mime = Mimes.mimes().getProperty(ext);
			}
			return true;
		}
		catch (Exception e) {
			expiration = System.currentTimeMillis() + expiration;
			return false;
		}
	}

	public boolean isExpired() {
		if (expiration >= System.currentTimeMillis())
			return true;

		if (file == null && future != null)
			return false;

		if (!file.isFile())
			return true;

		if (time < file.lastModified())
			return true;

		if (bundle != null && bundle.getLastModified() > time)
			return true;

		return false;
	}

	public boolean isNotFound() {
		// TODO Auto-generated method stub
		return false;
	}
}
