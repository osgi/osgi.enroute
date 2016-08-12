package osgi.enroute.rest.openapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Info {
	String	title() ;
	String	description() default "";
	String	termsOfService() default "";
	String	version();
	String	contactName() default "";
	String	contactUrl()  default "";
	String	contactEmail()  default "";
	String	licenseName()  default "";
	String	licenseUrl()  default "";
}
