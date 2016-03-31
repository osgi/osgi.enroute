package osgi.enroute.quantity.base.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UnitInfo {
	String unit();
	String symbol();
	String dimension();
	String symbolForDimension();
	String description() default "";
	String name() default "";// Use last part of class name
}
