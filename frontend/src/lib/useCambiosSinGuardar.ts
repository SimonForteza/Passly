import { useCallback, useEffect } from 'react';
import { useBeforeUnload, useBlocker } from 'react-router-dom';

export function useCambiosSinGuardar(hayCambios: boolean) {
  const blocker = useBlocker(hayCambios);
  useBeforeUnload(
    useCallback(
      (event: BeforeUnloadEvent) => {
        if (hayCambios) {
          event.preventDefault();
          event.returnValue = '';
        }
      },
      [hayCambios],
    ),
  );
  useEffect(() => {
    if (blocker.state === 'blocked') {
      if (
        window.confirm(
          'Tenés cambios sin guardar. ¿Querés salir y descartarlos?',
        )
      )
        blocker.proceed();
      else blocker.reset();
    }
  }, [blocker]);
}
