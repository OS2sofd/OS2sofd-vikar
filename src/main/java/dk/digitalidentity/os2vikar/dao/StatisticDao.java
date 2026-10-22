package dk.digitalidentity.os2vikar.dao;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.Statistic;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StatisticDao extends JpaRepository<Statistic, Long> , JpaSpecificationExecutor<Statistic> {
	Statistic findByWorkplaceId(long id);

	long deleteByStartDateBefore(LocalDate cutoff);


}
