package dk.digitalidentity.os2vikar.config.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cpr {
	private String url = "http://cpr.internal-dns.dk";
	private boolean dev = false;
}
