import { useCallback, useEffect, useState } from 'react';
import { consultarDisponibilidad } from './api';
import type { DisponibilidadDTO } from './types';

/**
 * Hook de disponibilidad de un tipo de entrada, pensado para reusarse tal cual desde el
 * carrito de ServicioDeVentas: no conoce AuthContext ni ninguna página en particular.
 *
 * El endpoint exige estar autenticado (cualquier rol), así que quien lo consuma decide
 * cuándo corresponde llamarlo pasando `null` en vez de un id (por ejemplo, para un
 * visitante anónimo mirando la cartelera).
 */
export function useDisponibilidad(idTipoEntrada: number | null) {
  const [disponibilidad, setDisponibilidad] = useState<DisponibilidadDTO | null>(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const recargar = useCallback(() => {
    if (idTipoEntrada === null) {
      setDisponibilidad(null);
      setError(null);
      setCargando(false);
      return;
    }

    let vigente = true;
    setCargando(true);
    setError(null);

    consultarDisponibilidad(idTipoEntrada)
      .then((resultado) => {
        if (vigente) setDisponibilidad(resultado);
      })
      .catch((causa: unknown) => {
        if (vigente) setError(causa);
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });

    return () => {
      vigente = false;
    };
  }, [idTipoEntrada]);

  useEffect(() => {
    const limpiar = recargar();
    return limpiar;
  }, [recargar]);

  return { disponibilidad, cargando, error, recargar };
}
