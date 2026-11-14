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
  login (`/login`, canjea email + password por un JWT en `POST /api/auth/login`, PAS-34), perfil propio
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
- **Ventas (PAS-18, PAS-30):** carrito (`/carrito`), checkout con tarjeta de prueba, mis órdenes
  (`/mis-ordenes`) y detalle con tickets QR (`/mis-ordenes/:id`). También se puede comprar como
  invitado: se piden nombre y email y la orden se consulta desde su enlace privado. Los datos de
  tarjeta viven solo en el estado del formulario y no se guardan en el navegador.

## Componentes compartidos

- **`eventos/TarjetaEvento.tsx`:** la tarjeta de un evento en una grilla (productora, nombre,
  fecha, lugar, precio "desde" y "Ver entradas"). La usan `CarteleraPage` y
  `productoras/PaginaProductoraPage`; en esta última se omite el nombre de la productora
  (`mostrarOrganizador={false}`) porque ya está en el encabezado. Si cambia cómo se ve un
  evento en una grilla, se cambia acá.

## Autenticación (JWT, PAS-34)

- `lib/auth/session.ts` guarda en `sessionStorage` `{ usuario, token, expiraEn }`. **El password
  no se guarda nunca:** viaja solo en `POST /api/auth/login`.
- `lib/http/httpClient.ts` agrega `Authorization: Bearer <token>` a cada request. Si una request
  con token devuelve **401** (token vencido o adulterado), borra la sesión y la app vuelve a
  `/login` con `from`, para retomar donde estaba.
- El login se manda sin token (`{ anonimo: true }`): el backend responde 401 a cualquier token
  inválido, aunque la ruta sea pública.
- No hay refresh token. El token dura 2 h; al vencer, se vuelve a iniciar sesión.
- Los requests autenticados siguen en una cola serial, porque el backend rota el `JSESSIONID` en
  cada request autenticado y el carrito vive en esa sesión (CLAUDE.md §4.7).
- Para probarlo a mano: en DevTools → Application → Session Storage, cambiar un carácter del
  `token` de `passly.sesion`; la siguiente acción manda al login.
