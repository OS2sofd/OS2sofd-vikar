package dk.digitalidentity.os2vikar.dao.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "auditlog")
@Getter
@Setter
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "tts")
	private LocalDateTime timestamp;

	@Column
	private String ip;

	@Column
	private String administrator;

	@Column
	private String substitute;

	@Column
	private String operation;

	@Column
	private String details;
	
	// these two columns are only used for workplace registrations on substitutes (and only available in the excel download of logs)

	@Column
	private String startDate;
	
	@Column
	private String stopDate;
}
