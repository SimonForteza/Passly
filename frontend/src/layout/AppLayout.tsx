import { Outlet, useLocation } from 'react-router-dom';
import { Header } from './Header';

export function AppLayout() {
  const location = useLocation();
  return (
    <div
      style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}
    >
      <Header />
      <main
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          maxWidth: '1100px',
          margin: '0 auto',
          padding: '2rem 1.5rem',
          width: '100%',
        }}
      >
        <Outlet key={location.pathname} />
      </main>
    </div>
  );
}
