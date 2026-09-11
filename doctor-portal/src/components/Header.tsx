import { useNavigate } from 'react-router-dom';
import { LogOut } from 'lucide-react';
import { clearSession } from '../api/client';

// Brand mark: a calendar with a pulse cross — replaces the placeholder Stethoscope-as-logo.
function BrandMark({ size = 14 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="3" y="5" width="18" height="16" rx="4" stroke="currentColor" strokeWidth="2" />
      <path d="M8 3v4M16 3v4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M12 10.5v7M8.5 14h7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

export function Header() {
  const navigate = useNavigate();
  return (
    <header className="border-b border-slate-line">
      <div className="mx-auto max-w-2xl px-5 py-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="flex h-7 w-7 items-center justify-center rounded-full bg-teal/15 text-teal">
            <BrandMark size={14} />
          </span>
          <p className="text-xs font-mono uppercase tracking-widest text-teal">Doctor Portal</p>
        </div>
        <button
          onClick={() => { clearSession(); navigate('/login'); }}
          className="flex items-center gap-1.5 text-xs text-ink/40 hover:text-coral focus-ring transition-colors"
        >
          <LogOut size={13} /> Sign out
        </button>
      </div>
    </header>
  );
}
