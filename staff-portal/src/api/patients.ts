import { api } from './client';
import type { PatientResponse } from './types';

export function searchPatients(query: string) {
  return api.get<PatientResponse[]>(`/api/v1/patients?query=${encodeURIComponent(query)}`);
}

export interface RegisterPatientPayload {
  firstName: string;
  lastName: string;
  birthDate: string;
  sex: 'MALE' | 'FEMALE';
  mobileNumber?: string;
  email?: string;
  registrationSource: 'FRONT_DESK' | 'PHONE' | 'WALK_IN' | 'STAFF';
  privacyNoticeVersion?: string;
  dataProcessingConsentGranted: boolean;
}

export function registerPatient(payload: RegisterPatientPayload) {
  return api.post<PatientResponse>('/api/v1/patients', payload);
}
