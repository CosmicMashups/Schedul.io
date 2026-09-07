import { api, setTenantId, setToken } from './client';
import type { LoginResponse, PatientResponse } from './types';

export async function login(tenantId: string, email: string, password: string): Promise<LoginResponse> {
  setTenantId(tenantId);
  const res = await api.post<LoginResponse>('/api/v1/auth/login', { email, password });
  setToken(res.accessToken);
  return res;
}

export interface RegisterPatientPayload {
  firstName: string;
  middleName?: string;
  lastName: string;
  suffix?: string;
  birthDate: string; // yyyy-MM-dd
  sex: 'MALE' | 'FEMALE';
  mobileNumber?: string;
  email?: string;
  addressLine?: string;
  emergencyContactName?: string;
  emergencyContactNumber?: string;
  preferredContactMethod?: 'SMS' | 'EMAIL' | 'PHONE_CALL';
  registrationSource?: 'ONLINE';
  privacyNoticeVersion?: string;
  dataProcessingConsentGranted: boolean;
}

export function registerPatient(payload: RegisterPatientPayload) {
  return api.post<PatientResponse>('/api/v1/patients', payload);
}
