import { api } from './client';
import type { AppointmentResponse } from './types';

export function searchMyAppointments(practitionerId: string, fromDate: string, toDate: string) {
  const qs = new URLSearchParams({ practitionerId, fromDate, toDate });
  return api.get<AppointmentResponse[]>(`/api/v1/staff/appointments?${qs.toString()}`);
}
