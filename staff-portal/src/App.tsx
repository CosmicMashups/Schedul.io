import { Navigate, Route, Routes } from 'react-router-dom';
import { Sidebar } from './components/Sidebar';
import { getToken } from './api/client';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Calendar from './pages/Calendar';
import Appointments from './pages/Appointments';
import CheckIn from './pages/CheckIn';
import Queue from './pages/Queue';
import Patients from './pages/Patients';
import Practitioners from './pages/Practitioners';
import Services from './pages/Services';
import Schedules from './pages/Schedules';
import Reports from './pages/Reports';
import Terms from './pages/Terms';
import Privacy from './pages/Privacy';

// Terms/Privacy are reachable whether signed in or not (linked from the Login screen), so
// they sit outside RequireAuth with their own minimal centered layout rather than the
// authenticated Sidebar shell.
function Standalone({ children }: { children: React.ReactNode }) {
  return <div className="min-h-screen bg-paper p-8 sm:p-12">{children}</div>;
}

function RequireAuth({ children }: { children: JSX.Element }) {
  return getToken() ? children : <Navigate to="/login" replace />;
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
            <div className="flex">
              <Sidebar />
              <div className="flex-1 p-8 max-w-6xl">
                <Routes>
                  <Route path="/" element={<Navigate to="/dashboard" replace />} />
                  <Route path="/dashboard" element={<Dashboard />} />
                  <Route path="/calendar" element={<Calendar />} />
                  <Route path="/appointments" element={<Appointments />} />
                  <Route path="/check-in" element={<CheckIn />} />
                  <Route path="/queue" element={<Queue />} />
                  <Route path="/patients" element={<Patients />} />
                  <Route path="/practitioners" element={<Practitioners />} />
                  <Route path="/services" element={<Services />} />
                  <Route path="/schedules" element={<Schedules />} />
                  <Route path="/reports" element={<Reports />} />
                </Routes>
              </div>
            </div>
          </RequireAuth>
        }
      />
      {!authed && <Route path="*" element={<Navigate to="/login" replace />} />}
    </Routes>
  );
}
