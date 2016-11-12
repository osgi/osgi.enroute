package osgi.enroute.polymer.demo.application;

import org.osgi.service.component.annotations.Component;

import osgi.enroute.configurer.api.RequireConfigurerExtender;
import osgi.enroute.polymer.app.webresource.capabilities.RequirePolymerAppWebresource;
import osgi.enroute.polymer.iron.webresource.capabilities.RequirePolymerIronWebresource;
import osgi.enroute.rest.api.REST;
import osgi.enroute.twitter.bootstrap.capabilities.RequireBootstrapWebResource;
import osgi.enroute.webcomponentsjs.webresource.capabilities.RequireWebcomponentsJSWebresource;
import osgi.enroute.webserver.capabilities.RequireWebServerExtender;

@RequireWebcomponentsJSWebresource(resource = "webcomponents-lite.min.js")
@RequirePolymerIronWebresource(resource = "*.html")
@RequirePolymerAppWebresource(resource = "*.html")
@RequireBootstrapWebResource(resource="css/bootstrap.css")
@RequireWebServerExtender
@RequireConfigurerExtender
@Component(name="osgi.enroute.polymer.demo")
public class PolymerDemoApplication implements REST {

	public String getUpper(String string) {
		return string.toUpperCase();
	}

}
