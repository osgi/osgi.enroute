package osgi.enroute.oauth2.basic.provider;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.osgi.dto.DTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import aQute.lib.base64.Base64;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.oauth2.api.AuthorizationServer;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTRequest;
import osgi.enroute.scheduler.api.Scheduler;

@Designate(ocd=OAuth2BasicConfig.class, factory=true)
@Component(scope = ServiceScope.BUNDLE, name = "osgi.enroute.oauth2.basic", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class OAuth2Impl implements AuthorizationServer, Filter, REST {
	final static String				redirect	= "enrouteredirectendpoint";
	final ThreadLocal<HttpSession>	local		= new ThreadLocal<>();
	private Deferred<AccessToken>	self		= null;

	URI					authorizationEndpoint;
	URI					tokenEndpoint;
	URI					redirect_endpoint;
	String				clientId;
	String				clientSecret;
	boolean				selfAuthorize;
	String domain;
	Map<String, State>	states	= new HashMap<>();

	class State {
		String					name;
		String					state;
		Deferred<AccessToken>	deferred	= new Deferred<>();
		URI						redirect;
		long					created		= System.currentTimeMillis();
	}

	public OAuth2Impl() throws Exception {

	}

	@Reference
	DTOs dtos;

	@Reference
	Scheduler scheduler;

	public static class XAccessToken extends DTO implements Serializable {
		private static final long serialVersionUID = 1L;

		public String				access_token;
		public String				token_type;
		public long					expires_in;
		public String				refresh_token;
		public String				scope;
		public Map<String, Object>	__extra;
	}

	@Activate
	void activate(OAuth2BasicConfig config) throws URISyntaxException {
		authorizationEndpoint = new URI(config.authorization_endpoint());
		tokenEndpoint = new URI(config.token_endpoint());
		clientId = config.client_id();
		clientSecret = config.client_secret();
		selfAuthorize = config.self_authorize();
		domain = config.domain();
		
		if ( clientSecret == null)
			throw new IllegalArgumentException("Client secret not set");
		if ( clientId == null)
			throw new IllegalArgumentException("Client secret not set");
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse arg1, FilterChain chain)
			throws IOException, ServletException {
		try {
			HttpServletRequest req = (HttpServletRequest) request;
			local.set(req.getSession());
			chain.doFilter(request, arg1);
		} finally {
			local.set(null);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getClientId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getAuthorizationEndpoint(String name, URI success, URI failure, String... scope) throws Exception {
		StringBuilder sb = new StringBuilder(authorizationEndpoint.toString());
		String del = authorizationEndpoint.getRawQuery() == null ? "?" : "&";
		del = addQuery(sb, del, "response_type", "code");
		del = addQuery(sb, del, "cliend_id", clientId);
		del = addQuery(sb, del, "state", name);

		if (scope.length > 0) {
			String scopes = Strings.join(" ", scope);
			del = addQuery(sb, del, "scopes", scopes);
		}

		if (redirect != null)
			del = addQuery(sb, del, "redirect_uri", redirect.toString());

		return new URI(sb.toString());
	}

	@Override
	public Promise<osgi.enroute.oauth2.api.AuthorizationServer.AccessToken> getAccessToken(String name)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	private String encode(String in) {
		byte[] encoded = in.getBytes(StandardCharsets.UTF_8);
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < encoded.length; i++) {
			char c = (char) encoded[i];
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '.'
					|| c == '_' || c == '~')
				sb.append(c);
			else {
				sb.append("%");
				sb.append(Hex.nibble(c / 16));
				sb.append(Hex.nibble(c % 16));
			}
		}
		return in;
	}

	private String addQuery(StringBuilder sb, String del, String key, String value) {
		sb.append(del);
		sb.append(key);
		sb.append("=");
		sb.append(encode(value));
		return "&";
	}

	interface RedirectEndpointOptions extends RESTRequest {
		String code();

		String state();
	}

	public void getEnrouteredirectendpoint(RedirectEndpointOptions rq) throws Exception {
		System.out.println("Code is " + rq.code());
		System.out.println("State is " + rq.state());

		HttpSession session = local.get();
		State state = (State) session.getAttribute(getStateName(rq.state()));
		if (state == null) {
			rq._response().sendError(404);
			return;
		}

		String u = "code=" + encode(rq.code());

		HttpsURLConnection con = authorize(tokenEndpoint, "Basic", clientId, clientSecret);

		con.setRequestMethod("POST");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setDoOutput(true);
		con.getOutputStream().write(u.getBytes(StandardCharsets.UTF_8));
		if (con.getResponseCode() / 100 == 2) {
			String s = IO.collect(con.getInputStream());
			System.out.println(con.getContent());
			AccessToken accessToken = dtos.decoder(AccessToken.class).get(s);
			System.out.println("AT is " + accessToken);

			state.deferred.resolve(accessToken);
			if (state.redirect != null) {
				rq._response().addHeader("Location", state.redirect.toString());
				return;
			}
		} else {
			state.deferred.fail(new SecurityException("Invalid request OAuth2 token request " + con.getResponseCode()
					+ " : " + rq._request().getRequestURI()));
		}
	}

	private HttpsURLConnection authorize(URI uri, String type, String userId, String password) throws IOException {
		HttpsURLConnection con = (HttpsURLConnection) uri.toURL().openConnection();
		String authorization = encode(clientId) + ":" + encode(clientSecret);
		con.setRequestProperty("Authorization",
				"Basic " + Base64.encodeBase64(authorization.getBytes(StandardCharsets.UTF_8)));
		return con;
	}

	private String getStateName(String state) {
		return "_oauth2_" + state;
	}

	@Override
	public Promise<AccessToken> getAccessToken(String... scope) throws Exception {
		if (!selfAuthorize)
			throw new IllegalArgumentException("Does not support self authorization via 'Client Credentials Grant' for domain "+ domain);
		
		synchronized (this) {
			if (self != null)
				return self.getPromise();

			self = new Deferred<>();
		}

		StringBuilder sb = new StringBuilder();
		String del = "";
		addQuery(sb, del, "grant_type", "client_credentials");
		if (scope.length > 0) {
			addQuery(sb, del, "scope", Strings.join(" ", scope));
		}

		HttpURLConnection urlc = authorize(authorizationEndpoint, "Basic", clientId, clientSecret);

		urlc.setRequestMethod("POST");
		urlc.setDoOutput(true);
		urlc.setDoInput(true);
		urlc.getOutputStream().write(sb.toString().getBytes(StandardCharsets.UTF_8));
		
		return doInBackground(urlc,scope);
	}

	private Promise<AccessToken> doInBackground(HttpURLConnection urlc, String... scope) throws IOException, ProtocolException {

		try {
			if (urlc.getResponseCode() / 100 != 2) {
				self.fail(new IOException("response=" + urlc.getResponseCode()));
			} else {
				String response = IO.collect(urlc.getInputStream());
				if (!response.startsWith("{\"token")) {
					self.fail(new SecurityException(response));
				} else {
					XAccessToken token = dtos.decoder(XAccessToken.class).get(response);
					self.resolve(new AccessToken() {

						@Override
						public HttpURLConnection authorize(URI resource) throws Exception {
							return authorize(resource, null);
						}

						@Override
						public HttpURLConnection authorize(URI resource, Proxy proxy) throws Exception {
							URL url = resource.toURL();
							
							HttpURLConnection con = (HttpURLConnection) (proxy == null ? url.openConnection() : url.openConnection(proxy));
							switch( token.token_type.toLowerCase()) {
							case "bearer":
								con.setRequestProperty("Authorization", "Bearer " + token.access_token);
								return con;

								default:
									throw new UnsupportedOperationException("The token type is not supported");
							}
						}

						@Override
						public Set<String> getScopes() {
							if ( token.scope != null) {
								return Stream.of(token.scope.split(" +")).collect( Collectors.toSet());
							} else
								return Collections.emptySet();
						}});
				}
			}
		} catch (Throwable e) {
			self.fail(e);
		}
		return self.getPromise();
	}

}
