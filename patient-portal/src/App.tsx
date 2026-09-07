import { Route, Routes } from 'react-router-dom';
import { Nav } from './components/Nav';
import Home from './pages/Home';
import Register from './pages/Register';
import Login from './pages/Login';
import DoctorSearch from './pages/DoctorSearch';
import DoctorProfile from './pages/DoctorProfile';
import BookingConfirmation from './pages/BookingConfirmation';
import MyAppointments from './pages/MyAppointments';
import AppointmentDetail from './pages/AppointmentDetail';

export default function App() {
  return (
    <div className="min-h-screen">
      <Nav />
      <main className="mx-auto max-w-3xl px-5 py-10">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/register" element={<Register />} />
          <Route path="/login" element={<Login />} />
          <Route path="/doctors" element={<DoctorSearch />} />
          <Route path="/doctors/:id" element={<DoctorProfile />} />
          <Route path="/appointments" element={<MyAppointments />} />
          <Route path="/appointments/:id" element={<AppointmentDetail />} />
          <Route path="/appointments/:id/confirmed" element={<BookingConfirmation />} />
        </Routes>
      </main>
    </div>
  );
}
