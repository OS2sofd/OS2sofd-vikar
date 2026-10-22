package dk.digitalidentity.os2vikar.dao.model;

import java.time.LocalDateTime;

import dk.digitalidentity.os2vikar.dao.model.enums.ADAccountPoolStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "ad_account_pool")
@Getter
@Setter
public class ADAccountPool {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String username;

	@Column(name = "with_o365_license")
	private boolean withO365License;

	@Column
	private LocalDateTime created;

	@Column
	@Enumerated(EnumType.STRING)
	private ADAccountPoolStatus status;

	@Column
	private String statusMessage;
}
