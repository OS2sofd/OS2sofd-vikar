package dk.digitalidentity.os2vikar.dao.model;

import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "orgunit")
public class OrgUnit {

	@Id
	@Column
	private String uuid;

	@Column
	private String name;

	@Column
	private boolean canHaveSubstitutes;

	@Column
	private int maxSubstituteWorkingDays;

	@Column
	private int defaultSubstituteWorkingDays;
	
	@Column
	private int maxDaysInFuture;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_uuid")
	private OrgUnit parent;
	
	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@Valid
	private List<OrgUnit> children;

	@OneToMany(mappedBy = "orgUnit", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<LocalTitle> localTitles;

	@OneToMany(mappedBy = "orgUnit", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<LocalRole> localRoles;
	
	@OneToMany(mappedBy = "orgUnit", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<OrgUnitADGroupMapping> adGroups;

	@OneToMany(mappedBy = "orgUnit", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<OrgUnitUserRoleMapping> automaticUserRoles;

	@ElementCollection
	@CollectionTable(name = "orgunit_constraint_it_system", joinColumns = @JoinColumn(name = "orgunit_uuid"))
	@Column(name = "it_system")
	private Set<String> itSystems;
}
