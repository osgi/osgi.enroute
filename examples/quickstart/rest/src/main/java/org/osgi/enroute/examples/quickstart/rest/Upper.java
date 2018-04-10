package org.osgi.enroute.examples.quickstart.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardResource;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

@Component(service=Upper.class)
@JaxrsResource
@HttpWhiteboardResource(pattern="/quickstart/*", prefix="static")
public class Upper {
    
    @Path("rest/upper/{param}")
    @GET
    public String toUpper(@PathParam("param") String param) {
        return param.toUpperCase();
    }
}
