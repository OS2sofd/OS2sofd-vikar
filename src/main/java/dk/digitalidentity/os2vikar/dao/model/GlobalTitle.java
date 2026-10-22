package dk.digitalidentity.os2vikar.dao.model;

import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "global_title")
public class GlobalTitle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String title;
	
	@OneToMany(mappedBy = "globalTitle", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<GlobalTitleADGroupMapping> adGroups;

	@OneToMany(mappedBy = "globalTitle", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<GlobalTitleUserRoleMapping> automaticUserRoles;

	@ElementCollection
	@CollectionTable(name = "global_title_constraint_it_system", joinColumns = @JoinColumn(name = "title_id"))
	@Column(name = "it_system")
	private Set<String> itSystems;
}
