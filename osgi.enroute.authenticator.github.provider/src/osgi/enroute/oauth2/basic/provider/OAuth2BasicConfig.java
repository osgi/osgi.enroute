package osgi.enroute.oauth2.basic.provider;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface OAuth2BasicConfig {
	String domain();

	String authorization_endpoint();

	String token_endpoint();

	String redirect_endpoint() default "";

	String client_id();

	String client_secret();

	boolean self_authorize() default false;

}
