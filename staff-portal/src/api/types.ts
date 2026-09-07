export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: { code: string; message: string; details?: unknown } | null;
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

export interface ServiceResponse {
  id: string;
  name: string;
  description: string | null;
  durationMinutes: number;
  bufferMinutes: number;
  price: number | null;
  consultationMode: string;
  allowedSpecialty: string | null;
  allowedSpecialtyId: string | null;
  status: string;
  clinics: string[];
  clinicIds: string[];
}

export interface ClinicResponse {
  id: string;
  name: string;
  addressLine: string;
  city: string | null;
  province: string | null;
  postalCode: string | null;
  contactNumber: string | null;
  timezone: string;
  status: string;
}

export interface SlotResponse {
  slotId: string;
  practitionerId: string;
  clinicId: string;
  start: string;
  end: string;
  status: string;
}

export interface AvailabilityResponse {
  practitionerId: string;
  serviceId: string | null;
  slots: SlotResponse[];
}

export interface AppointmentTypeResponse {
  id: string;
  code: string;
  name: string;
  confirmationPolicy: string;
  cancellationWindowHours: number | null;
  advanceBookingLimitDays: number | null;
  active: boolean;
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

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  tenantId: string;
  email: string;
  roles: string[];
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

export interface CheckInResponse {
  id: string;
  appointmentId: string;
  patientId: string;
  checkedInAt: string;
  method: string;
  identityVerified: boolean;
  queueTicketId: string;
  ticketNumber: string;
}

export interface ScheduleRuleResponse {
  id: string;
  practitionerId: string;
  clinicId: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  effectiveFrom: string;
  effectiveTo: string | null;
  slotDurationMinutes: number;
  bufferMinutes: number;
  status: string;
}

export interface ReportSummaryResponse {
  fromDate: string;
  toDate: string;
  totalAppointments: number;
  confirmedCount: number;
  cancelledCount: number;
  noShowCount: number;
  completedCount: number;
  confirmationRatePct: number;
  cancellationRatePct: number;
  noShowRatePct: number;
  averageBookingLeadTimeHours: number | null;
  averageWaitTimeMinutes: number | null;
  queueTicketsCompleted: number;
}

export interface SpecialtyResponse {
  id: string;
  code: string;
  name: string;
}

export interface NotificationResponse {
  id: string;
  eventCode: string;
  channel: string;
  recipientContact: string | null;
  renderedBody: string;
  status: string;
  sentAt: string | null;
  errorMessage: string | null;
}
