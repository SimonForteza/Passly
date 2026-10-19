/** Acto de validacion en puerta, autorizacion y auditoria propias. */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {
    "tickets", "tickets :: dto", "eventos", "eventos :: dto", "productoras"
})
package com.passly.accesos;
