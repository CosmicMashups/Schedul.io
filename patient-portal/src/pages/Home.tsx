import { Link } from 'react-router-dom';
import { ArrowRight, CalendarCheck2, Search, ShieldCheck } from 'lucide-react';

export default function Home() {
  return (
    <div className="animate-page">
      <div className="text-center py-16">
        <p className="font-mono text-xs uppercase tracking-widest text-teal mb-4">Patient access</p>
        <h1 className="font-display text-4xl sm:text-5xl leading-tight text-ink mb-5">
          See a doctor,<br />on your schedule.
        </h1>
        <p className="text-ink/60 max-w-md mx-auto mb-8">
          Search by doctor or specialty, pick a time that works, and get a confirmation you can trust —
          no phone calls, no waiting on hold.
        </p>
        <div className="flex items-center justify-center gap-3">
          <Link to="/doctors" className="btn-press flex items-center gap-2 rounded-full bg-teal px-6 py-3 text-white text-sm font-medium hover:bg-teal-dark focus-ring shadow-sm shadow-teal/20">
            Find a doctor <ArrowRight size={16} />
          </Link>
          <Link to="/register" className="btn-press rounded-full border border-line px-6 py-3 text-ink text-sm font-medium hover:border-teal focus-ring">
            Create your record
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pb-8">
        <Feature icon={Search} title="Search & compare" desc="Browse doctors by specialty and see real open times." />
        <Feature icon={CalendarCheck2} title="Book in minutes" desc="Hold your slot, confirm details, done." />
        <Feature icon={ShieldCheck} title="Your data, protected" desc="Consent-based, privacy-first record keeping." />
      </div>
    </div>
  );
}

function Feature({ icon: Icon, title, desc }: { icon: typeof Search; title: string; desc: string }) {
  return (
    <div className="card-interactive rounded-card border border-line bg-paper p-5">
      <div className="mb-3 flex h-9 w-9 items-center justify-center rounded-full bg-teal/10 text-teal">
        <Icon size={18} strokeWidth={1.75} />
      </div>
      <p className="font-medium text-ink text-sm mb-1">{title}</p>
      <p className="text-xs text-ink/50 leading-relaxed">{desc}</p>
    </div>
  );
}
