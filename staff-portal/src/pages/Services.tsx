import { FormEvent, useEffect, useState } from 'react';
import { ClipboardList, PlusCircle } from 'lucide-react';
import { createService, listClinics, listServices } from '../api/clinic';
import { ApiError } from '../api/client';
import type { ClinicResponse, ServiceResponse } from '../api/types';
import { Field, SelectField } from '../components/Field';
import { EmptyState } from '../components/EmptyState';
import { Skeleton } from '../components/Skeleton';
import { useToast } from '../components/Toast';

export default function Services() {
  const [services, setServices] = useState<ServiceResponse[]>([]);
  const [clinics, setClinics] = useState<ClinicResponse[]>([]);
  // Fixed: without a loading flag, the empty state briefly flashed "No services yet" on every
  // load, before the initial fetch had a chance to resolve.
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const toast = useToast();

  async function load() {
    setLoading(true);
    try {
      const [svc, cls] = await Promise.all([listServices(), listClinics()]);
      setServices(svc);
      setClinics(cls);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not load services.', 'error');
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => { load(); }, []);

  return (
    <div className="max-w-2xl animate-page">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <ClipboardList size={20} className="text-teal" />
          <h1 className="text-xl font-semibold text-ink">Services</h1>
        </div>
        <button onClick={() => setShowForm((v) => !v)} className="btn-press flex items-center gap-1.5 rounded-lg border border-line px-3.5 py-2 text-sm font-medium hover:border-teal focus-ring">
          <PlusCircle size={15} /> {showForm ? 'Cancel' : 'New service'}
        </button>
      </div>

      {showForm ? (
        <ServiceForm clinics={clinics} onDone={() => { setShowForm(false); load(); toast.show('Service created.'); }} />
      ) : loading && services.length === 0 ? (
        <div className="panel p-4 space-y-3">
          {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-11" />)}
        </div>
      ) : services.length === 0 ? (
        <div className="panel"><EmptyState icon={ClipboardList} title="No services yet" /></div>
      ) : (
        <div className="panel divide-y divide-line/60">
          {services.map((s) => (
            <div key={s.id} className="px-4 py-3 flex items-center justify-between">
              <div>
                <p className="font-medium text-ink">{s.name}</p>
                <p className="text-xs text-ink/50 mt-0.5 tabular-nums">{s.durationMinutes} min · {s.consultationMode} · {s.clinics.join(', ')}</p>
              </div>
              {s.price != null && <span className="font-mono text-sm text-ink/60 tabular-nums">₱{s.price.toLocaleString()}</span>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function ServiceForm({ clinics, onDone }: { clinics: ClinicResponse[]; onDone: () => void }) {
  const [name, setName] = useState('');
  const [durationMinutes, setDurationMinutes] = useState('30');
  const [bufferMinutes, setBufferMinutes] = useState('5');
  const [price, setPrice] = useState('');
  const [mode, setMode] = useState('IN_PERSON');
  const [clinicId, setClinicId] = useState(clinics[0]?.id ?? '');
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      await createService({
        name,
        durationMinutes: Number(durationMinutes),
        bufferMinutes: Number(bufferMinutes),
        price: price ? Number(price) : undefined,
        consultationMode: mode as 'IN_PERSON' | 'TELECONSULT' | 'EITHER',
        clinicIds: [clinicId],
      });
      onDone();
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not create this service.', 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="panel p-5 space-y-4 animate-scale-in">
      <Field label="Name" value={name} onChange={setName} placeholder="General Consultation" />
      <div className="grid grid-cols-3 gap-4">
        <Field label="Duration (min)" type="number" value={durationMinutes} onChange={setDurationMinutes} />
        <Field label="Buffer (min)" type="number" value={bufferMinutes} onChange={setBufferMinutes} />
        <Field label="Price (₱)" type="number" value={price} onChange={setPrice} required={false} />
      </div>
      <SelectField
        label="Consultation mode"
        value={mode}
        onChange={setMode}
        options={[
          { value: 'IN_PERSON', label: 'In-person' },
          { value: 'TELECONSULT', label: 'Teleconsult' },
          { value: 'EITHER', label: 'Either' },
        ]}
      />
      <label className="block">
        <span className="block text-xs font-medium text-ink/60 mb-1.5">Clinic</span>
        <select value={clinicId} onChange={(e) => setClinicId(e.target.value)} required className="w-full rounded-lg border border-line bg-paper px-3.5 py-2.5 text-sm focus-ring">
          {clinics.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </label>
      <button type="submit" disabled={loading} className="btn-press rounded-lg bg-teal px-5 py-2.5 text-sm font-medium text-white hover:bg-teal-dark focus-ring disabled:opacity-50">
        {loading ? 'Saving…' : 'Create service'}
      </button>
    </form>
  );
}
