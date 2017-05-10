package osgi.enroute.authenticator.simple.provider;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface Config {
	public enum Algorithm {
		PBKDF2WithHmacSHA1
	}

	byte[] salt() default {
			0x2f, 0x68, (byte) 0xcb, 0x75, 0x6c, (byte) 0xf1, 0x74, (byte) 0x84, 0x2a, (byte) 0xef
	};

	int iterations() default 997;

	Algorithm algorithm() default Algorithm.PBKDF2WithHmacSHA1;
	
	String _root() default "";
}
