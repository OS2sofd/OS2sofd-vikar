package dk.digitalidentity.os2vikar.service.xls;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.View;

import dk.digitalidentity.os2vikar.dao.model.AuditLogProjection;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuditlogXlsView implements View {
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final String CONTENT_TYPE = "application/ms-excel";
	private int rowCount = 1;

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}
	
	@Override
	public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		@SuppressWarnings("unchecked")
		List<AuditLogProjection> auditlogs = (List<AuditLogProjection>) model.get("auditlogs");

		response.setContentType(getContentType());
		response.setHeader("Content-Disposition", "attachment; filename=\"auditlog.xlsx\"");

		try (Workbook workbook = new DisposableSXSSFWorkbook()) {

			// create excel xls sheet
			Sheet sheet = workbook.createSheet("Auditlogs");
	
			// create header row
			createHeader(workbook, sheet);
	
			for (AuditLogProjection auditlog : auditlogs) {
				Row row = sheet.createRow(rowCount++);

				row.createCell(0).setCellValue(auditlog.getTts().format(formatter));
				row.createCell(1).setCellValue(auditlog.getAdministrator());
				row.createCell(2).setCellValue(auditlog.getSubstitute());
				row.createCell(3).setCellValue(auditlog.getOperation());
				row.createCell(4).setCellValue(auditlog.getStartDate());
				row.createCell(5).setCellValue(auditlog.getStopDate());
				row.createCell(6).setCellValue(auditlog.getDetails());
			}
			
			workbook.write(response.getOutputStream());
		}
	}

	private void createHeader(Workbook workbook, Sheet sheet) {
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(0);
		createCell(header, 0, "Tidspunkt", headerStyle);
		createCell(header, 1, "Administrator", headerStyle);
		createCell(header, 2, "Vikar", headerStyle);
		createCell(header, 3, "Handling", headerStyle);
		createCell(header, 4, "Start dato", headerStyle);
		createCell(header, 5, "Stop dato", headerStyle);
		createCell(header, 6, "Detaljer", headerStyle);
	}

	private static void createCell(Row header, int column, String value, CellStyle style) {
		Cell cell = header.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
