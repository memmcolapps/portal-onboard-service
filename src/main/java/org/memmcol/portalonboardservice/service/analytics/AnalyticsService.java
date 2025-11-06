package org.memmcol.portalonboardservice.service.analytics;

import java.time.LocalDate;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public interface AnalyticsService {
//    Map<String, Object> getAnalytics(int resolvedYear, int resolvedMonth, Integer resolvedDay);

    Map<String, Object> getAnalytics(LocalDate day);

    Map<String, Object> getDashboardAnalytics(LocalDate day);

    Map<String, Object> getIncidentReport(Boolean status, int page, int size);

    Map<String, Object> getIncidentReportResolve(UUID id, Boolean status);
}
