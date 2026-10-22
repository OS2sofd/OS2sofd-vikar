package dk.digitalidentity.os2vikar.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dk.digitalidentity.os2vikar.service.dto.chartJS.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2vikar.dao.StatisticDao;
import dk.digitalidentity.os2vikar.dao.model.Statistic;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.dao.model.Workplace;
import dk.digitalidentity.os2vikar.dao.model.specification.StatisticSpecification;
import dk.digitalidentity.os2vikar.service.dto.chartJS.ChartJsConfigDTO;
import dk.digitalidentity.os2vikar.service.dto.chartJS.ChartJsDataDTO;
import dk.digitalidentity.os2vikar.service.dto.chartJS.ChartJsDataPointDTO;
import dk.digitalidentity.os2vikar.service.dto.chartJS.ChartJsDatasetDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StatisticService {

	@Autowired
	private StatisticDao statisticDao;

	@Autowired
	private SubstituteService substituteService;

	@Autowired
	private StatisticService self;

	// migrate existing workplaces into statistics
	// can be deleted once everyone has been updated to at least july 2025 version
	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		self.migrate();
	}

	@Transactional
	public void migrate() {
		List<Statistic> existing = statisticDao.findAll();
		if (existing.size() == 0) {
			List<Substitute> substitutes = substituteService.getAll();

			for (Substitute substitute : substitutes) {
				for (Workplace workplace : substitute.getWorkplaces()) {
					save(workplace);
				}
			}
		}
	}

	public void save(Workplace workplace) {
		if (workplace.getId() == 0) {
			log.error("Called with workplace without ID!");
			return;
		}

		Statistic statistic = statisticDao.findByWorkplaceId(workplace.getId());
		if (statistic == null) {
			statistic = new Statistic();
			statistic.setOrgunitName(workplace.getOrgUnit().getName());
			statistic.setOrgunitUuid(workplace.getOrgUnit().getUuid());
			statistic.setStartDate(workplace.getStartDate());
			statistic.setSubstituteName(workplace.getSubstitute().getName());
			statistic.setSubstituteUserId(workplace.getSubstitute().getUsername());
			statistic.setWorkplaceId(workplace.getId());
			statistic.setCreatedBy(workplace.getCreatedBy());
			statistic.setTitle(workplace.getTitle());
		}

		statistic.setStopDate(workplace.getStopDate());

		// cancelling is done by setting the stop-date 1 day in the past, which MIGHT mean that it was never created in the first place
		if (workplace.getStopDate().isBefore(workplace.getStartDate())) {
			statisticDao.delete(statistic);
		}
		else {
			statisticDao.save(statistic);
		}
	}

	public List<Statistic> getAll() {
		return statisticDao.findAll();
	}

	@Transactional
	public void cleanUp() {
		log.info("Started statistic clean up task");

		LocalDate cutoff = LocalDate.now().minusMonths(13);
		long deletedCount = statisticDao.deleteByStartDateBefore(cutoff);

		log.info("Finished statistic clean up task. " + deletedCount + " statistics was deleted.");
	}

	/**
	 * Gets all statistic entities from db whose active range overlaps the filter window.
	 * A null value for a filter will match all rows.
	 * @param allowedOuUUids list of orgunit uuids to match
	 * @param allowedUserIds list of user ids to match with the createdBy field
	 * @param fromDate lower bound of the filter window (inclusive); statistics with stopDate > fromDate are included
	 * @param toDate upper bound of the filter window (inclusive); statistics with startDate <= toDate are included
	 * @return a list of filtered Statistic
	 */
	@Transactional
	public List<Statistic> getFilteredChartData(List<String> allowedOuUUids, List<String> allowedUserIds, LocalDate fromDate, LocalDate toDate) {
		return statisticDao.findAll(Specification.allOf(
					StatisticSpecification.overlapsDateRange(fromDate, toDate),
					StatisticSpecification.hasOrgunitsIn(allowedOuUUids),
					StatisticSpecification.hasCreatedByIn(allowedUserIds)
				)
		);
	}

	/**
	 * Returns a stream of dates within the statistic's active range, clipped to the given filter window.
	 * startDate is inclusive, stopDate is inclusive.
	 */
	private Stream<LocalDate> activeDays(Statistic s, LocalDate from, LocalDate to) {
		LocalDate start = from != null && s.getStartDate().isBefore(from) ? from : s.getStartDate();
		
		// stopDate is inclusive; toDate is inclusive, so the effective exclusive end is min(stopDate+1, toDate+1)
		LocalDate exclusiveEnd = s.getStopDate().plusDays(1);
		LocalDate end = to != null && exclusiveEnd.isAfter(to.plusDays(1)) ? to.plusDays(1) : exclusiveEnd;

		if (!start.isBefore(end)) {
			return Stream.empty();
		}
		
		return start.datesUntil(end);
	}

	/**
	 * Builds a ChartJS DTO for time-series (bar) charts by expanding each Statistic into individual active days
	 * and grouping them with the provided day-level label function.
	 * @param data list of Statistic entities
	 * @param dayLabelGetter function that maps a day to its chart label (e.g. day string or last-of-month string)
	 * @param fromDate lower bound used to clip partial-overlap statistics
	 * @param toDate upper bound used to clip partial-overlap statistics
	 * @return ChartJS compatible DTO
	 */
	public ChartJsConfigDTO toTimeChartConfigDTO(List<Statistic> data, Function<LocalDate, String> dayLabelGetter, LocalDate fromDate, LocalDate toDate, TimeUnit timeUnit) {
		Map<String, Long> aggregated = data.stream()
				.flatMap(s -> activeDays(s, fromDate, toDate))
				.collect(Collectors.groupingBy(dayLabelGetter, Collectors.counting()));

		return buildChartConfigDTO(aggregated, false, timeUnit);
	}

	/**
	 * Builds a ChartJS DTO for distribution (pie) charts by counting active days per statistic-level group.
	 * @param data list of Statistic entities
	 * @param labelGetter function that maps a Statistic to its group label
	 * @param fromDate lower bound used to clip partial-overlap statistics
	 * @param toDate upper bound used to clip partial-overlap statistics
	 * @return ChartJS compatible DTO (includes label array required by pie charts)
	 */
	public ChartJsConfigDTO toDistributionChartConfigDTO(List<Statistic> data, Function<Statistic, String> labelGetter, LocalDate fromDate, LocalDate toDate) {
		Map<String, Long> aggregated = data.stream()
				.collect(Collectors.groupingBy(labelGetter,
						Collectors.summingLong(s -> activeDays(s, fromDate, toDate).count())));

		return buildChartConfigDTO(aggregated, true, null);
	}

	private ChartJsConfigDTO buildChartConfigDTO(Map<String, Long> aggregated, boolean includeLabelArray, TimeUnit timeUnit) {
		List<ChartJsDataPointDTO> mappedData = aggregated.entrySet().stream()
				.map(e -> ChartJsDataPointDTO.builder()
						.x(e.getKey() + (includeLabelArray ? " (" + e.getValue() + ")" : ""))
						.y(e.getValue())
						.build())
				.toList();

		List<ChartJsDatasetDTO> datasets = List.of(ChartJsDatasetDTO.builder()
				.data(mappedData)
				.build());

		ChartJsDataDTO chartJsDataDTO = ChartJsDataDTO.builder()
				.labels(includeLabelArray ? mappedData.stream().map(ChartJsDataPointDTO::getX).toList() : null)
				.datasets(datasets)
				.build();

		long sum = aggregated.values().stream().mapToLong(l -> l).sum();

		return ChartJsConfigDTO.builder()
				.data(chartJsDataDTO)
				.timeUnit(timeUnit)
				.sum(sum)
				.build();
	}

}
