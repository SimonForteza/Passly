import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth/AuthContext';
import { MenuDeCuenta } from './MenuDeCuenta';
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
      </nav>
      {/* Fuera del nav: en el celular el avatar se ve sin abrir el menú hamburguesa. */}
      <div className="cabecera-acciones">
        {usuario ? (
          <MenuDeCuenta usuario={usuario} saliendo={saliendo} onSalir={salir} />
        ) : (
          <Link className="boton boton-primario" to="/login">
            Ingresar
          </Link>
        )}
        <button
          className="menu-movil boton boton-secundario"
          aria-controls="navegacion-principal"
          aria-expanded={menu}
          onClick={() => setMenu(!menu)}
        >
          {menu ? 'Cerrar menú' : 'Menú'}
        </button>
      </div>
    </header>
  );
}
