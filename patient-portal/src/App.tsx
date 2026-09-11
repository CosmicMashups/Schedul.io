import { Route, Routes } from 'react-router-dom';
import { Nav } from './components/Nav';
import { Footer } from './components/Footer';
import Home from './pages/Home';
import Register from './pages/Register';
import Login from './pages/Login';
import DoctorSearch from './pages/DoctorSearch';
import DoctorProfile from './pages/DoctorProfile';
import BookingConfirmation from './pages/BookingConfirmation';
import MyAppointments from './pages/MyAppointments';
import AppointmentDetail from './pages/AppointmentDetail';
import Terms from './pages/Terms';
import Privacy from './pages/Privacy';

// Home manages its own full-width hero/CTA sections plus its own max-w-6xl content — it
// deliberately does NOT sit inside the max-w-3xl reading-width container every other page
// uses, so it needs no "full-bleed breakout" trick (an earlier version tried to escape the
// container with a viewport-width hack, which drifted out of sync with the real layout on
// some screens). Every other route keeps the narrow container as before.
function Main({ children }: { children: React.ReactNode }) {
  return <main className="mx-auto max-w-3xl px-5 py-10">{children}</main>;
}

export default function App() {
  return (
    <div className="min-h-screen flex flex-col">
      <Nav />
      <div className="flex-1">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/register" element={<Main><Register /></Main>} />
          <Route path="/login" element={<Main><Login /></Main>} />
          <Route path="/doctors" element={<Main><DoctorSearch /></Main>} />
          <Route path="/doctors/:id" element={<Main><DoctorProfile /></Main>} />
          <Route path="/appointments" element={<Main><MyAppointments /></Main>} />
          <Route path="/appointments/:id" element={<Main><AppointmentDetail /></Main>} />
          <Route path="/appointments/:id/confirmed" element={<Main><BookingConfirmation /></Main>} />
          <Route path="/terms" element={<Main><Terms /></Main>} />
          <Route path="/privacy" element={<Main><Privacy /></Main>} />
        </Routes>
      </div>
      <Footer />
    </div>
  );
}
