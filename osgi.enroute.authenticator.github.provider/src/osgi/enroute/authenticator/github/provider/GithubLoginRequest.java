package osgi.enroute.authenticator.github.provider;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpSession;

import org.osgi.dto.DTO;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.lib.base64.Base64;
import aQute.lib.io.IO;
import osgi.enroute.authorization.api.Authority;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTRequest;

@Component
public class GithubLoginRequest implements REST {

	static String	clientId		= "";
	static String	clientSecret	= "";

	@Reference
	DTOs dtos;

	@Reference
	Authority authority;

	interface LoginParameters extends RESTRequest {
		String code();

		String state();
	};

	public static class AccessToken extends DTO implements Serializable {
		private static final long serialVersionUID = 1L;
		
		public String				access_token;
		public String				token_type;
		public long					expires_in;
		public String				refresh_token;
		public String				scope;
		public Map<String, Object>	__extra;
	}

	public AccessToken getLogin(LoginParameters parms) throws IOException, Exception {
		HttpSession session = parms._request().getSession();

		String u = "code="+encode(parms.code());
		
		URL uri = new URL("https://github.com/login/oauth/access_token");
		HttpsURLConnection con = (HttpsURLConnection) uri.openConnection();
		String authorization = encode(clientId) +":"+ encode(clientSecret);
		
		con.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64(authorization.getBytes()));
		
		con.setRequestMethod("POST");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		con.setDoOutput(true);
		con.getOutputStream().write(u.getBytes(StandardCharsets.UTF_8));
		
		if (con.getResponseCode() == 200) {
			String s = IO.collect(con.getInputStream());
			System.out.println(con.getContent());
			System.out.println(con.getContentType());
			AccessToken accessToken = dtos.decoder(AccessToken.class).get(s);

			session.setAttribute("_oauth2_"+parms.state(), accessToken);
			
			System.out.println("Code is " + parms.code());
			System.out.println("State is " + parms.state());
			System.out.println("AT is " + accessToken);
			
			URL url = new URL("https://api.github.com/user/emails");
			con = (HttpsURLConnection) url.openConnection();
			con.setRequestProperty("Authorization", "Bearer " + accessToken.access_token);
			String email = IO.collect(con.getInputStream());
			System.out.println(email);
			return accessToken;
		} else {
			System.out.println("Wrong");
			return null;
		}
	}

	public String getUserid() throws Exception {
		return authority.getUserId();
	}
	
	private String encode(String in) {
		return in;
	}
}
