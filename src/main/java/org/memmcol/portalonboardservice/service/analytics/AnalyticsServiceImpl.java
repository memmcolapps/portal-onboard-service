package org.memmcol.portalonboardservice.service.analytics;

import com.mongodb.lang.Nullable;
import org.memmcol.portalonboardservice.config.ResponseProperties;
import org.memmcol.portalonboardservice.mapper.AnalyticsMapper;
import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
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
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            // Fetch daily reports directly from DB
            List<UptimeReport> dailyReports = reportRepository
                    .findByReportTypeAndCreatedAtBetweenAndServiceNameIn(
                            "DAILY", startDate.toString(), endDate.toString(), SERVICES
                    );

            // Fetch monthly reports directly from DB
            List<UptimeReport> monthlyReports = reportRepository
                    .findByReportTypeAndMonthAndServiceNameIn(
                            "MONTHLY", ym.toString(), SERVICES
                    );

            // Aggregate uptime/downtime across both services for this day
            long totalUp = dailyReports.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
            long totalDown = dailyReports.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
            long total = totalUp + totalDown;

            // Aggregate uptime/downtime across both services for this day
            long monthlyTotalUp = monthlyReports.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
            long monthlyTotalDown = monthlyReports.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
            long monthlyTotal = monthlyTotalUp + monthlyTotalDown;

            Map<String, Object> aggregated = new HashMap<>();
            aggregated.put("services", SERVICES);
            aggregated.put("uptimePercent", total == 0 ? 0 : (totalUp * 100.0 / total));
            aggregated.put("downtimePercent", total == 0 ? 0 : (totalDown * 100.0 / total));
            aggregated.put("uptimeMinutes", totalUp);
            aggregated.put("downtimeMinutes", totalDown);

            Map<String, Object> monthlyAggregated = new HashMap<>();
            monthlyAggregated.put("services", SERVICES);
            monthlyAggregated.put("uptimePercent", monthlyTotal == 0 ? 0 : (monthlyTotalUp * 100.0 / monthlyTotal));
            monthlyAggregated.put("downtimePercent", monthlyTotal == 0 ? 0 : (monthlyTotalDown * 100.0 / monthlyTotal));
            monthlyAggregated.put("uptimeMinutes", monthlyTotalUp);
            monthlyAggregated.put("downtimeMinutes", monthlyTotalDown);

            // Final response
            Map<String, Object> response = new HashMap<>();
            response.put("dailyReports", dailyReports);
            response.put("dailySummary", aggregated);
            response.put("monthlyReports", monthlyReports);
            response.put("monthlySummary", monthlyAggregated);
            response.put("totalUtilityCompany", organizations.size());
            response.put("activeUtilityCompany", activeCount);
            response.put("incidentReport", 0); // TODO: compute if needed
            response.put("averageRecoveryTime", 0); // TODO: compute if needed

            return ResponseMap.response(status.getSuccessCode(), "Analytics summary fetched successfully", response);
        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetch analytics summary");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

}
