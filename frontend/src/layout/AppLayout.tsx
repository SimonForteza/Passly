import { Outlet } from 'react-router-dom';
import { Header } from './Header';

export function AppLayout() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
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
        <Outlet />
      </main>
    </div>
  );
}
