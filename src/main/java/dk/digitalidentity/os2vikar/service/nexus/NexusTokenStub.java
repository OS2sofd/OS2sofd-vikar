package dk.digitalidentity.os2vikar.service.nexus;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NexusTokenStub {
	private LocalDateTime fetched = LocalDateTime.now();
	private String accessToken = null;
	private int expiresIn = 0;

	@Autowired
	private OS2VikarConfiguration configuration;
	
	@Autowired
	private RestClient restClient;
	
	@SneakyThrows
	public String fetchToken() {
		if (accessToken != null) {
			if (LocalDateTime.now().minusSeconds(expiresIn).isBefore(fetched)) {
				return accessToken;
			}
		}

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "client_credentials");
		body.add("client_id", configuration.getKmdNexus().getClientId());
		body.add("client_secret", configuration.getKmdNexus().getClientSecret());

		TokenResponse response = restClient
			.post()
			.uri(configuration.getKmdNexus().getTokenUrl())
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(body)
			.retrieve()
			.body(TokenResponse.class);

		accessToken = response.getAccess_token();
		fetched = LocalDateTime.now();
		expiresIn = response.getExpiresIn();

		return accessToken;
	}
}
