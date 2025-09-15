package org.memmcol.portalonboardservice.service.analytics;

import java.util.Map;

public interface AnalyticsService {
//    Map<String, Object> getAnalytics(int resolvedYear, int resolvedMonth, Integer resolvedDay);

    Map<String, Object> getAnalytics(int year, int month);

    Map<String, Object> getDashboardAnalytics(int resolvedYear, int resolvedMonth);

    Map<String, Object> getIncidentReport(String type);

    Map<String, Object> getIncidentReportResolve(Boolean status);
}
