
package org.osgi.enroute.examples.gogo.command;

import org.osgi.service.component.annotations.Component;

@Component(
        /* Activate the component immediately, also if no one depends on it. */
        immediate = true,
        /* You need to implement some class, without any it will not be picked up. */
        service = { ComponentMulti.class },
        /*
         * Set the scope and function(s) properties.
         * If multiple functions are provided repeat the function property for all of them.
         */
        property = { //
                Constants.COMMAND_SCOPE + "=mycm", //
                Constants.COMMAND_FUNCTION + "=foo", //
                Constants.COMMAND_FUNCTION + "=bar" //
        })
public class ComponentMulti {
    public static void foo() {
        System.out.println("component multi foo");
    }

    public static void bar() {
        System.out.println("component multi bar");
    }
}
