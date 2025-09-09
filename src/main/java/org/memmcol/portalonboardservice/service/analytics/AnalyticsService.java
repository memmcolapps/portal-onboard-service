package org.memmcol.portalonboardservice.service.analytics;

import java.util.Map;

public interface AnalyticsService {
//    Map<String, Object> getAnalytics(int resolvedYear, int resolvedMonth, Integer resolvedDay);

    Map<String, Object> getAnalytics(int year, int month);
}
