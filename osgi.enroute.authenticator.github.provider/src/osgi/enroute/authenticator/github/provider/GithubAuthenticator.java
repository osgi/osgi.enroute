package osgi.enroute.authenticator.github.provider;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;

import osgi.enroute.authentication.api.Authenticator;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.dto.api.TypeReference;
import osgi.enroute.oauth2.api.AuthorizationServer;
import osgi.enroute.oauth2.api.AuthorizationServer.AccessToken;
import osgi.enroute.rest.api.REST;

@Component
public class GithubAuthenticator implements REST, Authenticator {
	private static TypeReference<List<GithubEmail>> EMAIL_RESULT = new TypeReference<List<GithubEmail>>() {
	};

	public static class GithubEmail {
		public String email;
	}

	private static final String	GITHUB_LOGIN	= "github_login";
	@Reference
	AuthorizationServer			oauth2;
	@Reference
	DTOs						dtos;

	public URI getAuthorizationuri() throws Exception {
		return oauth2.getAuthorizationEndpoint(GITHUB_LOGIN, new URI("/welcome.html"), new URI("/fail.html"), "user:mail");
	}

	@Override
	public String authenticate(Map<String, Object> arguments, String... sources) throws Exception {
		Promise<AccessToken> accessToken = oauth2.getAccessToken(GITHUB_LOGIN);
		if ( accessToken.isDone() && accessToken.getFailure() == null) {
			AccessToken token = accessToken.getValue();
			HttpURLConnection con = token.authorize( new URI("https://api.github.com/user/emails"));
			List<GithubEmail> list = dtos.decoder(EMAIL_RESULT).get(con.getInputStream());
			if ( list.isEmpty())
				return null;
			
			return list.get(0).email;
		}
		return null;
	}

	@Override
	public boolean forget(String userid) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}
	
	

}
