import { Navigate, Route, Routes } from 'react-router-dom';
import { getToken } from './api/client';
import Login from './pages/Login';
import Today from './pages/Today';
import { Header } from './components/Header';

function RequireAuth({ children }: { children: JSX.Element }) {
  return getToken() ? children : <Navigate to="/login" replace />;
}

export default function App() {
  const authed = !!getToken();
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
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
            </div>
          </RequireAuth>
        }
      />
      {!authed && <Route path="*" element={<Navigate to="/login" replace />} />}
    </Routes>
  );
}
