import { Link } from 'react-router-dom';
export function OrganizarPage() {
  return (
    <section className="stack-grande pagina-organizar">
      <p className="eyebrow">Organizá con Passly</p>
      <h2>Tu próximo evento empieza acá</h2>
      <p>
        Creá tu productora, prepará tus eventos e invitá a las personas que van
        a ayudarte.
      </p>
      <ol className="pasos-organizar">
        <li>
          <strong>Creá tu productora</strong>
          <span>
            Elegí un nombre comercial. El CUIT, la descripción y el logo son
            opcionales.
          </span>
        </li>
        <li>
          <strong>Prepará tu evento</strong>
          <span>
            Guardalo como borrador y revisá la fecha, el lugar y las entradas
            antes de publicarlo.
          </span>
        </li>
        <li>
          <strong>Sumá a tu equipo</strong>
          <span>
            Invitá staff para gestionar eventos o validadores para controlar el
            ingreso.
          </span>
        </li>
      </ol>
      <div>
        <Link className="boton boton-primario" to="/productoras/nueva">
          Crear mi productora
        </Link>
      </div>
      <p className="texto-secundario">
        Primero vas a ingresar o crear tu cuenta. Después continuás con el
        formulario de tu productora.
      </p>
    </section>
  );
}
