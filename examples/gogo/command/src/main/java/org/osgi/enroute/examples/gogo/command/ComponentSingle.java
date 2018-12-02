
package org.osgi.enroute.examples.gogo.command;

import org.osgi.service.component.annotations.Component;

@Component(
        /* Activate the component immediately, also if no one depends on it. */
        immediate = true,
        /* You need to implement some class, without any it will not be picked up. */
        service = { ComponentSingle.class },
        /* Set the scope and function(s) properties. */
        property = { //
                Constants.COMMAND_SCOPE + "=mycs", //
                Constants.COMMAND_FUNCTION + "=foo" //
        })
public class ComponentSingle {
    public static void foo() {
        System.out.println("component single foo");
    }
}
