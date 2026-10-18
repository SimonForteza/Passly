# Passly — app web

Vite + React + TypeScript. Ver [../README.md](../README.md) para cómo levantarla junto al
backend, y [../CLAUDE.md](../CLAUDE.md) §8/§9 para la estructura de carpetas y las decisiones
de diseño (por qué carpetas por módulo de negocio, cliente HTTP centralizado, CORS, etc.).

```bash
cp .env.example .env
npm install
npm run dev
```

## Scripts

- `npm run dev` — servidor de desarrollo (puerto 5173)
- `npm run build` — chequeo de tipos + build de producción
- `npm run lint` — Oxlint

## Pantallas

- **Usuarios (PAS-15):** registro (`/registro`, siempre crea `COMPRADOR`, sin selector de rol),
  login (`/login`, valida email + password contra `GET /api/usuarios/me`), perfil propio
  (`/perfil`) y logout. El perfil incluye un alta de roles privilegiados
  (`ORGANIZADOR`/`VALIDADOR`/`ADMIN`) visible sólo para `ADMIN`.
- **Productoras (PAS-16):** alta, listado de las productoras del usuario y padrón de miembros,
  con gestión de roles internos en `/productoras` y `/productoras/:id/backoffice`.
- **Eventos (PAS-17):** cartelera pública, detalle, alta, listado de una productora y publicación
  de borradores desde el backoffice. El backoffice copia el link compartible de cada evento
  publicado (PAS-25).
- **Página pública de productora y búsqueda (PAS-26):** `/productoras/:id` (pública) muestra los
  eventos publicados de una productora; se llega desde el detalle de un evento o desde
  "Ver página pública" en el backoffice. La cartelera busca por nombre o lugar en el cliente y
  filtra por productora con `?productora=` en la URL.
- **Ventas (PAS-18):** carrito (`/carrito`), checkout, mis órdenes (`/mis-ordenes`) y detalle de
  cada orden con sus tickets QR (`/mis-ordenes/:id`).

## Componentes compartidos

- **`eventos/TarjetaEvento.tsx`:** la tarjeta de un evento en una grilla (productora, nombre,
  fecha, lugar, precio "desde" y "Ver entradas"). La usan `CarteleraPage` y
  `productoras/PaginaProductoraPage`; en esta última se omite el nombre de la productora
  (`mostrarOrganizador={false}`) porque ya está en el encabezado. Si cambia cómo se ve un
  evento en una grilla, se cambia acá.
