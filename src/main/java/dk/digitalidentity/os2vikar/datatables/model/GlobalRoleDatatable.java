package dk.digitalidentity.os2vikar.datatables.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "view_global_roles")
public class GlobalRoleDatatable {
	
    @Id
    @Column
    private long id;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String itSystem;

    @Column
    private boolean checked;
}