package org.osgi.enroute.examples.microservice.rest;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.osgi.enroute.examples.microservice.dao.dto.AddressDTO;
import org.osgi.enroute.examples.microservice.dao.dto.PersonDTO;

public class JsonConverterTest {
    
    @Test
    public void testJSONSerialization() throws WebApplicationException, IOException {
        
        JsonpConvertingPlugin<PersonDTO> plugin = new JsonpConvertingPlugin<>();
        
        
        PersonDTO dto = new PersonDTO();
        dto.firstName = "Tim";
        dto.lastName = "Ward";
        dto.personId = 1234;
        
        AddressDTO dto2 = new AddressDTO();
        dto2.city = "London";
        dto2.country = "UK";
        dto2.personId = dto.personId;
        dto2.emailAddress = "tim.ward@paremus.com";
        
        dto.addresses.add(dto2);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        plugin.writeTo(dto, PersonDTO.class, PersonDTO.class, null, MediaType.APPLICATION_JSON_TYPE, null, baos);
        
        PersonDTO roundTrip = plugin.readFrom(PersonDTO.class, PersonDTO.class,null, MediaType.APPLICATION_JSON_TYPE, 
                null, new ByteArrayInputStream(baos.toByteArray()));
        
        assertEquals(1234, roundTrip.personId);
        assertEquals("Tim", roundTrip.firstName);
        assertEquals("Ward", roundTrip.lastName);
        
        AddressDTO roundTrip2 = roundTrip.addresses.get(0);
        assertEquals(1234, roundTrip2.personId);
        assertEquals("London", roundTrip2.city);
        assertEquals("UK", roundTrip2.country);
        assertEquals("tim.ward@paremus.com", roundTrip2.emailAddress);
    }
    
}
