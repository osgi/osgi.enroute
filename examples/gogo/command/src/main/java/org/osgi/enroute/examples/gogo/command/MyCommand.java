
package org.osgi.enroute.examples.gogo.command;

import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.service.component.annotations.Component;

@Component(service = {
        MyCommand.class } /* You need to provide the command as a service or it it will not be picked up. */)
@GogoCommand(scope = "mycmd", function = { "foo", "bar" })
public class MyCommand {
    public static void foo() {
        System.out.println("my command foo");
    }

    public static void bar() {
        System.out.println("my command bar");
    }
}
