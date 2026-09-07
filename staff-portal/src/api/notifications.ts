import { api } from './client';
import type { NotificationResponse } from './types';

export function getNotificationHistory(patientId: string) {
  return api.get<NotificationResponse[]>(`/api/v1/staff/patients/${patientId}/notifications`);
}
