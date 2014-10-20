package osgi.enroute.everything.application;

import org.osgi.dto.DTO;
import org.osgi.service.component.annotations.Component;

import osgi.enroute.capabilities.AngularUIWebResource;
import osgi.enroute.capabilities.AngularWebResource;
import osgi.enroute.capabilities.BootstrapWebResource;
import osgi.enroute.capabilities.WebServerExtender;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTRequest;

@AngularWebResource.Require
@BootstrapWebResource.Require
@AngularUIWebResource.Require
@WebServerExtender.Require
@Component(name = "osgi.enroute.everything")
public class EverythingApplication implements REST {

	public static class SignupData extends DTO {
		public String name;
		public long time;
	}

	interface SignupRequest extends RESTRequest {
		SignupData _body();
	}

	public String postSignup(SignupRequest rq) {
		SignupData body = rq._body();
		return "Welcome " + body.name;
	}

}
