package dk.digitalidentity.os2vikar.service.nexus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
	private long id;
	private int version;
	private String username;
}
