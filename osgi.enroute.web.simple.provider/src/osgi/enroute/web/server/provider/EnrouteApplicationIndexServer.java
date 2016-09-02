package osgi.enroute.web.server.provider;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.libg.sed.ReplacerAdapter;
import osgi.enroute.servlet.api.ConditionalServlet;
import osgi.enroute.web.server.cache.Cache;
import osgi.enroute.web.server.cache.CacheFile;
import osgi.enroute.web.server.config.WebServerConfig;
import osgi.enroute.web.server.exceptions.ExceptionHandler;

@Component(
		service = { ConditionalServlet.class }, 
		immediate = true, 
		property = {
				"service.ranking:Integer=1001", 
				"name=" + EnrouteApplicationIndexServer.NAME,
				"addTrailingSlash=true"
		}, 
		name = EnrouteApplicationIndexServer.NAME, 
		configurationPid = BundleMixinServer.NAME,
		configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class EnrouteApplicationIndexServer implements ConditionalServlet {

	static final String NAME = "osgi.enroute.simple.web.application";

	public static class IndexDTO extends DTO {
		public List<ApplicationDTO>	applications = new ArrayList<>();
		public Map<String,Object> configuration;		
	}

	public static class ApplicationDTO extends DTO {
		public long			bundle;
		public String		name;
		public String		bsn;
		public String		version;
		public String		link;
		public String		description;
	}

	WebServerConfig								config;
	private BundleTracker<Bundle>				applicationTracker;
	private Cache								cache;
	private ExceptionHandler					exceptionHandler;
	private LogService							log;
	IndexDTO									index = new IndexDTO();
	BundleContext								context;

	@Activate
	void activate(WebServerConfig config, Map<String,Object> props, BundleContext context) throws Exception {
		this.context = context;
		index.configuration = props;
		this.config = config;
		this.exceptionHandler = new ExceptionHandler(config.addTrailingSlash(), log);

		applicationTracker = new BundleTracker<Bundle>(context, Bundle.ACTIVE, null) {
			@Override
			public Bundle addingBundle(Bundle bundle, BundleEvent event) {
				String applicationBundle = bundle.getHeaders().get("EnRoute-Application");
				if (applicationBundle == null)
					return null;

				String[] links = applicationBundle.split("\\s*,\\s*");
				for (String link : links) {
					ApplicationDTO dto = new ApplicationDTO();
					dto.bsn = bundle.getSymbolicName();
					dto.version = bundle.getHeaders().get(Constants.BUNDLE_VERSION);
					dto.bundle = bundle.getBundleId();
					dto.description = bundle.getHeaders().get(Constants.BUNDLE_DESCRIPTION);
					dto.link = link;
					dto.name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
					if (dto.name == null)
						dto.name = dto.bsn;

					synchronized (index) {
						index.applications.add(dto);
					}
				}

				return super.addingBundle(bundle, event);
			}

			@Override
			public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
				synchronized (index) {
					for (Iterator<ApplicationDTO> i = index.applications.iterator(); i.hasNext();) {
						ApplicationDTO dto = i.next();
						if (dto.bundle == bundle.getBundleId())
							i.remove();
					}
				}
				super.removedBundle(bundle, event, object);
			}
		};
		applicationTracker.open();
	}

	@Override
	public boolean doConditionalService(HttpServletRequest rq, HttpServletResponse rsp) throws Exception {
		try {
			String path = rq.getRequestURI();
			if (path != null && path.startsWith("/"))
				path = path.substring(1);

			if ("index.html".equals(path)) {
				index(rsp);
				return true;
			} else {
				return false;
			}
		}
		catch (Exception e) {
			exceptionHandler.handle(rq, rsp, e);
		}

		return true;
	}

	private void index(HttpServletResponse rsp) throws Exception {
		Bundle b = context.getBundle();
		URL url = cache.internalUrlOf(b, "osgi/enroute/web/index.html");
		CacheFile c = cache.getFromBundle(b, url, "osgi/enroute/web/index.html");
		if (c == null || c.is404 || c.isNotFound()) {
			url = cache.internalUrlOf(b, "osgi/enroute/web/local/index.html");
			c = cache.getFromBundle(b, url, "osgi/enroute/web/local/index.html");
		}

		String content = IO.collect(c.file);
		Map<String,String> map = new HashMap<>();

		synchronized (index) {
			map.put("index", new JSONCodec().enc().put(index).indent(" ").toString());
		}

		ReplacerAdapter ra = new ReplacerAdapter(map);
		content = ra.process(content);
		IO.store(content, rsp.getOutputStream());
	}

	@Deactivate
	void deactivate() {
		applicationTracker.close();
	}

	@Reference
	void setLog(LogService log) {
		this.log = log;
	}

	@Reference
	void setCache(Cache cache) {
		this.cache = cache;
	}
}