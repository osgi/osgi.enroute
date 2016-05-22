package osgi.enroute.web.server.cache;

import java.util.*;

public class Mimes {
	static Properties				mimes							= new Properties();

	static Properties mimes() {
		Properties copy = new Properties(mimes);
		return copy;
	}
}
