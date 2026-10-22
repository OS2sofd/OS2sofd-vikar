package dk.digitalidentity.os2vikar.service.nexus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenResponse {
	
    @JsonProperty("access_token")
	private String access_token;
    
    @JsonProperty("expires_in")
	private int expiresIn;
}