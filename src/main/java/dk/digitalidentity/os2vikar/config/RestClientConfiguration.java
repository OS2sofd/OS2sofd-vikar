package dk.digitalidentity.os2vikar.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {
    
	@Bean("myRestClient")
	public RestClient restClient() throws Exception {
	    CloseableHttpClient httpClient = HttpClients.custom()
	            .build();

	    return RestClient.builder()
	            .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
	            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
	            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
	            .build();


	}
}
