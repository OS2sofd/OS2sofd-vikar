package dk.digitalidentity.os2vikar.config.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KMDNexus {
   	private boolean enabled = false;

	private String tokenUrl;
	private String clientId;
	private String clientSecret;
	private String nexusBaseUrl;
}
