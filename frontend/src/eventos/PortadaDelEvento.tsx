import { useEffect, useState } from 'react';
import { mensajeDeError } from '../lib/http/ApiError';
import { bajarPortada, quitarPortada, subirPortada } from './api';
import { CampoPortada } from './CampoPortada';
import { errorDePortada, useObjectUrl } from './portada';
import type { EventoDTO } from './types';

/**
 * Portada de un evento existente, en su gestión (PAS-47). Cada cambio se guarda en el acto, en
 * borrador o publicado. La imagen actual se baja con el token: la de un borrador no es pública y un
 * <img> no manda el header Authorization.
 */
export function PortadaDelEvento({
  evento,
  onCambio,
}: {
  evento: EventoDTO;
  onCambio: (evento: EventoDTO) => void;
}) {
  const [imagen, setImagen] = useState<Blob | null>(null);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const vistaPrevia = useObjectUrl(imagen);

  useEffect(() => {
    if (!evento.portadaUrl) return;
    let vigente = true;
    bajarPortada(evento.portadaUrl)
      .then((blob) => {
        if (vigente) setImagen(blob);
      })
      .catch(() => {
        if (vigente) setImagen(null);
      });
    return () => {
      vigente = false;
    };
  }, [evento.portadaUrl]);

  async function elegir(nuevo: File) {
    const invalido = errorDePortada(nuevo);
    if (invalido) {
      setError(invalido);
      return;
    }
    setError(null);
    setOcupado(true);
    try {
      const actualizado = await subirPortada(evento.id, nuevo);
      setArchivo(nuevo);
      setImagen(nuevo);
      onCambio(actualizado);
    } catch (e) {
      setError(mensajeDeError(e));
    } finally {
      setOcupado(false);
    }
  }

  async function quitar() {
    setError(null);
    setOcupado(true);
    try {
      const actualizado = await quitarPortada(evento.id);
      setArchivo(null);
      setImagen(null);
      onCambio(actualizado);
    } catch (e) {
      setError(mensajeDeError(e));
    } finally {
      setOcupado(false);
    }
  }

  return (
    <CampoPortada
      vistaPrevia={evento.portadaUrl ? vistaPrevia : null}
      nombreArchivo={archivo?.name}
      pesoArchivo={imagen?.size}
      ocupado={ocupado}
      error={error}
      onElegir={elegir}
      onQuitar={quitar}
    />
  );
}
