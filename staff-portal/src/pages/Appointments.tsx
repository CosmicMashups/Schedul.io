import { FormEvent, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { CalendarClock, Check, LogIn, PlusCircle, Slash, UserX } from 'lucide-react';
import {
  cancelAppointment, confirmAppointment, listAppointmentTypes, markNoShow, rejectAppointment,
  rescheduleAppointment, searchAppointments, staffBookAppointment,
} from '../api/appointments';
import { getAvailability, holdSlot, listClinics, listServices, releaseSlot, searchPractitioners } from '../api/clinic';
import { searchPatients } from '../api/patients';
import { ApiError } from '../api/client';
import type { AppointmentResponse, ClinicResponse, PatientResponse, PractitionerResponse, ServiceResponse, SlotResponse } from '../api/types';
import { StatusPill } from '../components/StatusPill';
import { Skeleton } from '../components/Skeleton';
import { EmptyState } from '../components/EmptyState';
import { Spinner } from '../components/Spinner';
import { SlotPicker } from '../components/SlotPicker';
import { useToast } from '../components/Toast';

const STATUS_FILTERS = ['', 'PENDING_CONFIRMATION', 'CONFIRMED', 'CHECKED_IN', 'IN_QUEUE', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];

export default function Appointments() {
  const [status, setStatus] = useState('');
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [showNewForm, setShowNewForm] = useState(false);
  const [rescheduling, setRescheduling] = useState<AppointmentResponse | null>(null);
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  async function load() {
    setLoading(true);
    try {
      setAppointments(await searchAppointments(status ? { status } : {}));
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not load appointments.', 'error');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, [status]);

  useEffect(() => {
    if ((location.state as { openNewAppointment?: boolean } | null)?.openNewAppointment) setShowNewForm(true);
  }, [location.state]);

  async function runAction(id: string, action: () => Promise<unknown>, successMessage: string) {
    setBusyId(id);
    try {
      await action();
      await load();
      toast.show(successMessage);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Action failed.', 'error');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="animate-page">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <CalendarClock size={20} className="text-teal" />
          <h1 className="text-xl font-semibold text-ink">Appointments</h1>
        </div>
        <div className="flex items-center gap-2">
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            className="rounded-lg border border-line bg-panel px-3 py-2 text-sm focus-ring"
          >
            {STATUS_FILTERS.map((s) => <option key={s} value={s}>{s || 'All statuses'}</option>)}
          </select>
          <button
            onClick={() => { setShowNewForm((v) => !v); setRescheduling(null); }}
            className="btn-press flex items-center gap-1.5 rounded-lg border border-line px-3.5 py-2 text-sm font-medium hover:border-teal focus-ring"
          >
            <PlusCircle size={15} /> {showNewForm ? 'Cancel' : 'New appointment'}
          </button>
        </div>
      </div>

      {showNewForm && (
        <NewAppointmentForm onDone={() => { setShowNewForm(false); load(); toast.show('Appointment booked.'); }} />
      )}

      {rescheduling && (
        <RescheduleForm
          appointment={rescheduling}
          onDone={() => { setRescheduling(null); load(); toast.show('Appointment rescheduled.'); }}
          onCancel={() => setRescheduling(null)}
        />
      )}

      {!showNewForm && !rescheduling && (
        <>
          {loading && appointments.length === 0 && (
            <div className="panel p-4 space-y-3">
              {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10" />)}
            </div>
          )}

          {!loading && appointments.length === 0 && (
            <div className="panel"><EmptyState icon={CalendarClock} title="No appointments match this filter" /></div>
          )}

          {appointments.length > 0 && (
            <div className="panel overflow-hidden">
              <table className="data-table w-full">
                <thead>
                  <tr>
                    <th scope="col">When</th>
                    <th scope="col">Status</th>
                    <th scope="col">Reason</th>
                    <th scope="col">Source</th>
                    <th scope="col"><span className="sr-only">Actions</span></th>
                  </tr>
                </thead>
                <tbody>
                  {appointments.map((a) => (
                    <tr key={a.id} className={busyId === a.id ? 'opacity-50' : ''}>
                      <td className="font-mono text-xs tabular-nums">
                        {new Date(a.scheduledStart).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })}
                      </td>
                      <td><StatusPill status={a.status} /></td>
                      <td className="text-ink/60">{a.reason ?? '\u2014'}</td>
                      <td className="text-ink/50 text-xs uppercase">{a.source}</td>
                      <td className="text-right">
                        <div className="flex justify-end gap-1.5">
                          {a.status === 'PENDING_CONFIRMATION' && (
                            <>
                              <ActionButton icon={Check} label="Confirm" busy={busyId === a.id} onClick={() => runAction(a.id, () => confirmAppointment(a.id), 'Appointment confirmed.')} />
                              <ActionButton icon={Slash} label="Reject" tone="coral" busy={busyId === a.id} onClick={() => runAction(a.id, () => rejectAppointment(a.id), 'Appointment rejected.')} />
                            </>
                          )}
                          {a.status === 'CONFIRMED' && (
                            <>
                              <ActionButton icon={LogIn} label="Check in" tone="teal" busy={busyId === a.id} onClick={() => navigate('/check-in', { state: { appointmentId: a.id } })} />
                              <ActionButton icon={CalendarClock} label="Reschedule" tone="teal" busy={busyId === a.id} onClick={() => { setRescheduling(a); setShowNewForm(false); }} />
                              <ActionButton icon={UserX} label="No-show" tone="amber" busy={busyId === a.id} onClick={() => runAction(a.id, () => markNoShow(a.id), 'Marked as no-show.')} />
                              <ActionButton icon={Slash} label="Cancel" tone="coral" busy={busyId === a.id} onClick={() => runAction(a.id, () => cancelAppointment(a.id, 'CLINIC_CLOSED'), 'Appointment cancelled.')} />
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function ActionButton({
  icon: Icon, label, onClick, busy, tone = 'teal',
}: { icon: typeof Check; label: string; onClick: () => void; busy: boolean; tone?: 'teal' | 'coral' | 'amber' }) {
  const toneClass = { teal: 'text-teal hover:bg-teal/10', coral: 'text-coral hover:bg-coral/10', amber: 'text-amber hover:bg-amber/10' }[tone];
  return (
    <button onClick={onClick} disabled={busy} className={`btn-press flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium focus-ring disabled:opacity-40 ${toneClass}`}>
      <Icon size={12} /> {label}
    </button>
  );
}

function NewAppointmentForm({ onDone }: { onDone: () => void }) {
  const [patientQuery, setPatientQuery] = useState('');
  const [patientResults, setPatientResults] = useState<PatientResponse[]>([]);
  const [patient, setPatient] = useState<PatientResponse | null>(null);

  const [doctors, setDoctors] = useState<PractitionerResponse[]>([]);
  const [practitionerId, setPractitionerId] = useState('');
  const [services, setServices] = useState<ServiceResponse[]>([]);
  const [serviceId, setServiceId] = useState('');
  const [source, setSource] = useState<'PHONE' | 'FRONT_DESK'>('FRONT_DESK');
  const [reason, setReason] = useState('');

  const [slots, setSlots] = useState<SlotResponse[]>([]);
  const [selectedSlot, setSelectedSlot] = useState<SlotResponse | null>(null);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  useEffect(() => { searchPractitioners().then(setDoctors).catch(() => {}); }, []);
  useEffect(() => { listServices().then(setServices).catch(() => {}); }, []);

  const selectedDoctor = doctors.find((d) => d.id === practitionerId);
  const clinicId = selectedDoctor?.clinicIds[0];

  async function searchForPatient(e: FormEvent) {
    e.preventDefault();
    try {
      setPatientResults(await searchPatients(patientQuery));
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Search failed.', 'error');
    }
  }

  async function loadAvailability() {
    if (!practitionerId || !clinicId) return;
    setSlots([]);
    setSelectedSlot(null);
    setSlotsLoading(true);
    const from = new Date().toISOString().slice(0, 10);
    const to = new Date(Date.now() + 14 * 86400_000).toISOString().slice(0, 10);
    try {
      const res = await getAvailability({ practitionerId, serviceId: serviceId || undefined, clinicId, from, to });
      setSlots(res.slots);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not load availability.', 'error');
    } finally {
      setSlotsLoading(false);
    }
  }

  useEffect(() => { if (practitionerId && serviceId) loadAvailability(); }, [practitionerId, serviceId]);

  async function onSelectSlot(slot: SlotResponse) {
    if (selectedSlot) releaseSlot(selectedSlot.slotId).catch(() => {});
    setSelectedSlot(slot);
    try {
      await holdSlot(slot.slotId);
    } catch {
      toast.show('That slot was just taken \u2014 please pick another.', 'error');
      setSelectedSlot(null);
      loadAvailability();
    }
  }

  async function onSubmit() {
    if (!patient || !practitionerId || !clinicId || !serviceId || !selectedSlot) return;
    setLoading(true);
    try {
      const types = await listAppointmentTypes();
      const defaultType = types.find((t) => t.code === 'GENERAL') ?? types[0];
      if (!defaultType) throw new Error('No appointment type is configured yet.');

      await staffBookAppointment({
        patientId: patient.id, practitionerId, clinicId, serviceId,
        appointmentTypeId: defaultType.id, slotId: selectedSlot.slotId,
        reason: reason || undefined, source,
      });
      onDone();
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Booking failed.', 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="panel p-5 mb-6 space-y-5 animate-scale-in">
      <div>
        <p className="text-xs font-medium text-ink/60 mb-1.5">Patient</p>
        {patient ? (
          <div className="flex items-center justify-between rounded-lg border border-teal bg-teal/5 px-3 py-2">
            <span className="text-sm text-ink">{patient.firstName} {patient.lastName}</span>
            <button onClick={() => setPatient(null)} className="text-xs text-ink/40 hover:text-coral focus-ring">Change</button>
          </div>
        ) : (
          <>
            <form onSubmit={searchForPatient} className="flex gap-2 mb-2">
              <input
                value={patientQuery}
                onChange={(e) => setPatientQuery(e.target.value)}
                placeholder="Search by name, mobile, or email..."
                className="flex-1 rounded-lg border border-line bg-paper px-3.5 py-2 text-sm focus-ring"
              />
              <button type="submit" className="btn-press rounded-lg bg-teal px-4 py-2 text-sm text-white font-medium hover:bg-teal-dark focus-ring">Search</button>
            </form>
            {patientResults.length > 0 && (
              <div className="border border-line rounded-lg divide-y divide-line/60 max-h-32 overflow-y-auto">
                {patientResults.map((p) => (
                  <button key={p.id} onClick={() => { setPatient(p); setPatientResults([]); }} className="w-full text-left px-3 py-2 text-sm hover:bg-paper focus-ring">
                    {p.firstName} {p.lastName} <span className="text-ink/40 text-xs">{p.mobileNumber ?? p.email}</span>
                  </button>
                ))}
              </div>
            )}
          </>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <label className="block">
          <span className="block text-xs font-medium text-ink/60 mb-1.5">Doctor</span>
          <select value={practitionerId} onChange={(e) => setPractitionerId(e.target.value)} className="w-full rounded-lg border border-line bg-paper px-3.5 py-2.5 text-sm focus-ring">
            <option value="">Select...</option>
            {doctors.map((d) => <option key={d.id} value={d.id}>Dr. {d.firstName} {d.lastName}</option>)}
          </select>
        </label>
        <label className="block">
          <span className="block text-xs font-medium text-ink/60 mb-1.5">Service</span>
          <select value={serviceId} onChange={(e) => setServiceId(e.target.value)} className="w-full rounded-lg border border-line bg-paper px-3.5 py-2.5 text-sm focus-ring">
            <option value="">Select...</option>
            {services.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </label>
      </div>

      {practitionerId && serviceId && (
        <div>
          <p className="text-xs font-medium text-ink/60 mb-2">Available times</p>
          <SlotPicker slots={slots} selectedSlotId={selectedSlot?.slotId ?? null} onSelect={onSelectSlot} loading={slotsLoading} />
        </div>
      )}

      <div className="grid grid-cols-2 gap-4">
        <label className="block">
          <span className="block text-xs font-medium text-ink/60 mb-1.5">Booking source</span>
          <select value={source} onChange={(e) => setSource(e.target.value as 'PHONE' | 'FRONT_DESK')} className="w-full rounded-lg border border-line bg-paper px-3.5 py-2.5 text-sm focus-ring">
            <option value="FRONT_DESK">Front desk</option>
            <option value="PHONE">Phone</option>
          </select>
        </label>
        <label className="block">
          <span className="block text-xs font-medium text-ink/60 mb-1.5">Reason (optional)</span>
          <input value={reason} onChange={(e) => setReason(e.target.value)} className="w-full rounded-lg border border-line bg-paper px-3.5 py-2.5 text-sm focus-ring" />
        </label>
      </div>

      <button
        onClick={onSubmit}
        disabled={loading || !patient || !selectedSlot}
        className="btn-press flex items-center gap-2 rounded-lg bg-teal px-5 py-2.5 text-sm font-medium text-white hover:bg-teal-dark focus-ring disabled:opacity-50"
      >
        {loading && <Spinner size={15} />}
        {loading ? 'Booking...' : 'Book appointment'}
      </button>
    </div>
  );
}

function RescheduleForm({
  appointment, onDone, onCancel,
}: { appointment: AppointmentResponse; onDone: () => void; onCancel: () => void }) {
  const [clinics, setClinics] = useState<ClinicResponse[]>([]);
  const [slots, setSlots] = useState<SlotResponse[]>([]);
  const [selectedSlot, setSelectedSlot] = useState<SlotResponse | null>(null);
  const [slotsLoading, setSlotsLoading] = useState(true);
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  useEffect(() => { listClinics().then(setClinics).catch(() => {}); }, []);

  useEffect(() => {
    setSlotsLoading(true);
    const from = new Date().toISOString().slice(0, 10);
    const to = new Date(Date.now() + 14 * 86400_000).toISOString().slice(0, 10);
    getAvailability({ practitionerId: appointment.practitionerId, clinicId: appointment.clinicId, from, to })
      .then((res) => setSlots(res.slots))
      .catch((err) => toast.show(err instanceof ApiError ? err.message : 'Could not load availability.', 'error'))
      .finally(() => setSlotsLoading(false));
  }, [appointment]);

  async function onSelectSlot(slot: SlotResponse) {
    if (selectedSlot) releaseSlot(selectedSlot.slotId).catch(() => {});
    setSelectedSlot(slot);
    try {
      await holdSlot(slot.slotId);
    } catch {
      toast.show('That slot was just taken \u2014 please pick another.', 'error');
      setSelectedSlot(null);
    }
  }

  async function onConfirm() {
    if (!selectedSlot) return;
    setLoading(true);
    try {
      await rescheduleAppointment(appointment.id, selectedSlot.slotId);
      onDone();
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not reschedule this appointment.', 'error');
    } finally {
      setLoading(false);
    }
  }

  const currentClinicName = clinics.find((c) => c.id === appointment.clinicId)?.name;

  return (
    <div className="panel p-5 mb-6 space-y-4 animate-scale-in">
      <div>
        <h2 className="text-sm font-medium text-ink mb-0.5 flex items-center gap-1.5">
          <CalendarClock size={15} className="text-teal" /> Reschedule appointment
        </h2>
        <p className="text-xs text-ink/50 tabular-nums">
          Currently {new Date(appointment.scheduledStart).toLocaleString(undefined, { weekday: 'long', month: 'long', day: 'numeric', hour: 'numeric', minute: '2-digit' })}
          {currentClinicName && ` at ${currentClinicName}`}
        </p>
      </div>

      <SlotPicker slots={slots} selectedSlotId={selectedSlot?.slotId ?? null} onSelect={onSelectSlot} loading={slotsLoading} />

      <div className="flex gap-2">
        <button
          onClick={onConfirm}
          disabled={loading || !selectedSlot}
          className="btn-press flex items-center gap-2 rounded-lg bg-teal px-5 py-2.5 text-sm font-medium text-white hover:bg-teal-dark focus-ring disabled:opacity-50"
        >
          {loading && <Spinner size={15} />}
          {loading ? 'Rescheduling...' : 'Confirm new time'}
        </button>
        <button onClick={onCancel} className="rounded-lg px-4 py-2.5 text-sm text-ink/60 hover:text-ink focus-ring">Cancel</button>
      </div>
    </div>
  );
}
