package dk.digitalidentity.os2vikar.dao.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "statistic")
public class Statistic {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private long workplaceId;

	@Column
	private LocalDate startDate;

	@Column
	private LocalDate stopDate;

	@Column
	private String orgunitUuid;

	@Column
	private String orgunitName;

	@Column
	private String substituteName;

	@Column
	private String substituteUserId;
	
	@Column
	private String createdBy;

	@Column
	private String title;

}
