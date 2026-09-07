import { api } from './client';
import type { CheckInResponse } from './types';

export function checkIn(appointmentId: string, method: string, identityVerified: boolean) {
  return api.post<CheckInResponse>(`/api/v1/staff/appointments/${appointmentId}/check-in`, { method, identityVerified });
}
