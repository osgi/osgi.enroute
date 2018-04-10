package org.osgi.enroute.examples.microservice.rest;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.osgi.enroute.examples.microservice.dao.PersonDao;
import org.osgi.enroute.examples.microservice.dao.dto.PersonDTO;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardResource;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

@Component(service=RestComponentImpl.class)
@JaxrsResource
@Path("person")
@Produces(MediaType.APPLICATION_JSON)
@JSONRequired
@HttpWhiteboardResource(pattern="/microservice/*", prefix="static")
public class RestComponentImpl {
    
	@Reference
	private PersonDao personDao;

	@GET
	@Path("{person}")
	public PersonDTO getPerson(@PathParam("person") Long personId) {
		return personDao.findByPK(personId);
	}

	@GET
	public List<PersonDTO> getPerson() {
		return personDao.select();
	}

	@DELETE
	@Path("{person}")
	public boolean deletePerson(@PathParam("person") long personId) {
		personDao.delete(personId);
		return true;
	}

	@POST
	public PersonDTO postPerson(PersonDTO person) {
		if (person.personId > 0) {
			personDao.update(person);
			return person;
		}
		else {
			long id = personDao.save(person);
			person.personId = id;
			return person;
		}
	}
}
