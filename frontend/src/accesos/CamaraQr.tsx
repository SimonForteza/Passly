import { useEffect, useRef, useState } from 'react';

interface Props { onLeer: (contenido: string) => void; }

/** Detiene todas las pistas al pausar, cambiar de evento o abandonar la pantalla. */
export function CamaraQr({ onLeer }: Props) {
  const video = useRef<HTMLVideoElement>(null);
  const [error, setError] = useState('');
  useEffect(() => {
    let vigente = true;
    let detener: (() => void) | undefined;
    const elemento = video.current;
    async function iniciar() {
      try {
        if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
          throw new Error('La cámara requiere una conexión HTTPS o localhost. Podés ingresar el contenido manualmente.');
        }
        const { BrowserQRCodeReader } = await import('@zxing/browser');
        if (!vigente || !elemento) return;
        const lector = new BrowserQRCodeReader();
        const controles = await lector.decodeFromConstraints(
          { video: { facingMode: { ideal: 'environment' } }, audio: false },
          elemento,
          (resultado) => { if (vigente && resultado) onLeer(resultado.getText()); },
        );
        detener = () => controles.stop();
        if (!vigente) controles.stop();
      } catch (causa) {
        if (vigente) setError(causa instanceof Error && causa.message.includes('HTTPS') ? causa.message
          : 'No pudimos abrir la cámara. Revisá el permiso del navegador o usá el ingreso manual.');
      }
    }
    void iniciar();
    return () => {
      vigente = false;
      detener?.();
      const stream = elemento?.srcObject;
      if (stream instanceof MediaStream) stream.getTracks().forEach((pista) => pista.stop());
    };
  }, [onLeer]);
  return <div className="validador-camara">
    <video ref={video} muted playsInline aria-label="Vista de la cámara para escanear entradas" />
    {error ? <p role="alert">{error}</p> : <p>Apuntá al QR de la entrada</p>}
  </div>;
}
