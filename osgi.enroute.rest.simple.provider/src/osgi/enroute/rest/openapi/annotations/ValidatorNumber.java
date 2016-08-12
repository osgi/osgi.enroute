package osgi.enroute.rest.openapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER,
		ElementType.TYPE_PARAMETER, ElementType.TYPE_USE })
public @interface ValidatorNumber {
	double maximum() default Double.MAX_VALUE;

	boolean exclusiveMaximum() default false;

	double minimum() default Double.MIN_VALUE;

	boolean exclusiveMinimum() default false;


	double multipleOf() default 1;

}
