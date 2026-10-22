package dk.digitalidentity.os2vikar.config.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleCatalogConfiguration {
	private String url;
	private String apiKey;
	private boolean enabled = true;
	
	// new feature, enable to control roles with API calls to OS2rollekatalog
	private boolean enabledForWorkplaces = false;
	
	// old feature, disabled everywhere except Roskilde
	private boolean allowAutomaticUserRoles = false;
}
