import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth/AuthContext';
import { useOrganizaciones } from '../productoras/OrganizacionContext';
import { vaciarCarrito } from '../ventas/api';
import { actualizarCarrito, useCantidadCarrito } from '../ventas/carritoEstado';

export function Header() {
  const { usuario, estaAutenticado, cerrarSesion } = useAuth();
  const { organizaciones } = useOrganizaciones();
  const cantidad = useCantidadCarrito();
  const navigate = useNavigate();
  const [menu, setMenu] = useState(false);
  const [saliendo, setSaliendo] = useState(false);
  async function salir() {
    setSaliendo(true);
    await vaciarCarrito().catch(() => undefined);
    actualizarCarrito(null);
    cerrarSesion();
    setMenu(false);
    setSaliendo(false);
    navigate('/');
  }
  return (
    <header className="cabecera">
      <Link className="marca" to="/" aria-label="Passly, ir a eventos">
        Passly
      </Link>
      <button
        className="menu-movil boton boton-secundario"
        aria-controls="navegacion-principal"
        aria-expanded={menu}
        onClick={() => setMenu(!menu)}
      >
        {menu ? 'Cerrar menú' : 'Menú'}
      </button>
      <nav
        id="navegacion-principal"
        className={'cabecera-nav' + (menu ? ' abierto' : '')}
        aria-label="Navegación principal"
        onClick={(e) => {
          if ((e.target as HTMLElement).closest('a')) setMenu(false);
        }}
      >
        <NavLink to="/" end>
          Eventos
        </NavLink>
        <NavLink to={estaAutenticado ? '/mis-ordenes' : '/orden-invitado'}>
          Mis entradas
        </NavLink>
        <NavLink to="/carrito">
          Carrito{cantidad > 0 ? ' (' + cantidad + ')' : ''}
        </NavLink>
        {organizaciones.length ? (
          <>
            <NavLink to="/productoras">Panel de organización</NavLink>
            <NavLink to="/validar">Validar entradas</NavLink>
          </>
        ) : (
          <NavLink to="/organizar">Organizar eventos</NavLink>
        )}
        {usuario ? (
          <>
            <NavLink to="/perfil" className="cuenta-cabecera">
              {usuario.nombre}
            </NavLink>
            <button
              className="boton boton-secundario salir-cabecera"
              disabled={saliendo}
              onClick={salir}
            >
              {saliendo ? 'Saliendo…' : 'Salir'}
            </button>
          </>
        ) : (
          <Link className="boton boton-primario" to="/login">
            Ingresar
          </Link>
        )}
      </nav>
    </header>
  );
}
