import { Link } from 'react-router-dom';

export function Footer() {
  return (
    <footer className="border-t border-line mt-16">
      <div className="mx-auto max-w-3xl px-5 py-6 flex flex-wrap items-center justify-between gap-3 text-xs text-ink/40">
        <p>&copy; {new Date().getFullYear()} Schedul.io</p>
        <div className="flex items-center gap-4">
          <Link to="/terms" className="hover:text-ink focus-ring rounded">Terms of Service</Link>
          <Link to="/privacy" className="hover:text-ink focus-ring rounded">Privacy Policy</Link>
        </div>
      </div>
    </footer>
  );
}
