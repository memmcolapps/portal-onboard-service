package org.memmcol.portalonboardservice.repository;

import org.memmcol.portalonboardservice.model.audit.UptimeReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface UptimeReportRepository extends MongoRepository<UptimeReport, String> {
    List<UptimeReport> findByReportTypeAndCreatedAtBetweenAndServiceNameIn(String reportType, LocalDate startDate, LocalDate endDate, List<String> services);

    List<UptimeReport> findByReportTypeAndMonthAndServiceNameIn(String reportType, String ym, List<String> services);
}
