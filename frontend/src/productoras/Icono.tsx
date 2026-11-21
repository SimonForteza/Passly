// Íconos del dashboard de gestión (los mismos trazos que el diseño en Figma, Web 20).
const TRAZOS = {
  resumen: (
    <>
      <rect x="3" y="3" width="7" height="9" rx="1" />
      <rect x="14" y="3" width="7" height="5" rx="1" />
      <rect x="14" y="12" width="7" height="9" rx="1" />
      <rect x="3" y="16" width="7" height="5" rx="1" />
    </>
  ),
  eventos: (
    <>
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M16 2v4M8 2v4M3 10h18" />
    </>
  ),
  equipo: (
    <>
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
    </>
  ),
  validar: (
    <path d="M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2M7 12h10" />
  ),
  externo: (
    <path d="M15 3h6v6M10 14 21 3M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
  ),
  volver: <path d="m12 19-7-7 7-7M19 12H5" />,
  chevrones: <path d="m7 15 5 5 5-5M7 9l5-5 5 5" />,
  check: <path d="M20 6 9 17l-5-5" />,
  menu: <path d="M4 6h16M4 12h16M4 18h16" />,
  cerrar: <path d="M18 6 6 18M6 6l12 12" />,
  mas: <path d="M12 5v14M5 12h14" />,
};

export type NombreDeIcono = keyof typeof TRAZOS;

export function Icono({ nombre, tamanio = 18 }: { nombre: NombreDeIcono; tamanio?: number }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width={tamanio}
      height={tamanio}
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {TRAZOS[nombre]}
    </svg>
  );
}
