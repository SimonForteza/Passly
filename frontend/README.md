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
  de borradores desde el backoffice.
- **Pendientes:** `/carrito` y `/mis-ordenes` siguen siendo placeholders hasta sus respectivas
  issues de frontend.
