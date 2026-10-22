package dk.digitalidentity.os2vikar.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2vikar.dao.AuditLogDao;
import dk.digitalidentity.os2vikar.dao.model.AuditLog;
import dk.digitalidentity.os2vikar.dao.model.AuditLogProjection;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.security.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuditLogService {
	
	@Autowired
	private AuditLogDao auditLogDao;

	public List<AuditLogProjection> search(String searchTime, String searchAdmin, String searchSubstitute, String searchEvent) {
		return auditLogDao.search(searchTime, searchAdmin, searchSubstitute, searchEvent);
	}

	public AuditLog getById(long id) {
		return auditLogDao.findById(id);
	}

	public void logSystem(String operation, String details, Substitute substitute) {
		AuditLog auditLog = new AuditLog();
		auditLog.setIp(SecurityUtil.getUserIP());
		auditLog.setTimestamp(LocalDateTime.now());
		auditLog.setOperation(operation);
		auditLog.setDetails(details);

		if (substitute != null) {
			auditLog.setSubstitute(substitute.getName() + " " + substitute.getSurname() + " (" + substitute.getUsername() + ")");
		}

		auditLogDao.save(auditLog);
	}
	
	public void log(String operation, String details, Substitute substitute) {
		AuditLog auditLog = new AuditLog();
		auditLog.setIp(SecurityUtil.getUserIP());
		auditLog.setAdministrator(SecurityUtil.getUser());
		auditLog.setTimestamp(LocalDateTime.now());
		auditLog.setOperation(operation);
		auditLog.setDetails(details);

		if (substitute != null) {
			auditLog.setSubstitute(substitute.getName() + " " + substitute.getSurname() + " (" + substitute.getUsername() + ")");
		}

		auditLogDao.save(auditLog);
	}
	
	public void createOrEditWorkplace(String operation, String details, Substitute substitute, String startDate, String stopDate) {
		AuditLog auditLog = new AuditLog();
		auditLog.setIp(SecurityUtil.getUserIP());
		auditLog.setAdministrator(SecurityUtil.getUser());
		auditLog.setTimestamp(LocalDateTime.now());
		auditLog.setOperation(operation);
		auditLog.setDetails(details);
		auditLog.setStartDate(startDate);
		auditLog.setStopDate(stopDate);

		if (substitute != null) {
			auditLog.setSubstitute(substitute.getName() + " " + substitute.getSurname() + " (" + substitute.getUsername() + ")");
		}

		auditLogDao.save(auditLog);
	}

	public void syncOUsToNexus(Substitute substitute, String ousString) {
		AuditLog auditLog = new AuditLog();
		auditLog.setIp(SecurityUtil.getUserIP());
		auditLog.setTimestamp(LocalDateTime.now());
		auditLog.setOperation("Opdateret organisationer i KMD Nexus");
		auditLog.setDetails(ousString);
		auditLog.setSubstitute(substitute.getName() + " " + substitute.getSurname() + " (" + substitute.getUsername() + ")");

		auditLogDao.save(auditLog);
	}

	// TODO: better deleting of auditlogs - bypass Hibernate and use native query
	@Transactional
	public void cleanUp() {
		log.info("Started audit log clean up task");
		
		LocalDateTime now = LocalDateTime.now();
		// TODO: make configurable
		LocalDateTime sixMonthsAgo = now.minusMonths(13);
		long deletedCount = auditLogDao.deleteByTimestampBefore(sixMonthsAgo);

		log.info("Finished audit log clean up task. " + deletedCount + " audit logs was deleted.");
	}
}
