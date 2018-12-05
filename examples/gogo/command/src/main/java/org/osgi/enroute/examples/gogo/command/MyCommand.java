
package org.osgi.enroute.examples.gogo.command;

import org.osgi.enroute.examples.gogo.command.upstream.annotations.RequireCommand;
import org.osgi.enroute.examples.gogo.command.upstream.propertytypes.Command;
import org.osgi.service.component.annotations.Component;

@Component(service = { MyCommand.class } /* You need to provide a class, without it will not be picked up. */)
@Command(scope = "mycmd", function = { "foo", "bar" })
@RequireCommand
public class MyCommand {
    public static void foo() {
        System.out.println("my command foo");
    }

    public static void bar() {
        System.out.println("my command bar");
    }
}
