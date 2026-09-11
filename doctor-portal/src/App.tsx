import { Navigate, Route, Routes, Link } from 'react-router-dom';
import { getToken } from './api/client';
import Login from './pages/Login';
import Today from './pages/Today';
import Terms from './pages/Terms';
import Privacy from './pages/Privacy';
import { Header } from './components/Header';

function RequireAuth({ children }: { children: JSX.Element }) {
  return getToken() ? children : <Navigate to="/login" replace />;
}

// Terms/Privacy are reachable whether signed in or not (linked from the Login screen), so
// they sit outside RequireAuth with their own minimal centered layout rather than the
// authenticated single-screen shell.
function Standalone({ children }: { children: React.ReactNode }) {
  return <div className="min-h-screen bg-slate p-8 sm:p-12">{children}</div>;
}

export default function App() {
  const authed = !!getToken();
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/terms" element={<Standalone><Terms /></Standalone>} />
      <Route path="/privacy" element={<Standalone><Privacy /></Standalone>} />
      <Route
        path="/*"
        element={
          <RequireAuth>
            <div>
              <Header />
              <main className="mx-auto max-w-2xl px-5 py-8">
                <Routes>
                  <Route path="/" element={<Today />} />
                </Routes>
              </main>
              {/* Quiet footnote, not a nav element — this app is deliberately single-screen
                  with no navigation, so legal links get one small unobtrusive line rather
                  than a full footer bar. */}
              <p className="mx-auto max-w-2xl px-5 pb-6 text-[11px] text-ink/30 flex gap-3">
                <Link to="/terms" className="hover:text-ink/60 focus-ring rounded">Terms</Link>
                <Link to="/privacy" className="hover:text-ink/60 focus-ring rounded">Privacy</Link>
              </p>
            </div>
          </RequireAuth>
        }
      />
      {!authed && <Route path="*" element={<Navigate to="/login" replace />} />}
    </Routes>
  );
}
