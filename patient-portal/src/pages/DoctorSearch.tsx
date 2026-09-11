import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, Search, Stethoscope, UserRound } from 'lucide-react';
import { searchDoctors } from '../api/booking';
import { ApiError } from '../api/client';
import type { PractitionerResponse } from '../api/types';
import { CardSkeleton } from '../components/Skeleton';
import { EmptyState } from '../components/EmptyState';
import { useToast } from '../components/Toast';

export default function DoctorSearch() {
  const [name, setName] = useState('');
  const [doctors, setDoctors] = useState<PractitionerResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const toast = useToast();

  async function search(query: string) {
    setLoading(true);
    try {
      setDoctors(await searchDoctors({ name: query || undefined }));
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not load doctors right now.', 'error');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { search(''); }, []);

  return (
    <div className="animate-page">
      <h1 className="font-display text-2xl text-ink mb-1">Find a doctor</h1>
      <p className="text-sm text-ink/60 mb-6">Search by name or browse everyone currently seeing patients.</p>

      <form onSubmit={(e) => { e.preventDefault(); search(name); }} className="mb-8 relative">
        <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-ink/30" />
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Doctor name…"
          className="w-full rounded-lg border border-line bg-paper pl-10 pr-24 py-2.5 text-sm focus-ring"
        />
        <button
          type="submit"
          className="btn-press absolute right-1.5 top-1/2 -translate-y-1/2 rounded-md bg-teal px-3.5 py-1.5 text-xs text-white font-medium hover:bg-teal-dark focus-ring"
        >
          Search
        </button>
      </form>

      {loading && (
        <div className="space-y-3">
          <CardSkeleton /><CardSkeleton /><CardSkeleton />
        </div>
      )}

      {!loading && doctors.length === 0 && (
        <EmptyState icon={UserRound} title="No doctors match that search" description="Try a different name or clear your search." />
      )}

      <div className="space-y-3">
        {doctors.map((d, i) => (
          <Link
            key={d.id}
            to={`/doctors/${d.id}`}
            style={{ animationDelay: `${i * 40}ms` }}
            className="animate-scale-in card-interactive flex items-center gap-4 rounded-card border border-line bg-paper p-4 hover:border-teal focus-ring"
          >
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-teal/10 text-teal">
              <Stethoscope size={18} />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-baseline justify-between gap-2">
                <h2 className="font-medium text-ink truncate">
                  Dr. {d.firstName} {d.lastName}
                  {d.credentials && <span className="text-ink/40 font-normal text-sm"> · {d.credentials}</span>}
                </h2>
                {d.defaultConsultationFee != null && (
                  <span className="font-mono text-xs text-ink/50 shrink-0 tabular-nums">₱{d.defaultConsultationFee.toLocaleString()}</span>
                )}
              </div>
              <p className="text-sm text-ink/60 mt-0.5 truncate">
                {d.specialties.join(', ') || 'General practice'} · {d.clinics.join(', ')}
              </p>
            </div>
            <ChevronRight size={18} className="text-ink/20 shrink-0" />
          </Link>
        ))}
      </div>
    </div>
  );
}
