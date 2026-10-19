# Correcciones de los recorridos de Passly

Implementación posterior a la auditoría UX del 18 de octubre de 2026.

## Cambios por hallazgo

| Auditoría | Comportamiento corregido |
| --- | --- |
| UX-01 | En desarrollo, el navegador usa `/api` en el mismo origen mediante el proxy de Vite. La cookie de sesión conserva ambos carritos. Se mantiene la protección del origen de las compras invitadas y sus rechazos incluyen un mensaje específico. |
| UX-02 | Guardar un evento abre su gestión, con confirmación, edición, vista previa y publicación. La vista pública permite volver a gestionar; los borradores no muestran selectores ni resumen de compra. |
| UX-03 | Crear una productora abre su resumen con el siguiente paso: preparar el primer evento e invitar al equipo. |
| UX-04 | Cabecera con Eventos, Mis entradas y Carrito. Quien tiene membresías ve Panel de organización. Crear otra productora queda como acción secundaria dentro de ese panel. |
| UX-05 | Las acciones dependen de la membresía en la productora: dueño y staff gestionan, el dueño invita y los validadores acceden a su tarea. ADMIN puede usar los formularios cuando tiene la membresía correspondiente. |
| UX-06 | Navegación móvil plegable, paneles que permiten reducir su ancho, integrantes en filas adaptables y eventos en tarjetas. Comprobados anchos de 320 y 390 px. |
| UX-07 | Invitados y compradores con cuenta usan el mismo resumen de compra y la misma presentación de entradas: evento, fecha, lugar, categoría, importe y QR. |
| UX-08 | Carrito y recuperación de entradas siguen accesibles sin sesión. El carrito vacío no muestra un vencimiento; el carrito con selección usa una cuenta regresiva y ofrece volver a elegir cuando vence. |
| UX-09 | Guardados con confirmación, botones que identifican su sección y aviso antes de salir con cambios pendientes. |
| UX-10 | Invitaciones con tareas explicadas, email destinatario, vencimiento, aceptación y entrega del correo como estados separados. La aceptación conduce a gestionar eventos o a validar según la invitación. |
| UX-11 | Publicar abre una revisión que explica la venta pública y las restricciones posteriores. Puede conservarse el borrador. |
| UX-12 | El precio Desde toma categorías disponibles; el selector limita la cantidad al cupo mostrado. El texto aclara que el carrito no reserva y que el pago vigente es simulado. |
| UX-13 | Se conserva la identidad oscura y roja, con títulos de gestión más compactos, mejor contraste para texto de acento, estados legibles y foco visible. Fecha y lugar de los tickets tienen tamaño de lectura normal. |
| UX-14 | Alta de productora en un formulario propio con datos opcionales agrupados y vista previa de logo. Login y registro explican la tarea y conservan su destino. La página pública muestra el logo. |

## Validaciones realizadas antes de limpiar los datos de demostración

Los resultados siguientes corresponden a la revisión de la implementación antes de limpiar los eventos y compras de prueba de la base local. La limpieza posterior eliminó esos registros temporales; no se ejecutaron nuevamente pruebas contra esa base.

- Backend completo: **72 pruebas, cero fallos**, incluida `EstructuraDeModulosTest` de Spring Modulith.
- Tras ampliar las comprobaciones de invitación y del rechazo por origen: **14 pruebas de integración, cero fallos**.
- Frontend: compilación TypeScript y Vite correcta. Lint termina sin errores; mantiene advertencias de React sobre efectos y archivos que comparten componentes y hooks.
- Navegador real: alta con cuenta existente y con registro nuevo, creación de borrador y destino de gestión, revisión y publicación de un evento de prueba, vista previa del borrador sin compra, guardado con confirmación, invitación STAFF y aceptación con una cuenta existente.
- Navegador real: selección invitada transferida al iniciar sesión sin duplicar cantidades; compra invitada y autenticada confirmadas; ambas muestran evento, fecha, lugar y QR.
- Correo de compra recibido en Mailpit. Solicitud de recuperación con respuesta genérica correcta.
- Validador sin acciones de gestión en su productora; acceso directo a gestión muestra una explicación. Después de aceptar STAFF en otra productora conserva los permisos distintos de cada equipo.
- QR de la compra autenticada validado desde el formulario de control de acceso.
- Cuenta regresiva y diálogo de vencimiento comprobados adelantando el reloj del navegador de prueba; reiniciar la selección permite salir del diálogo.
- Capturas locales no versionadas en `output/playwright/ux-fixes/`. Equipo, edición y vista previa sin desbordamiento horizontal en las resoluciones comprobadas; orden y QR comprobados a 390 px.

La demostración usó registros identificados como pruebas UX y compras simuladas de $10. Esos eventos y sus compras se eliminaron después de la revisión; la base local conserva los cuatro eventos originales.

## Guion manual

1. Abrir `http://localhost:5173/`. Si la pestaña estaba abierta, recargarla. Usar siempre `localhost` en este entorno.
2. Sin sesión: **Organizar eventos → Crear mi productora → Crear una cuenta**. Completar el registro: debe volver al formulario de productora.
3. Crear una productora con nombre único. Debe aparecer su confirmación y **Crear mi primer evento**.
4. Preparar un evento futuro con una categoría de entradas. **Guardar borrador** debe abrir la gestión del evento.
5. Abrir **Vista previa**: debe aclarar que es un borrador y permitir volver a gestionarlo. No debe ofrecer comprar.
6. Editar un dato, guardarlo y comprobar la confirmación. Editar nuevamente e intentar salir para comprobar el aviso de cambios sin guardar.
7. **Revisar y publicar**: comprobar los datos y las consecuencias antes de confirmar. Volver al panel debe mantener el contexto de la productora.
8. En **Equipo**, invitar un email de prueba como staff o validador. Revisar su entrega y abrir el correo en `http://localhost:8025/`. Aceptar en otra ventana con la cuenta invitada y comprobar el destino y sus permisos.
9. En una ventana privada, abrir el evento publicado, seleccionar una entrada y **Continuar como invitado**. Revisar evento, fecha, lugar, total y aviso de compra simulada. Completar nombre y email y confirmar.
10. Comprobar la orden y el QR, descargarlo y revisar el correo local. Desde **Mis entradas**, solicitar otro enlace usando email y número de orden.
11. Repetir la selección e iniciar sesión desde el carrito: debe conservarse la cantidad elegida. Confirmar y consultar la compra desde **Mis entradas**.
12. Con el validador de la misma productora, elegir el evento e ingresar el contenido del QR desde **Alternativa para ingreso manual**. El primer uso debe ser válido y el siguiente, ya usado.

El backend y Vite deben estar en ejecución. El proxy de desarrollo usa `VITE_API_BASE_URL` como destino del servidor; cambiar esa variable requiere reiniciar Vite. En producción se debe servir `/api` desde el mismo dominio o configurar la URL y los permisos de origen correspondientes.
