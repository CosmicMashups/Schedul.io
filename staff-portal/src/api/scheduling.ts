import { api } from './client';
import type { ScheduleRuleResponse } from './types';

export interface ScheduleRulePayload {
  practitionerId: string;
  clinicId: string;
  dayOfWeek: string;
  startTime: string; // HH:mm
  endTime: string;
  effectiveFrom: string; // yyyy-MM-dd
  effectiveTo?: string;
  slotDurationMinutes: number;
  bufferMinutes: number;
}

export function createScheduleRule(payload: ScheduleRulePayload) {
  return api.post<ScheduleRuleResponse>('/api/v1/schedules', payload);
}

export function listScheduleRules(practitionerId: string) {
  return api.get<ScheduleRuleResponse[]>(`/api/v1/schedules?practitionerId=${practitionerId}`);
}
