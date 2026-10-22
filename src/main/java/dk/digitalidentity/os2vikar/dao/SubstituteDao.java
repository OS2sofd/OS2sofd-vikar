package dk.digitalidentity.os2vikar.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import dk.digitalidentity.os2vikar.dao.model.Substitute;

public interface SubstituteDao extends JpaRepository<Substitute, Long>{
	Substitute findByid(long id);
	Substitute findByCpr(String cpr);
	List<Substitute> findByLastUpdatedAfter(LocalDateTime tts);
	List<Substitute> findByAuthorizationCodeCheckedFalse();
	List<Substitute> findByDisabledInAdTrue();
	List<Substitute> findByNexusIdIsNull();
	List<Substitute> findByNexusIdIsNotNull();
	List<Substitute> findByWorkplacesModifiedTrue();
	
	@Modifying
	@Query(nativeQuery = true, value = "UPDATE substitute SET workplaces_modified = 0 WHERE id IN ?1")
	void clearWorkplacesModified(Set<Long> ids);
}
