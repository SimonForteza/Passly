import { RouterProvider } from 'react-router-dom';
import { AuthProvider } from './lib/auth/AuthContext';
import { router } from './router';
import { OrganizacionProvider } from './productoras/OrganizacionContext';

export default function App() {
  return (
    <AuthProvider>
      <OrganizacionProvider>
        <RouterProvider router={router} />
      </OrganizacionProvider>
    </AuthProvider>
  );
}
