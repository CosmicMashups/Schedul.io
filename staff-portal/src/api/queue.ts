import { api } from './client';
import type { QueueResponse, QueueTicketResponse } from './types';

export function listQueues(clinicId: string) {
  return api.get<QueueResponse[]>(`/api/v1/queues?clinicId=${clinicId}`);
}

export function listActiveTickets(queueId: string) {
  return api.get<QueueTicketResponse[]>(`/api/v1/queues/${queueId}/tickets`);
}

export function enqueueWalkIn(queueId: string, patientId: string, practitionerId?: string) {
  return api.post<QueueTicketResponse>(`/api/v1/queues/${queueId}/walk-in`, { patientId, practitionerId });
}

export function callNext(queueId: string) {
  return api.post<QueueTicketResponse>(`/api/v1/queues/${queueId}/call-next`);
}

export function startServing(ticketId: string) {
  return api.post<QueueTicketResponse>(`/api/v1/queue-tickets/${ticketId}/serve`);
}

export function completeTicket(ticketId: string) {
  return api.post<QueueTicketResponse>(`/api/v1/queue-tickets/${ticketId}/complete`);
}

export function skipTicket(ticketId: string) {
  return api.post<QueueTicketResponse>(`/api/v1/queue-tickets/${ticketId}/skip`);
}

export function leaveWithoutBeingSeen(ticketId: string) {
  return api.post<QueueTicketResponse>(`/api/v1/queue-tickets/${ticketId}/left-without-being-seen`);
}
