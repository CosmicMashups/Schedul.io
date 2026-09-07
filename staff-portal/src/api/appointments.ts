import { api } from './client';
import type { AppointmentResponse, AppointmentTypeResponse } from './types';

export function searchAppointments(params: {
  practitionerId?: string; clinicId?: string; status?: string; fromDate?: string; toDate?: string;
} = {}) {
  const qs = new URLSearchParams(Object.entries(params).filter(([, v]) => v) as [string, string][]);
  return api.get<AppointmentResponse[]>(`/api/v1/staff/appointments?${qs.toString()}`);
}

export function listAppointmentTypes() {
  return api.get<AppointmentTypeResponse[]>('/api/v1/appointment-types');
}

export interface StaffBookPayload {
  patientId: string;
  practitionerId: string;
  clinicId: string;
  serviceId: string;
  appointmentTypeId: string;
  slotId: string;
  reason?: string;
  source: 'PHONE' | 'FRONT_DESK';
}

export function staffBookAppointment(payload: StaffBookPayload) {
  return api.post<AppointmentResponse>('/api/v1/staff/appointments', payload);
}

export function confirmAppointment(id: string) {
  return api.post<AppointmentResponse>(`/api/v1/staff/appointments/${id}/confirm`);
}

export function rejectAppointment(id: string, reason?: string) {
  return api.post<AppointmentResponse>(`/api/v1/staff/appointments/${id}/reject`, reason ? { reason } : undefined);
}

export function markNoShow(id: string) {
  return api.post<AppointmentResponse>(`/api/v1/staff/appointments/${id}/no-show`);
}

export function cancelAppointment(id: string, reason: string, note?: string) {
  return api.post<AppointmentResponse>(`/api/v1/appointments/${id}/cancel`, { reason, note });
}

export function getAppointment(id: string) {
  return api.get<AppointmentResponse>(`/api/v1/appointments/${id}`);
}

export function rescheduleAppointment(id: string, newSlotId: string, reason?: string) {
  return api.post<AppointmentResponse>(`/api/v1/appointments/${id}/reschedule`, { newSlotId, reason });
}
