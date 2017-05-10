package osgi.enroute.gogo.shell.provider;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnRouteCommands {

	public List<?> asList(Object array) {
		if (!array.getClass().isArray())
			return Collections.singletonList(array);

		int l = Array.getLength(array);
		List<Object> list = new ArrayList<>();
		for (int i = 0; i < l; i++)
			list.add(Array.get(array, i));

		return list;
	}
}
