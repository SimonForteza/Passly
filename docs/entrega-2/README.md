# Segunda entrega parcial · 16/11/2026

- [Guion de la demo](guion-demo.md)
- [Informe editable](informe-segunda-entrega.md)
- [Informe PDF](informe-segunda-entrega.pdf)

Generar el PDF desde la raíz del repo con Node y la misma hoja de estilos del [Checkpoint 2](../entrega-c2/README.md):

```sh
npx -y md-to-pdf@5 docs/entrega-2/informe-segunda-entrega.md --stylesheet docs/entrega-c2/pdf.css --pdf-options '{"format":"A4","margin":{"top":"14mm","bottom":"14mm","left":"12mm","right":"12mm"}}'
```
