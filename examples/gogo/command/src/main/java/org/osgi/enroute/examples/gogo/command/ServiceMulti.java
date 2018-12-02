
package org.osgi.enroute.examples.gogo.command;

import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(immediate = true)
public class ServiceMulti {

    private final ServiceRegistration<Object> srv;

    @Activate
    public ServiceMulti(final BundleContext bc) {
        final Hashtable<String, Object> props = new Hashtable();
        props.put(Constants.COMMAND_SCOPE, "mysm");
        // If multiple functions are provided an array of string needs to be used.
        props.put(Constants.COMMAND_FUNCTION, new String[] { "foo", "bar" });
        srv = bc.registerService(Object.class, new Object() {
            public void foo() {
                System.out.println("service multi foo");
            }

            public void bar() {
                System.out.println("service multi bar");
            }
        }, props);
    }

    @Deactivate
    public void close() {
        srv.unregister();
    }

}
