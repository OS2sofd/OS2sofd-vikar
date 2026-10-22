package dk.digitalidentity.os2vikar.dao.model;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "workplace")
public class Workplace {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String masterId;

	@Column
	private LocalDate startDate;

	@Column
	private LocalDate stopDate;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "substitute_id")
	private Substitute substitute;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "orgunit_uuid")
	private OrgUnit orgUnit;

	@Column
	private String title;

	@OneToMany(mappedBy = "workplace", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<WorkplaceAssignedRole> assignedRoles;
	
	@Column(name = "require_o365_license")
	private boolean requireO365License;

	@Column
	private String createdBy;

}
