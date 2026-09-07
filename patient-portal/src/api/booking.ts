import { api } from './client';
import type {
  AppointmentResponse,
  AppointmentTypeResponse,
  AvailabilityResponse,
  ClinicResponse,
  PractitionerResponse,
  ServiceResponse,
} from './types';

export function searchDoctors(params: { specialty?: string; clinicId?: string; name?: string }) {
  const qs = new URLSearchParams(Object.entries(params).filter(([, v]) => v) as [string, string][]);
  return api.get<PractitionerResponse[]>(`/api/v1/doctors?${qs.toString()}`);
}

export function getDoctor(id: string) {
  return api.get<PractitionerResponse>(`/api/v1/doctors/${id}`);
}

export function listServices(clinicId?: string) {
  const qs = clinicId ? `?clinicId=${clinicId}` : '';
  return api.get<ServiceResponse[]>(`/api/v1/services${qs}`);
}

export function listClinics() {
  return api.get<ClinicResponse[]>('/api/v1/staff/clinics'); // read is unauthenticated-safe per backend RBAC (no PreAuthorize on GET)
}

export function listAppointmentTypes() {
  return api.get<AppointmentTypeResponse[]>('/api/v1/appointment-types');
}

export function getAvailability(params: {
  practitionerId: string;
  serviceId?: string;
  clinicId?: string;
  from: string; // yyyy-MM-dd
  to: string;
}) {
  const qs = new URLSearchParams(Object.entries(params).filter(([, v]) => v) as [string, string][]);
  return api.get<AvailabilityResponse>(`/api/v1/availability?${qs.toString()}`);
}

export function holdSlot(slotId: string) {
  return api.post(`/api/v1/slots/${slotId}/hold`);
}

export function releaseSlot(slotId: string) {
  return api.post(`/api/v1/slots/${slotId}/release`);
}

export interface BookAppointmentPayload {
  patientId: string;
  practitionerId: string;
  clinicId: string;
  serviceId: string;
  appointmentTypeId: string;
  slotId: string;
  reason?: string;
  source?: 'ONLINE';
}

export function bookAppointment(payload: BookAppointmentPayload) {
  return api.post<AppointmentResponse>('/api/v1/appointments', payload);
}

export function getAppointment(id: string) {
  return api.get<AppointmentResponse>(`/api/v1/appointments/${id}`);
}

export function listMyAppointments(patientId: string) {
  return api.get<AppointmentResponse[]>(`/api/v1/patients/${patientId}/appointments`);
}

export function cancelAppointment(id: string, reason: string, note?: string) {
  return api.post<AppointmentResponse>(`/api/v1/appointments/${id}/cancel`, { reason, note });
}

export function rescheduleAppointment(id: string, newSlotId: string, reason?: string) {
  return api.post<AppointmentResponse>(`/api/v1/appointments/${id}/reschedule`, { newSlotId, reason });
}

// Closes the "no way to resolve a returning patient's record" gap: the backend now exposes
// GET /api/v1/patients/me for accounts that registered via SELF_REGISTER (see PatientService.getMe).
export function getMyPatientRecord() {
  return api.get<import('./types').PatientResponse>('/api/v1/patients/me');
}
