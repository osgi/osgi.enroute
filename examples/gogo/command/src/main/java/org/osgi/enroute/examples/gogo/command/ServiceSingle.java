
package org.osgi.enroute.examples.gogo.command;

import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(immediate = true)
public class ServiceSingle {

    private final ServiceRegistration<Object> srv;

    @Activate
    public ServiceSingle(final BundleContext bc) {
        final Hashtable<String, Object> props = new Hashtable();
        props.put(Constants.COMMAND_SCOPE, "myss");
        // If only one function is provided a string could be used.
        props.put(Constants.COMMAND_FUNCTION, "foo");
        srv = bc.registerService(Object.class, new Object() {
            public void foo() {
                System.out.println("service single foo");
            }
        }, props);
    }

    @Deactivate
    public void close() {
        srv.unregister();
    }

}
