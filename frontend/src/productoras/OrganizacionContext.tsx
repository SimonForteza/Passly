import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import { useAuth } from '../lib/auth/AuthContext';
import { listarMiembros, misProductoras } from './api';
import type { ProductoraDTO, RolEnProductora } from './types';

export interface Organizacion extends ProductoraDTO {
  rol: RolEnProductora;
  puedeGestionar: boolean;
  puedeInvitar: boolean;
}
interface Estado {
  usuarioId: number | null;
  organizaciones: Organizacion[];
  error: unknown;
}
interface Contexto {
  organizaciones: Organizacion[];
  cargando: boolean;
  error: unknown;
  recargar: () => Promise<Organizacion[]>;
}
const OrganizacionContext = createContext<Contexto | null>(null);

export function OrganizacionProvider({ children }: { children: ReactNode }) {
  const { usuario } = useAuth();
  const usuarioId = usuario?.id;
  const [estado, setEstado] = useState<Estado>({
    usuarioId: null,
    organizaciones: [],
    error: null,
  });
  const recargar = useCallback(async () => {
    if (!usuarioId) return [];
    const productoras = await misProductoras();
    const resultados = await Promise.all(
      productoras.map(async (p) => {
        const miembros = await listarMiembros(p.id);
        const rol = miembros.find(
          (m) => m.idUsuario === usuarioId,
        )?.rolEnProductora;
        return rol
          ? {
              ...p,
              rol,
              puedeGestionar: rol !== 'VALIDADOR',
              puedeInvitar: rol === 'DUENIO',
            }
          : null;
      }),
    );
    const organizaciones = resultados.filter(
      (p): p is Organizacion => p !== null,
    );
    setEstado({ usuarioId, organizaciones, error: null });
    return organizaciones;
  }, [usuarioId]);

  useEffect(() => {
    let vigente = true;
    if (!usuarioId) return;
    void recargar().catch((error) => {
      if (vigente) setEstado({ usuarioId, organizaciones: [], error });
    });
    return () => {
      vigente = false;
    };
  }, [recargar, usuarioId]);
  const vigente = estado.usuarioId === usuario?.id;
  return (
    <OrganizacionContext.Provider
      value={{
        organizaciones: usuario && vigente ? estado.organizaciones : [],
        cargando: !!usuario && !vigente,
        error: usuario && vigente ? estado.error : null,
        recargar,
      }}
    >
      {children}
    </OrganizacionContext.Provider>
  );
}
export function useOrganizaciones() {
  const contexto = useContext(OrganizacionContext);
  if (!contexto) throw new Error('Falta el contexto de organización.');
  return contexto;
}
