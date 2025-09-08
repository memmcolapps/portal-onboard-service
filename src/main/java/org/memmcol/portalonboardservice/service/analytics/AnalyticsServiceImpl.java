package org.memmcol.portalonboardservice.service.analytics;

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
                            "DAILY", startDate, endDate, SERVICES
                    );

            // Fetch monthly reports directly from DB
            List<UptimeReport> monthlyReports = reportRepository
                    .findByReportTypeAndMonthAndServiceNameIn(
                            "MONTHLY", ym.toString(), SERVICES
                    );

            // Aggregate uptime/downtime across both services for this month
            long totalUp = dailyReports.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
            long totalDown = dailyReports.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
            long total = totalUp + totalDown;

            Map<String, Object> aggregated = new HashMap<>();
            aggregated.put("services", SERVICES);
            aggregated.put("uptimePercent", total == 0 ? 0 : (totalUp * 100.0 / total));
            aggregated.put("downtimePercent", total == 0 ? 0 : (totalDown * 100.0 / total));
            aggregated.put("uptimeMinutes", totalUp);
            aggregated.put("downtimeMinutes", totalDown);

            // Final response
            Map<String, Object> response = new HashMap<>();
            response.put("dailyReports", dailyReports);
            response.put("monthlyReports", monthlyReports);
            response.put("aggregatedSummary", aggregated);
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


//    @Override
//    public Map<String, Object> getAnalytics(int year, int month) {
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//
//        YearMonth ym = YearMonth.of(year, month);
//        LocalDate startDate = ym.atDay(1);
//        LocalDate endDate = ym.atEndOfMonth();
//
//        try {
//            //Fetch utility company
//            List<Organization> organization = analyticsMapper.getAllOrganizations();
//
//            long count = organization.stream().filter(Organization::getStatus).count();
//
//
//            // Fetch daily reports for both services
//            List<UptimeReport> dailyReports = reportRepository.findAll().stream()
//                    .filter(r -> "DAILY".equals(r.getReportType())
//                            && r.getCreatedAt() != null
//                            && !r.getCreatedAt().isBefore(startDate)
//                            && !r.getCreatedAt().isAfter(endDate)
//                            && SERVICES.contains(r.getServiceName()))
//                    .toList();
//
//            // Fetch monthly reports for both services
//            List<UptimeReport> monthlyReports = reportRepository.findAll().stream()
//                    .filter(r -> "MONTHLY".equals(r.getReportType())
//                            && r.getMonth() != null
//                            && r.getMonth().equals(ym)
//                            && SERVICES.contains(r.getServiceName()))
//                    .toList();
//
//            // Aggregate uptime/downtime across both services
//            long totalUp = dailyReports.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
//            long totalDown = dailyReports.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
//            long total = totalUp + totalDown;
//
//            Map<String, Object> aggregated = new HashMap<>();
//            aggregated.put("services", SERVICES);
//            aggregated.put("uptimePercent", total == 0 ? 0 : (totalUp * 100.0 / total));
//            aggregated.put("downtimePercent", total == 0 ? 0 : (totalDown * 100.0 / total));
//            aggregated.put("uptimeMinutes", totalUp);
//            aggregated.put("downtimeMinutes", totalDown);
//
//            // Final response
//            Map<String, Object> response = new HashMap<>();
//            response.put("dailyReports", dailyReports);
//            response.put("monthlyReports", monthlyReports);
//            response.put("aggregatedSummary", aggregated);
//            response.put("totalUtilityCompany", organization.size());
//            response.put("activeUtilityCompany", count);
//            response.put("incidentReport", 0);
//            response.put("averageRecoveryTime", 0);
//
//            return ResponseMap.response(status.getSuccessCode(), "Analytics summary fetched successfully", response);
//        } catch (Exception exception) {
//            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
//            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
//            exceptionErrorLogs.setError_message(exception.getMessage().trim());
//            exceptionErrorLogs.setError(exception.toString().trim());
//            exceptionAuditRepository.save(exceptionErrorLogs);
//            throw exception;
//        }
//    }
}
