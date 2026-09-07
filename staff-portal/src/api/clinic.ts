import { api } from './client';
import type { ClinicResponse, PractitionerResponse, ServiceResponse } from './types';

export function listClinics() {
  return api.get<ClinicResponse[]>('/api/v1/staff/clinics');
}

export function searchPractitioners(params: { name?: string; specialty?: string; clinicId?: string } = {}) {
  const qs = new URLSearchParams(Object.entries(params).filter(([, v]) => v) as [string, string][]);
  return api.get<PractitionerResponse[]>(`/api/v1/doctors?${qs.toString()}`);
}

export interface PractitionerPayload {
  firstName: string;
  lastName: string;
  credentials?: string;
  defaultConsultationFee?: number;
  specialtyIds: string[];
  clinicIds: string[];
}

export function createPractitioner(payload: PractitionerPayload) {
  return api.post<PractitionerResponse>('/api/v1/staff/practitioners', payload);
}

export function listServices(clinicId?: string) {
  const qs = clinicId ? `?clinicId=${clinicId}` : '';
  return api.get<ServiceResponse[]>(`/api/v1/services${qs}`);
}

export interface ServicePayload {
  name: string;
  description?: string;
  durationMinutes: number;
  bufferMinutes: number;
  price?: number;
  consultationMode: 'IN_PERSON' | 'TELECONSULT' | 'EITHER';
  allowedSpecialtyId?: string;
  clinicIds: string[];
}

export function createService(payload: ServicePayload) {
  return api.post<ServiceResponse>('/api/v1/staff/services', payload);
}

export function listSpecialties() {
  return api.get<import('./types').SpecialtyResponse[]>('/api/v1/specialties');
}

export function createSpecialty(code: string, name: string) {
  return api.post<import('./types').SpecialtyResponse>('/api/v1/specialties', { code, name });
}

export interface CreateUserPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  mobileNumber?: string;
  roleCodes: string[];
}

export function createUser(payload: CreateUserPayload) {
  return api.post<{ id: string; email: string; firstName: string; lastName: string; status: string; roles: string[] }>('/api/v1/staff/users', payload);
}

export function linkPractitionerUser(practitionerId: string, userId: string) {
  return api.post<PractitionerResponse>(`/api/v1/staff/practitioners/${practitionerId}/link-user`, { userId });
}

export function getAvailability(params: { practitionerId: string; serviceId?: string; clinicId?: string; from: string; to: string }) {
  const qs = new URLSearchParams(Object.entries(params).filter(([, v]) => v) as [string, string][]);
  return api.get<import('./types').AvailabilityResponse>(`/api/v1/availability?${qs.toString()}`);
}

export function holdSlot(slotId: string) {
  return api.post(`/api/v1/slots/${slotId}/hold`);
}

export function releaseSlot(slotId: string) {
  return api.post(`/api/v1/slots/${slotId}/release`);
}
