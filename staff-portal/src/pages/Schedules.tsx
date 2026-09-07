import { FormEvent, useEffect, useState } from 'react';
import { listClinics, searchPractitioners } from '../api/clinic';
import { createScheduleRule, listScheduleRules } from '../api/scheduling';
import { ApiError } from '../api/client';
import type { ClinicResponse, PractitionerResponse, ScheduleRuleResponse } from '../api/types';
import { Field, SelectField } from '../components/Field';
import { EmptyState } from '../components/EmptyState';
import { useToast } from '../components/Toast';
import { CalendarRange, PlusCircle } from 'lucide-react';

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export default function Schedules() {
  const [doctors, setDoctors] = useState<PractitionerResponse[]>([]);
  const [clinics, setClinics] = useState<ClinicResponse[]>([]);
  const [practitionerId, setPractitionerId] = useState('');
  const [rules, setRules] = useState<ScheduleRuleResponse[]>([]);
  const toast = useToast();
  const [showForm, setShowForm] = useState(false);

  useEffect(() => {
    Promise.all([searchPractitioners(), listClinics()]).then(([docs, cls]) => {
      setDoctors(docs);
      setClinics(cls);
      if (docs[0]) setPractitionerId(docs[0].id);
    }).catch((err) => toast.show(err instanceof ApiError ? err.message : 'Could not load doctors.', 'error'));
  }, []);

  useEffect(() => {
    if (practitionerId) listScheduleRules(practitionerId).then(setRules).catch(() => {});
  }, [practitionerId]);

  return (
    <div className="max-w-2xl animate-page">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <CalendarRange size={20} className="text-teal" />
          <h1 className="text-xl font-semibold text-ink">Schedules</h1>
        </div>
        <button onClick={() => setShowForm((v) => !v)} className="btn-press flex items-center gap-1.5 rounded-lg border border-line px-3.5 py-2 text-sm font-medium hover:border-teal focus-ring">
          <PlusCircle size={15} /> {showForm ? 'Cancel' : 'Add availability'}
        </button>
      </div>

      <label className="block mb-6">
        <span className="block text-xs font-medium text-ink/60 mb-1.5">Doctor</span>
        <select value={practitionerId} onChange={(e) => setPractitionerId(e.target.value)} className="w-full max-w-xs rounded-lg border border-line bg-panel px-3.5 py-2.5 text-sm focus-ring">
          {doctors.map((d) => <option key={d.id} value={d.id}>Dr. {d.firstName} {d.lastName}</option>)}
        </select>
      </label>

      {rules.length === 0 ? (
        <div className="panel mb-6"><EmptyState icon={CalendarRange} title="No recurring availability set yet" /></div>
      ) : (
        <div className="panel divide-y divide-line/60 mb-6">
          {rules.map((r) => (
            <div key={r.id} className="px-4 py-3 flex items-center justify-between text-sm">
              <span className="font-medium text-ink">{r.dayOfWeek}</span>
              <span className="text-ink/60">{r.startTime}–{r.endTime}</span>
              <span className="font-mono text-xs text-ink/40">{r.slotDurationMinutes}min slots</span>
            </div>
          ))}
        </div>
      )}

      {showForm && practitionerId && (
        <NewRuleForm
          practitionerId={practitionerId}
          clinics={clinics}
          onCreated={(r) => { setRules((prev) => [...prev, r]); setShowForm(false); toast.show('Availability added.'); }}
        />
      )}
    </div>
  );
}

function NewRuleForm({
  practitionerId, clinics, onCreated,
}: { practitionerId: string; clinics: ClinicResponse[]; onCreated: (r: ScheduleRuleResponse) => void }) {
  const [clinicId, setClinicId] = useState(clinics[0]?.id ?? '');
  const [dayOfWeek, setDayOfWeek] = useState('');
  const [startTime, setStartTime] = useState('08:00');
  const [endTime, setEndTime] = useState('12:00');
  const [effectiveFrom, setEffectiveFrom] = useState(new Date().toISOString().slice(0, 10));
  const [slotDurationMinutes, setSlotDurationMinutes] = useState('30');
  const [bufferMinutes, setBufferMinutes] = useState('0');
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      const rule = await createScheduleRule({
        practitionerId, clinicId, dayOfWeek, startTime, endTime, effectiveFrom,
        slotDurationMinutes: Number(slotDurationMinutes),
        bufferMinutes: Number(bufferMinutes),
      });
      onCreated(rule);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not create this schedule rule.', 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="panel p-5 space-y-4 animate-scale-in">
      <h2 className="text-sm font-medium text-ink">Add recurring availability</h2>
      <div className="grid grid-cols-2 gap-4">
        <SelectField label="Day" value={dayOfWeek} onChange={setDayOfWeek} options={DAYS.map((d) => ({ value: d, label: d.charAt(0) + d.slice(1).toLowerCase() }))} />
        <label className="block">
          <span className="block text-xs font-medium text-ink/60 mb-1.5">Clinic</span>
          <select value={clinicId} onChange={(e) => setClinicId(e.target.value)} required className="w-full rounded-lg border border-line bg-paper px-3.5 py-2.5 text-sm focus-ring">
            {clinics.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <Field label="Start time" type="time" value={startTime} onChange={setStartTime} />
        <Field label="End time" type="time" value={endTime} onChange={setEndTime} />
      </div>
      <div className="grid grid-cols-3 gap-4">
        <Field label="Effective from" type="date" value={effectiveFrom} onChange={setEffectiveFrom} />
        <Field label="Slot length (min)" type="number" value={slotDurationMinutes} onChange={setSlotDurationMinutes} />
        <Field label="Buffer (min)" type="number" value={bufferMinutes} onChange={setBufferMinutes} />
      </div>
      <button type="submit" disabled={loading} className="btn-press rounded-lg bg-teal px-5 py-2.5 text-sm font-medium text-white hover:bg-teal-dark focus-ring disabled:opacity-50">
        {loading ? 'Saving…' : 'Add availability'}
      </button>
    </form>
  );
}
