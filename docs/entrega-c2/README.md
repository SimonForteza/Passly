# Entrega — Checkpoint 2 (19/10)

| Archivo | Fuente |
|---|---|
| `integraciones-checkpoint-2.pdf` | [`../integraciones.md`](../integraciones.md) (PAS-27) |
| `funcional-checkpoint-2.pdf` | [`../funcional.md`](../funcional.md) (PAS-28) |
| `manual-de-usuario-checkpoint-2.pdf` | [`../manual-de-usuario.md`](../manual-de-usuario.md) (PAS-28) |

Los otros dos se generan igual que el de integraciones, cambiando el `.md`. El diagrama de casos de
uso sale de `docs/diagramas/casos-de-uso.mmd` con el mismo comando de `mermaid-cli`. Las capturas
de `docs/manual/img/` se sacaron con Puppeteer contra la app real (perfil `demo`, base recién
sembrada), no se editan a mano.

El PDF se genera; no se edita. Para regenerarlo desde la raíz del repo:

```sh
# 1. Si cambió el diagrama, regenerar los exports desde la fuente Mermaid
npx -y @mermaid-js/mermaid-cli@11 -i docs/diagramas/integracion-soa.mmd -o docs/diagramas/integracion-soa.png -s 2 -b white
npx -y @mermaid-js/mermaid-cli@11 -i docs/diagramas/integracion-soa.mmd -o docs/diagramas/integracion-soa.svg -b white

# 2. Markdown -> PDF (md-to-pdf deja el PDF junto al .md; se mueve acá)
npx -y md-to-pdf@5 docs/integraciones.md --stylesheet docs/entrega-c2/pdf.css \
  --pdf-options '{"format":"A4","margin":{"top":"14mm","bottom":"14mm","left":"12mm","right":"12mm"}}'
mv docs/integraciones.pdf docs/entrega-c2/integraciones-checkpoint-2.pdf
```

Solo hace falta Node: ambas herramientas bajan un Chromium propio la primera vez.
