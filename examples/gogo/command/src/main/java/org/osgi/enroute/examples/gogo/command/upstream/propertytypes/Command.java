// Needs to be moved upstream

package org.osgi.enroute.examples.gogo.command.upstream.propertytypes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.enroute.examples.gogo.command.upstream.annotations.RequireCommand;
import org.osgi.service.component.annotations.ComponentPropertyType;

@ComponentPropertyType
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@RequireCommand
public @interface Command {
    /**
     * Prefix for the property name. This value is prepended to each property name.
     */
    String PREFIX_ = "osgi.command.";

    String scope();

    String[] function();

}
