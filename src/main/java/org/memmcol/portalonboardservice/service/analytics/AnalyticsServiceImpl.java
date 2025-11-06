package org.memmcol.portalonboardservice.service.analytics;

import org.memmcol.portalonboardservice.components.GenericHandler;
import org.memmcol.portalonboardservice.config.ResponseProperties;
import org.memmcol.portalonboardservice.mapper.AnalyticsMapper;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.memmcol.portalonboardservice.model.audit.IncidentReport;
import org.memmcol.portalonboardservice.model.audit.UptimeReport;
import org.memmcol.portalonboardservice.model.user.Organization;
import org.memmcol.portalonboardservice.repository.ExceptionAuditRepository;
import org.memmcol.portalonboardservice.repository.UptimeReportRepository;
import org.memmcol.portalonboardservice.service.node.NodeServiceImpl;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class AnalyticsServiceImpl implements AnalyticsService{

    private static final Logger log = LoggerFactory.getLogger(NodeServiceImpl.class);

    private static final List<String> SERVICES = List.of("API-GATEWAY-SERVICE", "GRIDFLEX-BACKEND-SERVICE");

    @Autowired
    private UptimeReportRepository reportRepository;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AnalyticsMapper analyticsMapper;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

       @Override
    public Map<String, Object> getAnalytics(int year, int month) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();

        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        try {
            // Fetch utility companies
            List<Organization> organizations = analyticsMapper.getAllOrganizations();
            long activeCount = organizations.stream().filter(Organization::getStatus).count();

            // Fetch daily reports
            List<UptimeReport> dailyReports = reportRepository
                    .findByReportTypeAndCreatedAtBetweenAndServiceNameIn(
                            "DAILY", startDate.toString(), endDate.toString(), SERVICES
                    );

            // Fetch monthly reports
            List<UptimeReport> monthlyReports = reportRepository
                    .findByReportTypeAndMonthStartingWithAndServiceNameIn(
                            "MONTHLY", String.valueOf(year), SERVICES
                    );

            // Daily Summaries (grouped by createdAt)
            Map<String, List<UptimeReport>> reportsByDate = dailyReports.stream()
                    .collect(Collectors.groupingBy(UptimeReport::getCreatedAt));

            List<Map<String, Object>> dailySummaries = new ArrayList<>();
            for (Map.Entry<String, List<UptimeReport>> entry : reportsByDate.entrySet()) {
                String date = entry.getKey();
                List<UptimeReport> reportsForDay = entry.getValue();

                long up = reportsForDay.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
                long down = reportsForDay.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
                long total = up + down;

                Map<String, Object> summary = new HashMap<>();
                summary.put("date", date);
                summary.put("services", reportsForDay.stream().map(UptimeReport::getServiceName).toList());
                summary.put("uptimeMinutes", up);
                summary.put("downtimeMinutes", down);
                summary.put("uptimePercent", total == 0 ? 0 : (up * 100.0 / total));
                summary.put("downtimePercent", total == 0 ? 0 : (down * 100.0 / total));

                dailySummaries.add(summary);
            }

            // Overall Daily Summary
            long totalUp = dailyReports.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
            long totalDown = dailyReports.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
            long total = totalUp + totalDown;

            Map<String, Object> dailySummary = new HashMap<>();
            dailySummary.put("services", SERVICES);
            dailySummary.put("uptimePercent", total == 0 ? 0 : (totalUp * 100.0 / total));
            dailySummary.put("downtimePercent", total == 0 ? 0 : (totalDown * 100.0 / total));
            dailySummary.put("uptimeMinutes", totalUp);
            dailySummary.put("downtimeMinutes", totalDown);

            // Monthly Summaries (group by month string)
            Map<String, List<UptimeReport>> reportsByMonth = monthlyReports.stream()
                    .collect(Collectors.groupingBy(UptimeReport::getMonth));

            List<Map<String, Object>> monthlySummaries = new ArrayList<>();
            for (Map.Entry<String, List<UptimeReport>> entry : reportsByMonth.entrySet()) {
                String monthStr = entry.getKey();
                List<UptimeReport> reportsForMonth = entry.getValue();

                long up = reportsForMonth.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
                long down = reportsForMonth.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
                long totalM = up + down;

                // Parse monthStr -> YearMonth
                YearMonth reportYm = YearMonth.parse(monthStr);

                Map<String, Object> summary = new HashMap<>();
                summary.put("month", monthStr);
                summary.put("monthDisplay", reportYm.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));// per-month summaries// per-month summaries
                summary.put("services", reportsForMonth.stream().map(UptimeReport::getServiceName).distinct().toList());
                summary.put("uptimeMinutes", up);
                summary.put("downtimeMinutes", down);
                summary.put("uptimePercent", totalM == 0 ? 0 : (up * 100.0 / totalM));
                summary.put("downtimePercent", totalM == 0 ? 0 : (down * 100.0 / totalM));

                monthlySummaries.add(summary);
            }

            // Total Monthly Summary (aggregate across all months)
            long monthlyTotalUp = monthlyReports.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
            long monthlyTotalDown = monthlyReports.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
            long monthlyTotal = monthlyTotalUp + monthlyTotalDown;

            Map<String, Object> totalMonthlySummary = new HashMap<>();
            totalMonthlySummary.put("services", monthlyReports.stream()
                    .map(UptimeReport::getServiceName).distinct().toList());
            totalMonthlySummary.put("uptimeMinutes", monthlyTotalUp);
            totalMonthlySummary.put("downtimeMinutes", monthlyTotalDown);
            totalMonthlySummary.put("uptimePercent", monthlyTotal == 0 ? 0 : (monthlyTotalUp * 100.0 / monthlyTotal));
            totalMonthlySummary.put("downtimePercent", monthlyTotal == 0 ? 0 : (monthlyTotalDown * 100.0 / monthlyTotal));

            // Build Final Response
            Map<String, Object> response = new HashMap<>();
            response.put("dailyReports", dailyReports);            // raw daily reports
            response.put("dailySummaries", dailySummaries);        // per-day summaries
            response.put("totalDailySummary", dailySummary);       // overall daily
            response.put("monthlyReports", monthlyReports);        // raw monthly reports
            response.put("monthlySummaries", monthlySummaries);
            response.put("totalMonthlySummary", totalMonthlySummary); // overall monthly
            response.put("totalUtilityCompany", organizations.size());
            response.put("activeUtilityCompany", activeCount);
            response.put("incidentReport", 0); // TODO
            response.put("averageRecoveryTime", 0); // TODO

            return ResponseMap.response(status.getSuccessCode(),
                    "Analytics summary fetched successfully",
                    response
            );

        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetch analytics summary");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> getDashboardAnalytics(int year, int month) {

        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        try {
            // Fetch utility companies
            List<Organization> organizations = analyticsMapper.getAllOrganizations();
            long totalCustomers = analyticsMapper.getTotalCustomer();

            List<IncidentReport> incidentReport = analyticsMapper.incidentReportResolveAnalytics();

            long totalResolved = incidentReport.stream().filter(IncidentReport::getStatus).count();

            long totalUnresolved = incidentReport.size() - totalResolved;

            // Fetch daily reports
            List<UptimeReport> dailyReports = reportRepository
                    .findByReportTypeAndCreatedAtBetweenAndServiceNameIn(
                            "DAILY", startDate.toString(), endDate.toString(), SERVICES
                    );

            // Fetch monthly reports
            List<UptimeReport> monthlyReports = reportRepository
                    .findByReportTypeAndMonthStartingWithAndServiceNameIn(
                            "MONTHLY", String.valueOf(year), SERVICES
                    );

            // Daily Summaries (grouped by createdAt)
            Map<String, List<UptimeReport>> reportsByDate = dailyReports.stream()
                    .collect(Collectors.groupingBy(UptimeReport::getCreatedAt));

            List<Map<String, Object>> dailySummaries = new ArrayList<>();
            for (Map.Entry<String, List<UptimeReport>> entry : reportsByDate.entrySet()) {
                String date = entry.getKey();
                List<UptimeReport> reportsForDay = entry.getValue();

                long up = reportsForDay.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
                long down = reportsForDay.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
                long total = up + down;

                Map<String, Object> summary = new HashMap<>();
                summary.put("date", date);
                summary.put("services", reportsForDay.stream().map(UptimeReport::getServiceName).toList());
                summary.put("uptimeMinutes", up);
                summary.put("downtimeMinutes", down);
                summary.put("uptimePercent", total == 0 ? 0 : (up * 100.0 / total));
                summary.put("downtimePercent", total == 0 ? 0 : (down * 100.0 / total));

                dailySummaries.add(summary);
            }

            // Overall Daily Summary
            long totalUp = dailyReports.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
            long totalDown = dailyReports.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
            long total = totalUp + totalDown;

            Map<String, Object> dailySummary = new HashMap<>();
            dailySummary.put("services", SERVICES);
            dailySummary.put("uptimePercent", total == 0 ? 0 : (totalUp * 100.0 / total));
            dailySummary.put("downtimePercent", total == 0 ? 0 : (totalDown * 100.0 / total));
            dailySummary.put("uptimeMinutes", totalUp);
            dailySummary.put("downtimeMinutes", totalDown);

            // Monthly Summaries (group by month string)
            Map<String, List<UptimeReport>> reportsByMonth = monthlyReports.stream()
                    .collect(Collectors.groupingBy(UptimeReport::getMonth));

            List<Map<String, Object>> monthlySummaries = new ArrayList<>();
            for (Map.Entry<String, List<UptimeReport>> entry : reportsByMonth.entrySet()) {
                String monthStr = entry.getKey();
                List<UptimeReport> reportsForMonth = entry.getValue();

                long up = reportsForMonth.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
                long down = reportsForMonth.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
                long totalM = up + down;

                // Parse monthStr -> YearMonth
                YearMonth reportYm = YearMonth.parse(monthStr);

                Map<String, Object> summary = new HashMap<>();
                summary.put("month", monthStr);
                summary.put("monthDisplay", reportYm.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));// per-month summaries// per-month summaries
                summary.put("services", reportsForMonth.stream().map(UptimeReport::getServiceName).distinct().toList());
                summary.put("uptimeMinutes", up);
                summary.put("downtimeMinutes", down);
                summary.put("uptimePercent", totalM == 0 ? 0 : (up * 100.0 / totalM));
                summary.put("downtimePercent", totalM == 0 ? 0 : (down * 100.0 / totalM));

                monthlySummaries.add(summary);
            }

            // Sort summaries chronologically
            monthlySummaries.sort(Comparator.comparing(m -> YearMonth.parse((String) m.get("month"))));

            // Remove helper key (yearMonth) before sending to frontend
            monthlySummaries.forEach(m -> m.remove("yearMonth"));

            // Total Monthly Summary (aggregate across all months)
            long monthlyTotalUp = monthlyReports.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
            long monthlyTotalDown = monthlyReports.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
            long monthlyTotal = monthlyTotalUp + monthlyTotalDown;

            Map<String, Object> totalMonthlySummary = new HashMap<>();
            totalMonthlySummary.put("services", monthlyReports.stream()
                    .map(UptimeReport::getServiceName).distinct().toList());
            totalMonthlySummary.put("uptimeMinutes", monthlyTotalUp);
            totalMonthlySummary.put("downtimeMinutes", monthlyTotalDown);
            totalMonthlySummary.put("uptimePercent", monthlyTotal == 0 ? 0 : (monthlyTotalUp * 100.0 / monthlyTotal));
            totalMonthlySummary.put("downtimePercent", monthlyTotal == 0 ? 0 : (monthlyTotalDown * 100.0 / monthlyTotal));

            // Build Final Response
            Map<String, Object> response = new HashMap<>();
            response.put("dailyReports", dailyReports);            // raw daily reports
            response.put("dailySummaries", dailySummaries);        // per-day summaries
            response.put("totalDailySummary", dailySummary);       // overall daily
            response.put("monthlyReports", monthlyReports);        // raw monthly reports
            response.put("monthlySummaries", monthlySummaries);
            response.put("totalMonthlySummary", totalMonthlySummary); // overall monthly
            response.put("totalUtilityCompany", organizations.size());
            response.put("totalCustomers", totalCustomers);
            response.put("totalResolvedIncident", totalResolved); // TODO
            response.put("totalUnresolvedIncident", totalUnresolved); // TODO
            response.put("incidentReports", incidentReport); // TODO

            return ResponseMap.response(status.getSuccessCode(),
                    "Analytics summary fetched successfully",
                    response
            );

        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "fetching dashboard analytics");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> getIncidentReport(Boolean state, int page, int size) {
        try {
            List<IncidentReport> allReports = analyticsMapper.getIncidentReport();

            List<IncidentReport> filteredReports;
            if (state == null) {
                // No filter → return all
                filteredReports = allReports;
            } else {
                // Filter by status
                filteredReports = allReports.stream()
                        .filter(i -> state.equals(i.getStatus()))
                        .toList();
            }
            // Pagination logic
            int totalReports = filteredReports.size();
            List<IncidentReport> paginatedReports;
            if (size == 0) {
                paginatedReports = filteredReports; // Return all users
            } else {
                int fromIndex = Math.min(page * size, totalReports);
                int toIndex = Math.min(fromIndex + size, totalReports);
                paginatedReports = filteredReports.subList(fromIndex, toIndex);
            }

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedReports);
            response.put("totalData", totalReports);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) paginatedReports.size() / size));


            return ResponseMap.response(status.getSuccessCode(),
                    "Incident reports fetched successfully", response
            );
        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "fetching incident report");
            throw exception;
        }
    }


    @Override
    public Map<String, Object> getIncidentReportResolve(UUID id, Boolean state) {
        try {
            IncidentReport response = analyticsMapper.getIncidentReportResolve(state, id);
            return ResponseMap.response(status.getSuccessCode(),
                    "Incident reports resolved successfully", response
            );
        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "resolving incident report");
            throw exception;
        }
    }



}
