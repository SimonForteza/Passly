export interface ItemDeCarritoDTO {
  idTipoEntrada: number;
  idEvento: number;
  nombreTipoEntrada: string;
  precioUnitario: number;
  cantidad: number;
}

export interface CarritoDTO {
  items: ItemDeCarritoDTO[];
  total: number;
  expiraEn: string;
  segundosRestantes: number;
  expirado: boolean;
}

export interface AgregarItemRequest {
  idTipoEntrada: number;
  cantidad: number;
}

export interface ItemDeOrdenDTO {
  idTipoEntrada: number;
  idEvento: number;
  nombreTipoEntrada: string;
  precioUnitario: number;
  cantidad: number;
}

export interface CompradorDeOrdenDTO {
  id: number;
  email: string;
  nombre: string;
}

export interface OrdenDTO {
  id: number;
  comprador: CompradorDeOrdenDTO;
  items: ItemDeOrdenDTO[];
  total: number;
  creadaEn: string;
}
