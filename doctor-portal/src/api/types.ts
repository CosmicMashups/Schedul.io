export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: { code: string; message: string; details?: unknown } | null;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  tenantId: string;
  email: string;
  roles: string[];
}

export interface PractitionerResponse {
  id: string;
  firstName: string;
  lastName: string;
  credentials: string | null;
  defaultConsultationFee: number | null;
  status: string;
  specialties: string[];
  specialtyIds: string[];
  clinics: string[];
  clinicIds: string[];
}

export interface AppointmentResponse {
  id: string;
  patientId: string;
  practitionerId: string;
  clinicId: string;
  serviceId: string;
  appointmentTypeId: string;
  slotId: string;
  scheduledStart: string;
  scheduledEnd: string;
  status: string;
  reason: string | null;
  source: string;
  previousAppointmentId: string | null;
  replacementAppointmentId: string | null;
  cancellationReason: string | null;
  cancellationNote: string | null;
}

export interface PatientResponse {
  id: string;
  firstName: string;
  middleName: string | null;
  lastName: string;
  suffix: string | null;
  birthDate: string;
  sex: string;
  mobileNumber: string | null;
  email: string | null;
  addressLine: string | null;
  preferredContactMethod: string;
}

export interface QueueResponse {
  id: string;
  clinicId: string;
  name: string;
  type: string;
  status: string;
}

export interface QueueTicketResponse {
  id: string;
  queueId: string;
  appointmentId: string | null;
  patientId: string;
  practitionerId: string | null;
  ticketNumber: string;
  priority: number;
  status: string;
  calledAt: string | null;
  servingStartedAt: string | null;
  completedAt: string | null;
}
