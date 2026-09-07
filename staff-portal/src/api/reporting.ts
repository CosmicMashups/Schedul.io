import { api } from './client';
import type { ReportSummaryResponse } from './types';

export function getReportSummary(params: { from: string; to: string; clinicId?: string; practitionerId?: string }) {
  const qs = new URLSearchParams(Object.entries(params).filter(([, v]) => v) as [string, string][]);
  return api.get<ReportSummaryResponse>(`/api/v1/staff/reports/summary?${qs.toString()}`);
}
