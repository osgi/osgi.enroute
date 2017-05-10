package osgi.enroute.twitter.provider;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.service.command.Parameter;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.lib.io.IO;
import osgi.enroute.debug.api.Debug;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.oauth2.api.AuthorizationServer;
import osgi.enroute.oauth2.api.AuthorizationServer.AccessToken;
import osgi.enroute.twitter.provider.dto.SearchResult;

@Component(service = Twitter.class, property = { Debug.COMMAND_SCOPE + "=twitter", Debug.COMMAND_FUNCTION + "=search" })
public class Twitter {

	@Reference(target = "(domain=twitter)")
	AuthorizationServer								oath2;

	@Reference
	DTOs											dtos;

	public List<String> search(//
			@Parameter(names = { "-c", "--count" }, absentValue = "10") int count, //
			@Parameter(names = { "-l", "--language" }, absentValue = "en") String language, //
			String arg)
			throws Exception {
		AccessToken at = oath2.getAccessToken().getValue();
		URI uri = new URI("https://api.twitter.com/1.1/search/tweets.json?q=" + URLEncoder.encode(arg, "UTF-8")
				+ "&count=" + count + "&lang="+ language);
		HttpURLConnection authorized = at.authorize(uri);
		String result = IO.collect(authorized.getInputStream());
		SearchResult statuses = dtos.decoder(SearchResult.class) //
				.get(result);

		return statuses.statuses.stream().map( s -> s.text).collect( Collectors.toList());
	}
}
