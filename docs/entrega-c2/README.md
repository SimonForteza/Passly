# Entrega — Checkpoint 2 (19/10)

| Archivo | Fuente |
|---|---|
| `integraciones-checkpoint-2.pdf` | [`../integraciones.md`](../integraciones.md) (PAS-27) |

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
