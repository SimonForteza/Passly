import { Link, useNavigate } from 'react-router-dom';
import { useDesplegable } from '../layout/useDesplegable';
import { Icono } from './Icono';
import type { Organizacion } from './OrganizacionContext';
import type { RolEnProductora } from './types';

const NOMBRE_DE_ROL: Record<RolEnProductora, string> = {
  DUENIO: 'Dueño',
  STAFF: 'Staff',
  VALIDADOR: 'Validador',
};

function Inicial({ productora }: { productora: Organizacion }) {
  return (
    <span className="selector-productora-inicial" aria-hidden="true">
      {productora.nombreComercial.trim().charAt(0).toUpperCase() || '?'}
    </span>
  );
}

/** Selector de productora arriba a la izquierda, al estilo del workspace de Linear o Notion. */
export function SelectorDeProductora({
  actual,
  organizaciones,
}: {
  actual: Organizacion;
  organizaciones: Organizacion[];
}) {
  const { abierto, alternar, cerrar, contenedor, disparador } = useDesplegable();
  const navigate = useNavigate();

  function elegir(id: number) {
    cerrar();
    if (id !== actual.id) navigate('/productoras/' + id + '/gestion');
  }

  return (
    <div className="selector-productora" ref={contenedor}>
      <button
        ref={disparador}
        type="button"
        className="selector-productora-disparador"
        aria-haspopup="menu"
        aria-expanded={abierto}
        aria-controls="lista-de-productoras"
        aria-label={`Productora: ${actual.nombreComercial}. Cambiar de productora`}
        onClick={alternar}
      >
        <Inicial productora={actual} />
        <span className="selector-productora-textos">
          <strong>{actual.nombreComercial}</strong>
          <span>{NOMBRE_DE_ROL[actual.rol]}</span>
        </span>
        <Icono nombre="chevrones" tamanio={16} />
      </button>

      {abierto ? (
        <div id="lista-de-productoras" className="selector-productora-menu" role="menu">
          <p className="selector-productora-titulo">Tus productoras</p>
          {organizaciones.map((p) => (
            <button
              key={p.id}
              type="button"
              role="menuitemradio"
              aria-checked={p.id === actual.id}
              className="selector-productora-opcion"
              onClick={() => elegir(p.id)}
            >
              <Inicial productora={p} />
              <span className="selector-productora-textos">
                <strong>{p.nombreComercial}</strong>
                <span>{NOMBRE_DE_ROL[p.rol]}</span>
              </span>
              {p.id === actual.id ? <Icono nombre="check" tamanio={16} /> : null}
            </button>
          ))}
          <hr className="cuenta-menu-separador" />
          <Link role="menuitem" className="cuenta-menu-item" to="/productoras/nueva" onClick={cerrar}>
            + Crear productora
          </Link>
          <Link role="menuitem" className="cuenta-menu-item" to="/productoras" onClick={cerrar}>
            Ver todas mis productoras
          </Link>
        </div>
      ) : null}
    </div>
  );
}
