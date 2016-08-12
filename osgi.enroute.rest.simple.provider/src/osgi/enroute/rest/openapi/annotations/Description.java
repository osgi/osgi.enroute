package osgi.enroute.rest.openapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.CONSTRUCTOR, ElementType.TYPE, ElementType.FIELD,
		ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE,
		ElementType.ANNOTATION_TYPE, ElementType.PACKAGE,
		ElementType.TYPE_PARAMETER, ElementType.TYPE_USE })
public @interface Description {
	String[] value() default {};
	
}
