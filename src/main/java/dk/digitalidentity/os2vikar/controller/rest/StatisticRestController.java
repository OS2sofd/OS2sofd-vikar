package dk.digitalidentity.os2vikar.controller.rest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2vikar.dao.model.Statistic;
import dk.digitalidentity.os2vikar.security.RequireStatisticsAdminAccess;
import dk.digitalidentity.os2vikar.service.StatisticService;
import dk.digitalidentity.os2vikar.service.dto.chartJS.ChartJsConfigDTO;
import dk.digitalidentity.os2vikar.service.dto.chartJS.TimeUnit;
import lombok.RequiredArgsConstructor;

@RequireStatisticsAdminAccess
@RequiredArgsConstructor
@RequestMapping("/rest/statistic")
@RestController
public class StatisticRestController {

	private final StatisticService statisticService;

	@GetMapping("chart/bar")
	public ResponseEntity<ChartJsConfigDTO> barChart(
			@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate,
			@RequestParam(required = false) List<String> includedOuUuids,
			@RequestParam(required = false) List<String> includedUserIds
	) {
		List<Statistic> data = statisticService.getFilteredChartData(includedOuUuids, includedUserIds, fromDate, toDate);

		// If the covered period is less than 31 days, time grouping should be per day instead of month
		boolean shortPeriod = fromDate != null && toDate != null
				&& ChronoUnit.DAYS.between(fromDate, toDate) < 31;

		Function<LocalDate, String> labelGetter = shortPeriod
				? LocalDate::toString
				: d -> d.with(TemporalAdjusters.lastDayOfMonth()).toString();

		TimeUnit timeUnit = shortPeriod ? TimeUnit.DAY : TimeUnit.MONTH;

		return ResponseEntity.ok(statisticService.toTimeChartConfigDTO(data, labelGetter, fromDate, toDate, timeUnit));
	}

	@GetMapping("chart/pie")
	public ResponseEntity<ChartJsConfigDTO> pieChart(
			@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate,
			@RequestParam(required = false) List<String> includedOuUuids,
			@RequestParam(required = false) List<String> includedUserIds,
			@RequestParam String fieldName
	) {
		Optional<Function<Statistic, String>> labelGetter = labelGetterFor(fieldName);
		if (labelGetter.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		List<Statistic> data = statisticService.getFilteredChartData(includedOuUuids, includedUserIds, fromDate, toDate);

		return ResponseEntity.ok(statisticService.toDistributionChartConfigDTO(data, labelGetter.get(), fromDate, toDate));
	}

	/**
	 * Returns a function determining the way to get the label for the field shown in the chart
	 */
	private static Optional<Function<Statistic, String>> labelGetterFor(String fieldName) {
		return switch (fieldName) {
			case "orgunitUuid"      -> Optional.of(Statistic::getOrgunitName);
			case "substituteUserId" -> Optional.of(Statistic::getSubstituteName);
			case "createdBy"        -> Optional.of(Statistic::getCreatedBy);
			case "title"            -> Optional.of(s -> s.getTitle() != null ? s.getTitle() : "(ingen titel)");
			default -> Optional.empty();
		};
	}
}
