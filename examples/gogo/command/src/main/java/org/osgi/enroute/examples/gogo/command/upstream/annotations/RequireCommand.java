// Needs to be moved upstream

package org.osgi.enroute.examples.gogo.command.upstream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Requirement;
import org.osgi.annotation.bundle.Requirements;

/**
 * This annotation can be used to require the OSGi command implementation.
 * It can be used directly, or as a meta-annotation.
 * <p>
 * This annotation is applied to the command property annotations meaning that it does not normally need to be
 * applied to Declarative Services components itself.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.PACKAGE })
@Requirements(value = { //
        @Requirement(namespace = "osgi.identity", name = "org.apache.felix.gogo.shell"), //
        @Requirement(namespace = "osgi.identity", name = "org.apache.felix.gogo.command"), //
})
public @interface RequireCommand {
    // This is a marker annotation.
}
