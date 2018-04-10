package rest.pkg;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

@Component(service=RestComponentImpl.class)
@JaxrsResource
public class RestComponentImpl {
    
    //TODO add an implementation
    
    @Path("rest")
    @GET
    public String toUpper() {
        return "Hello World!";
    }
    
}
