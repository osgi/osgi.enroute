package org.osgi.enroute.examples.microservice.dao.dto;

import java.util.ArrayList;
import java.util.List;

public class PersonDTO {

	public long personId;
	public String firstName;
	public String lastName;

	public List<AddressDTO> addresses = new ArrayList<>();
}