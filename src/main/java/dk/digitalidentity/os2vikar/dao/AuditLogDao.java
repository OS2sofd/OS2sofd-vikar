package dk.digitalidentity.os2vikar.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dk.digitalidentity.os2vikar.dao.model.AuditLog;
import dk.digitalidentity.os2vikar.dao.model.AuditLogProjection;

public interface AuditLogDao extends JpaRepository<AuditLog, Long> {
	AuditLog findById(long id);

	long deleteByTimestampBefore(LocalDateTime timestamp);

	@Query(nativeQuery = true, value = """
		SELECT * FROM auditlog
		WHERE (:searchTime = '' OR tts LIKE CONCAT('%', :searchTime, '%'))
		AND (:searchAdmin = '' OR administrator LIKE CONCAT('%', :searchAdmin, '%'))
		AND (:searchSubstitute = '' OR substitute LIKE CONCAT('%', :searchSubstitute, '%'))
		AND (:searchEvent = '' OR operation LIKE CONCAT('%', :searchEvent, '%'))
	""")
	List<AuditLogProjection> search(String searchTime, String searchAdmin, String searchSubstitute, String searchEvent);
}
