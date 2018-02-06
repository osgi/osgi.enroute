package org.osgi.enroute.examples.microservice.rest.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.service.jaxrs.runtime.JaxrsServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT;

import java.util.Collections;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.annotation.bundle.Capability;
import org.osgi.enroute.examples.microservice.dao.PersonDao;
import org.osgi.enroute.examples.microservice.dao.dto.PersonDTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.util.converter.Converters;
import org.osgi.util.tracker.ServiceTracker;

@Capability(namespace=SERVICE_NAMESPACE, 
    attribute="objectClass=org.osgi.enroute.examples.microservice.dao.PersonDao")
public class RestServiceIntegrationTest {

    private final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
    
    private PersonDao mockDAO;
    
    private ServiceRegistration<PersonDao> registration;

    private ServiceTracker<JaxrsServiceRuntime, JaxrsServiceRuntime> runtimeTracker;
    
    private ServiceTracker<ClientBuilder, ClientBuilder> clientTracker;

    private JaxrsServiceRuntime jaxrsServiceRuntime;

    private Client client;
    
    @Before
    public void setUp() throws Exception {
        assertNotNull("OSGi Bundle tests must be run inside an OSGi framework", bundle);
        
        mockDAO = mock(PersonDao.class);

        runtimeTracker = new ServiceTracker<>(bundle.getBundleContext(), JaxrsServiceRuntime.class, null);
        runtimeTracker.open();
        
        clientTracker = new ServiceTracker<>(bundle.getBundleContext(), ClientBuilder.class, null);
        clientTracker.open();
        
        jaxrsServiceRuntime = runtimeTracker.waitForService(2000);
        assertNotNull(jaxrsServiceRuntime);
        
        ClientBuilder cb = clientTracker.getService();
        assertNotNull(cb);
        client = cb.build();
    }
    
    @After
    public void tearDown() throws Exception {
        runtimeTracker.close();
        
        clientTracker.close();
        
        if(registration != null) {
            registration.unregister();
        }
    }
    
    private void registerDao() {
        registration = bundle.getBundleContext().registerService(PersonDao.class, mockDAO, null);
    }
    
    @Test
    public void testRestServiceRegistered() throws Exception {
        
        assertEquals(0, jaxrsServiceRuntime.getRuntimeDTO().defaultApplication.resourceDTOs.length);
        
        registerDao();
        
        assertEquals(1, jaxrsServiceRuntime.getRuntimeDTO().defaultApplication.resourceDTOs.length);
    }

    @Test
    public void testGetPerson() throws Exception {
        
        registerDao();
        
        // Set up a Base URI
        String base = Converters.standardConverter().convert(
                runtimeTracker.getServiceReference().getProperty(JAX_RS_SERVICE_ENDPOINT)).to(String.class);
        WebTarget target = client.target(base);
        
        // There should be no results in the answer
        assertEquals("[]", target.path("person")
            .request()
            .get(String.class));
        
        // Add a person to the DAO
        PersonDTO dto = new PersonDTO();
        dto.firstName = "Fizz";
        dto.lastName = "Buzz";
        dto.personId = 42;
        dto.addresses = Collections.emptyList();
        
        Mockito.when(mockDAO.select()).thenReturn(Collections.singletonList(dto));
        
        // We should get back the person in the answer
        assertEquals("[{\"firstName\":\"Fizz\",\"lastName\":\"Buzz\",\"addresses\":[],\"personId\":42}]", 
                target.path("person")
                    .request()
                    .get(String.class));
        
    }
}
