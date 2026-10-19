import { httpClient } from '../lib/http/httpClient';

export interface ValidacionDTO {
  resultado: 'VALIDO' | 'YA_USADO' | 'OTRO_EVENTO' | 'INVALIDO';
  mensaje: string;
  ticket: { id: number; idEvento: number; nombreTipoEntrada: string; usadoEn: string } | null;
}

export function validarEntrada(idEvento: number, contenidoQr: string): Promise<ValidacionDTO> {
  return httpClient.post<ValidacionDTO>('/api/accesos/validaciones', { idEvento, contenidoQr });
}
