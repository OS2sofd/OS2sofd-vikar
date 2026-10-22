package dk.digitalidentity.os2vikar.config.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Organisation {
	private OrganisationSource source = OrganisationSource.FK_ORGANISATION;

	private String url = "http://os2sync.internal-dns.dk/api/hierarchy";
}