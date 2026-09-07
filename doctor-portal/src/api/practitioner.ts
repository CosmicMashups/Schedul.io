import { api } from './client';
import type { PractitionerResponse } from './types';

/** Resolves the practitioner record linked to the signed-in doctor's account (backend: PractitionerService.getMe). */
export function getMyProfile() {
  return api.get<PractitionerResponse>('/api/v1/doctors/me');
}
