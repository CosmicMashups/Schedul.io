import { Link, useLocation, useNavigate } from 'react-router-dom';
import { CalendarSearch, LogOut, Stethoscope } from 'lucide-react';
import { clearSession } from '../api/client';

export function Nav() {
  const navigate = useNavigate();
  const location = useLocation();
  const patientId = localStorage.getItem('clinic.patientId');

  const isActive = (path: string) => location.pathname.startsWith(path);

  return (
    <header className="border-b border-line bg-paper/80 backdrop-blur sticky top-0 z-10">
      <div className="mx-auto max-w-3xl px-5 py-4 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2 font-display text-lg tracking-tight text-ink group">
          <span className="flex h-7 w-7 items-center justify-center rounded-full bg-teal text-white transition-transform group-hover:scale-105">
            <Stethoscope size={15} strokeWidth={2} />
          </span>
          Demo Clinic
        </Link>
        <nav className="flex items-center gap-1 text-sm">
          <Link
            to="/doctors"
            className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 transition-colors focus-ring ${
              isActive('/doctors') ? 'bg-teal/10 text-teal-dark font-medium' : 'text-ink/70 hover:text-ink hover:bg-ink/5'
            }`}
          >
            <Stethoscope size={15} className="icon-nudge" /> Find a doctor
          </Link>
          {patientId && (
            <Link
              to="/appointments"
              className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 transition-colors focus-ring ${
                isActive('/appointments') ? 'bg-teal/10 text-teal-dark font-medium' : 'text-ink/70 hover:text-ink hover:bg-ink/5'
              }`}
            >
              <CalendarSearch size={15} className="icon-nudge" /> My appointments
            </Link>
          )}
          {patientId ? (
            <button
              className="ml-1 flex items-center gap-1.5 rounded-full px-3 py-1.5 text-ink/50 hover:text-coral hover:bg-coral/5 transition-colors focus-ring"
              onClick={() => { clearSession(); localStorage.removeItem('clinic.patientId'); navigate('/'); }}
            >
              <LogOut size={15} />
            </button>
          ) : (
            <Link to="/register" className="ml-2 btn-press rounded-full bg-teal px-4 py-1.5 text-white hover:bg-teal-dark focus-ring shadow-sm shadow-teal/20">
              Get started
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
}
