package dk.digitalidentity.os2vikar.dao.model.specification;

import dk.digitalidentity.os2vikar.dao.model.Statistic;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StatisticSpecification {
	/**
	 * Matches records where orgunit_id is in the provided list of UUIDs
	 */
	public static Specification<Statistic> hasOrgunitsIn(List<String> orgunitIds) {
		return (root, _, builder) ->
				orgunitIds == null || orgunitIds.isEmpty() ?
						builder.conjunction() : root.get("orgunitUuid").in(orgunitIds);
	}

	/**
	 * Matches records where created_by is in the provided list of UUIDs
	 */
	public static Specification<Statistic> hasCreatedByIn(List<String> createdByIds) {
		return (root, _, builder) ->
				createdByIds == null || createdByIds.isEmpty() ?
						builder.conjunction() : root.get("createdBy").in(createdByIds);
	}

	/**
	 * Matches records whose active date range overlaps the given window.
	 * startDate and stopDate are both inclusive.
	 * A null fromDate or toDate means no bound in that direction.
	 */
	public static Specification<Statistic> overlapsDateRange(LocalDate fromDate, LocalDate toDate) {
		return (root, _, builder) -> {
			ArrayList<Predicate> predicates = new ArrayList<>();
			if (fromDate != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("stopDate"), fromDate));
			}
			if (toDate != null) {
				predicates.add(builder.lessThanOrEqualTo(root.get("startDate"), toDate));
			}
			return builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
		};
	}
}
