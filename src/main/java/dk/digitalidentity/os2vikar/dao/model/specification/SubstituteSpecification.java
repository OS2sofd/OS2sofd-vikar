package dk.digitalidentity.os2vikar.dao.model.specification;

import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.dao.model.Workplace;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

public class SubstituteSpecification {
	private SubstituteSpecification(){}

	/**
	 * Matches substitutes with at least one workplace whose orgUnit uuid is in orgUnitUuids.
	 * An empty/null orgUnitUuids ("unconstrained") matches everyone.
	 */
	public static Specification<Substitute> hasWorkplaceInOrgUnits(Set<String> orgUnitUuids) {
		return (root, query, builder) -> {
			if (orgUnitUuids == null || orgUnitUuids.isEmpty()) {
				return builder.conjunction();
			}

			query.distinct(true);

			Join<Substitute, Workplace> workplaceJoin = root.join("workplaces", JoinType.LEFT);

			return workplaceJoin.get("orgUnit").get("uuid").in(orgUnitUuids);
		};
	}

	/**
	 * Matches substitutes with no workplaces at all, or whose workplaces are all outside orgUnitUuids.
	 * Only meaningful when orgUnitUuids is non-empty; if empty ("unconstrained"), returns a predicate
	 * matching nothing, since "the rest" is empty for an unconstrained admin.
	 */
	public static Specification<Substitute> hasNoWorkplaceInOrgUnits(Set<String> orgUnitUuids) {
		return (root, query, builder) -> {
			if (orgUnitUuids == null || orgUnitUuids.isEmpty()) {
				return builder.disjunction();
			}

			Subquery<Long> subquery = query.subquery(Long.class);
			Join<Substitute, Workplace> workplace = subquery.from(Substitute.class).join("workplaces", JoinType.INNER);
			Predicate correlatedToOuterSubstitute = builder.equal(subquery.getRoots().iterator().next(), root);
			Predicate orgUnitMatches = workplace.get("orgUnit").get("uuid").in(orgUnitUuids);

			subquery.select(builder.literal(1L)).where(builder.and(correlatedToOuterSubstitute, orgUnitMatches));

			return builder.not(builder.exists(subquery));
		};
	}
}
