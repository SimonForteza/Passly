import { useState, type ReactNode } from 'react';
import {
  Link,
  NavLink,
  Outlet,
  useLocation,
  useOutletContext,
  useParams,
} from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { MenuDeCuenta } from '../layout/MenuDeCuenta';
import { useSalir } from '../layout/useSalir';
import { useAuth } from '../lib/auth/AuthContext';
import { Icono, type NombreDeIcono } from './Icono';
import { useOrganizaciones, type Organizacion } from './OrganizacionContext';
import { SelectorDeProductora } from './SelectorDeProductora';

/**
 * Esqueleto del dashboard de gestión (PAS-46, Figma Web 20 a 24): menú lateral con el selector
 * de productora arriba a la izquierda y el contenido a la derecha. Ocupa toda la pantalla, por eso
 * vive fuera de AppLayout (router.tsx). En pantallas angostas el menú se abre como panel.
 */
export function GestionLayout() {
  const { id } = useParams();
  const { organizaciones, cargando, error } = useOrganizaciones();
  const location = useLocation();
  const productora = organizaciones.find((p) => p.id === Number(id));

  // Cambiar de página cierra el panel del menú en el celular.
  const [panelAbierto, setPanelAbierto] = useState(false);
  const [rutaAnterior, setRutaAnterior] = useState(location.pathname);
  if (rutaAnterior !== location.pathname) {
    setRutaAnterior(location.pathname);
    setPanelAbierto(false);
  }

  if (cargando)
    return (
      <EstadoDeDashboard>
        <EstadoCarga mensaje="Cargando tu productora…" />
      </EstadoDeDashboard>
    );
  if (error)
    return (
      <EstadoDeDashboard>
        <EstadoError error={error} />
      </EstadoDeDashboard>
    );
  if (!productora)
    return (
      <EstadoDeDashboard>
        <h2>Esta productora no está en tu equipo</h2>
        <p>Ingresá con una cuenta que forme parte de la productora.</p>
        <Link to="/productoras">Volver al panel de organización</Link>
      </EstadoDeDashboard>
    );

  const requiereGestion = location.pathname.includes('/eventos');
  const contenido =
    requiereGestion && !productora.puedeGestionar ? (
      <section className="stack-grande">
        <h2>Tu tarea es validar entradas</h2>
        <p>La gestión de eventos corresponde al dueño y al staff de esta productora.</p>
        <Link className="boton boton-primario" to={'/validar?productora=' + productora.id}>
          Ir a validar entradas
        </Link>
      </section>
    ) : (
      <Outlet context={productora} />
    );

  return (
    <div className={'dashboard' + (panelAbierto ? ' panel-abierto' : '')}>
      <div className="dashboard-barra">
        <SelectorDeProductora actual={productora} organizaciones={organizaciones} />
        <button
          type="button"
          className="dashboard-boton-menu"
          aria-controls="menu-de-gestion"
          aria-expanded={panelAbierto}
          aria-label={panelAbierto ? 'Cerrar menú' : 'Abrir menú'}
          onClick={() => setPanelAbierto(!panelAbierto)}
        >
          <Icono nombre={panelAbierto ? 'cerrar' : 'menu'} tamanio={20} />
        </button>
      </div>
      <div
        className="dashboard-velo"
        aria-hidden="true"
        onClick={() => setPanelAbierto(false)}
      />
      <MenuLateral productora={productora} organizaciones={organizaciones} />
      <main className="dashboard-contenido" key={location.pathname}>
        <div className="gestion">{contenido}</div>
      </main>
    </div>
  );
}

function MenuLateral({
  productora,
  organizaciones,
}: {
  productora: Organizacion;
  organizaciones: Organizacion[];
}) {
  const { usuario } = useAuth();
  const { salir, saliendo } = useSalir();
  const base = '/productoras/' + productora.id;

  return (
    <aside id="menu-de-gestion" className="dashboard-lateral" aria-label="Menú de gestión">
      <div className="dashboard-lateral-selector">
        <SelectorDeProductora actual={productora} organizaciones={organizaciones} />
      </div>
      <nav className="nav-gestion" aria-label="Secciones de la productora">
        <p className="nav-gestion-titulo">Gestión</p>
        <ItemDeMenu to={base + '/gestion'} icono="resumen">
          Resumen
        </ItemDeMenu>
        {productora.puedeGestionar ? (
          <ItemDeMenu to={base + '/eventos'} icono="eventos">
            Eventos
          </ItemDeMenu>
        ) : null}
        <ItemDeMenu to={base + '/backoffice'} icono="equipo">
          Equipo
        </ItemDeMenu>
        <Link className="nav-gestion-item" to={'/validar?productora=' + productora.id}>
          <Icono nombre="validar" />
          Validar entradas
        </Link>
        <hr className="nav-gestion-separador" />
        <Link className="nav-gestion-item" to={base}>
          <Icono nombre="externo" />
          Ver página pública
        </Link>
      </nav>
      <div className="dashboard-lateral-pie">
        <Link className="nav-gestion-item" to="/">
          <Icono nombre="volver" />
          Volver a Passly
        </Link>
        {usuario ? (
          <MenuDeCuenta usuario={usuario} saliendo={saliendo} onSalir={salir} haciaArriba />
        ) : null}
      </div>
    </aside>
  );
}

function ItemDeMenu({
  to,
  icono,
  children,
}: {
  to: string;
  icono: NombreDeIcono;
  children: ReactNode;
}) {
  return (
    <NavLink className="nav-gestion-item" to={to}>
      <Icono nombre={icono} />
      {children}
    </NavLink>
  );
}

/** Carga, error o "no es tu productora": a pantalla completa, sin el esqueleto. */
function EstadoDeDashboard({ children }: { children: ReactNode }) {
  return (
    <main className="dashboard-estado">
      <div className="stack-grande">{children}</div>
    </main>
  );
}

export function useProductoraActual() {
  return useOutletContext<Organizacion>();
}
