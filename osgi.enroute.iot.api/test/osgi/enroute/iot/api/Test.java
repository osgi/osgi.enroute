package osgi.enroute.iot.api;

import java.util.Arrays;

public class Test {

	interface Foo {
		int bar();
	}

	public static void main(String[] args) {
		System.out.println("Methods " + Arrays.toString(Foo.class.getMethods()));
	}
}
