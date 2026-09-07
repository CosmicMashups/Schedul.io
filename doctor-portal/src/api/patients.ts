import { api } from './client';
import type { PatientResponse } from './types';

export function getPatient(id: string) {
  return api.get<PatientResponse>(`/api/v1/patients/${id}`);
}
