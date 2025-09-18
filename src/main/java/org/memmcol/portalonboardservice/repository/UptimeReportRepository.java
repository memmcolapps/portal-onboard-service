package org.memmcol.portalonboardservice.repository;

import org.memmcol.portalonboardservice.model.audit.UptimeReport;
import org.springframework.data.mongodb.repository.MongoRepository;


import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface UptimeReportRepository extends MongoRepository<UptimeReport, String> {
    List<UptimeReport> findByReportTypeAndCreatedAtBetweenAndServiceNameIn(String reportType, String startDate, String endDate, List<String> services);

//    List<UptimeReport> findByReportTypeAndMonthAndServiceNameIn(String reportType, String ym, List<String> services);

    // Fetch all monthly reports for a whole year (e.g. "2025-")
    List<UptimeReport> findByReportTypeAndMonthStartingWithAndServiceNameIn(
            String reportType, String yearPrefix, List<String> services);

}
