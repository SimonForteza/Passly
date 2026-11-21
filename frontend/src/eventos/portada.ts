import { useEffect, useState } from 'react';

export const TIPOS_DE_PORTADA = ['image/jpeg', 'image/png', 'image/webp'];
export const TAMANIO_MAXIMO_PORTADA = 2 * 1024 * 1024;

/**
 * Validación espejo del backend (PAS-47): evita subir algo que ya se sabe inválido. El backend
 * sigue siendo la autoridad: además mira los primeros bytes del archivo, no solo el tipo que
 * declara el navegador.
 */
export function errorDePortada(archivo: File): string | null {
  if (!TIPOS_DE_PORTADA.includes(archivo.type)) {
    return 'La portada tiene que ser una imagen JPG, PNG o WebP.';
  }
  if (archivo.size > TAMANIO_MAXIMO_PORTADA) {
    const mb = (archivo.size / 1024 / 1024).toFixed(1).replace('.', ',');
    return `El archivo pesa ${mb} MB. Subí una imagen de hasta 2 MB.`;
  }
  return null;
}

/**
 * Object URL de un Blob o File, liberado al cambiar o desmontar. Se crea dentro del efecto (y no
 * con useMemo) para que el doble montaje de StrictMode no deje una URL ya revocada en pantalla.
 */
export function useObjectUrl(fuente: Blob | null): string | null {
  const [creada, setCreada] = useState<{ fuente: Blob; url: string } | null>(null);
  useEffect(() => {
    if (!fuente) return;
    const url = URL.createObjectURL(fuente);
    // Async a propósito: el setState no corre durante el efecto (evita el render en cascada).
    queueMicrotask(() => setCreada({ fuente, url }));
    return () => URL.revokeObjectURL(url);
  }, [fuente]);
  // Si la fuente cambió y todavía no se creó su URL, no se muestra la de la fuente anterior.
  return creada && creada.fuente === fuente ? creada.url : null;
}
