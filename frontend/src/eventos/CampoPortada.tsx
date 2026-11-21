import { useRef, useState, type DragEvent } from 'react';
import { TIPOS_DE_PORTADA } from './portada';

function tamanio(bytes: number): string {
  return bytes >= 1024 * 1024
    ? `${(bytes / 1024 / 1024).toFixed(1).replace('.', ',')} MB`
    : `${Math.round(bytes / 1024)} KB`;
}

/**
 * Campo de portada (Figma Web 27): arrastrar o elegir, vista previa 4:5, Cambiar y Quitar.
 * Es presentacional: quien lo usa decide si el archivo se sube ya (gestión del evento) o después
 * de crear el evento (alta).
 *
 * @param vistaPrevia URL de la imagen actual (object URL), o null si no hay portada
 */
export function CampoPortada({
  vistaPrevia,
  nombreArchivo,
  pesoArchivo,
  ocupado = false,
  error,
  onElegir,
  onQuitar,
}: {
  vistaPrevia: string | null;
  nombreArchivo?: string;
  pesoArchivo?: number;
  ocupado?: boolean;
  error?: string | null;
  onElegir: (archivo: File) => void;
  onQuitar: () => void;
}) {
  const input = useRef<HTMLInputElement>(null);
  const [arrastrando, setArrastrando] = useState(false);

  function recibir(archivos: FileList | null) {
    const archivo = archivos?.[0];
    if (archivo) onElegir(archivo);
    if (input.current) input.current.value = '';
  }

  function alSoltar(e: DragEvent<HTMLButtonElement>) {
    e.preventDefault();
    setArrastrando(false);
    if (!ocupado) recibir(e.dataTransfer.files);
  }

  return (
    <div className="campo-portada panel stack-chico">
      <div>
        <h3>Portada del evento</h3>
        <p className="texto-terciario">
          Es la imagen de la tarjeta en la cartelera. Se puede cambiar aunque el evento esté publicado.
        </p>
      </div>
      <div className="campo-portada-fila">
        {vistaPrevia ? (
          <img className="campo-portada-vista" src={vistaPrevia} alt="Vista previa de la portada" />
        ) : (
          <button
            type="button"
            className={'campo-portada-zona' + (arrastrando ? ' arrastrando' : '')}
            disabled={ocupado}
            onClick={() => input.current?.click()}
            onDragOver={(e) => {
              e.preventDefault();
              setArrastrando(true);
            }}
            onDragLeave={() => setArrastrando(false)}
            onDrop={alSoltar}
          >
            <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12" />
            </svg>
            Arrastrá tu flyer
          </button>
        )}
        <div className="stack-chico">
          {vistaPrevia ? (
            <div>
              <strong>{nombreArchivo ?? 'Portada actual'}</strong>
              {pesoArchivo ? <p className="texto-secundario">{tamanio(pesoArchivo)}</p> : null}
            </div>
          ) : (
            <div>
              <strong>Todavía no tiene portada</strong>
              <p className="texto-secundario">
                Mientras tanto, la cartelera muestra el nombre del evento sobre un degradado.
              </p>
            </div>
          )}
          <div className="acciones-fila">
            <button
              type="button"
              className={'boton ' + (vistaPrevia ? 'boton-secundario' : 'boton-primario')}
              disabled={ocupado}
              onClick={() => input.current?.click()}
            >
              {ocupado ? 'Guardando…' : vistaPrevia ? 'Cambiar' : 'Elegir imagen'}
            </button>
            {vistaPrevia ? (
              <button type="button" className="boton boton-texto" disabled={ocupado} onClick={onQuitar}>
                Quitar
              </button>
            ) : null}
          </div>
          <p className="texto-terciario">
            JPG, PNG o WebP · hasta 2 MB · ideal 1080 × 1350 (vertical 4:5)
          </p>
        </div>
      </div>
      {error ? (
        <p role="alert" className="estado-error">
          {error}
        </p>
      ) : null}
      <input
        ref={input}
        type="file"
        accept={TIPOS_DE_PORTADA.join(',')}
        hidden
        onChange={(e) => recibir(e.target.files)}
      />
    </div>
  );
}
