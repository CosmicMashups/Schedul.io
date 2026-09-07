package com.clinic.platform.reporting.controller;

import com.clinic.platform.common.response.ApiResponse;
import com.clinic.platform.reporting.dto.ReportSummaryResponse;
import com.clinic.platform.reporting.service.ReportingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@PreAuthorize("hasAuthority('REPORT_VIEW')")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/api/v1/staff/reports/summary")
    public ApiResponse<ReportSummaryResponse> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID clinicId,
            @RequestParam(required = false) UUID practitionerId) {
        return ApiResponse.ok(reportingService.summary(from, to, clinicId, practitionerId));
    }
}
