import AsyncStorage from '@react-native-async-storage/async-storage';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StatusBar as RNStatusBar,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import {
  type EventoDTO,
  type Sesion,
  type ValidacionDTO,
  SesionVencidaError,
  iniciarSesion,
  listarEventosParaValidar,
  validarEntrada,
} from './src/api';

const CLAVE_SERVIDOR = 'passly.servidor';
const SERVIDOR_POR_DEFECTO = 'http://192.168.0.10:8080';

const colores = {
  fondo: '#0F0F14',
  tarjeta: '#1C1C24',
  borde: '#2E2E3A',
  texto: '#FFFFFF',
  secundario: '#A0A0B0',
  primario: '#7C5CFF',
  valido: '#16A34A',
  yaUsado: '#EA8A0C',
  invalido: '#DC2626',
};

/**
 * Android dibuja la app detrás de la barra de estado y de la de navegación (edge-to-edge).
 * Arriba se usa la altura real de la barra de estado; abajo, un margen fijo que cubre la barra de
 * tres botones (48 dp). No se usa react-native-safe-area-context: su código C++ supera el límite
 * de 260 caracteres de ruta de Windows al compilar.
 */
const margenes = {
  top: Platform.OS === 'android' ? (RNStatusBar.currentHeight ?? 24) : 48,
  bottom: Platform.OS === 'android' ? 48 : 34,
};

export default function App() {
  const [sesion, setSesion] = useState<Sesion | null>(null);
  const [evento, setEvento] = useState<EventoDTO | null>(null);
  const [aviso, setAviso] = useState('');

  const cerrarSesion = useCallback((mensaje = '') => {
    setEvento(null);
    setSesion(null);
    setAviso(mensaje);
  }, []);

  return (
    <View style={[estilos.raiz, { paddingTop: margenes.top, paddingBottom: margenes.bottom }]}>
      <StatusBar style="light" />
      {!sesion ? (
        <PantallaLogin aviso={aviso} onIngresar={(s) => { setAviso(''); setSesion(s); }} />
      ) : !evento ? (
        <PantallaEventos sesion={sesion} onElegir={setEvento} onSalir={cerrarSesion} />
      ) : (
        <PantallaEscaner
          sesion={sesion}
          evento={evento}
          onVolver={() => setEvento(null)}
          onSesionVencida={cerrarSesion}
        />
      )}
    </View>
  );
}

// ---------------------------------------------------------------------------
// Login
// ---------------------------------------------------------------------------

function PantallaLogin({ aviso, onIngresar }: { aviso: string; onIngresar: (s: Sesion) => void }) {
  const [servidor, setServidor] = useState(SERVIDOR_POR_DEFECTO);
  const [email, setEmail] = useState('validador@passly.test');
  const [password, setPassword] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(aviso);

  useEffect(() => {
    AsyncStorage.getItem(CLAVE_SERVIDOR)
      .then((guardado) => { if (guardado) setServidor(guardado); })
      .catch(() => undefined);
  }, []);

  async function ingresar() {
    setEnviando(true);
    setError('');
    const destino = servidor.trim();
    try {
      const s = await iniciarSesion(destino, email.trim(), password);
      AsyncStorage.setItem(CLAVE_SERVIDOR, destino).catch(() => undefined);
      onIngresar(s);
    } catch (causa) {
      setError(causa instanceof Error ? causa.message : 'No se pudo ingresar.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={estilos.pantallaCentrada} keyboardShouldPersistTaps="handled">
        <Text style={estilos.marca}>Passly</Text>
        <Text style={estilos.subtitulo}>Control de acceso</Text>

        <Text style={estilos.etiqueta}>Servidor</Text>
        <TextInput
          style={estilos.campo}
          value={servidor}
          onChangeText={setServidor}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="url"
          placeholder="http://192.168.x.x:8080"
          placeholderTextColor={colores.secundario}
        />

        <Text style={estilos.etiqueta}>Email</Text>
        <TextInput
          style={estilos.campo}
          value={email}
          onChangeText={setEmail}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="email-address"
        />

        <Text style={estilos.etiqueta}>Contraseña</Text>
        <TextInput
          style={estilos.campo}
          value={password}
          onChangeText={setPassword}
          secureTextEntry
          onSubmitEditing={ingresar}
        />

        {error ? <Text style={estilos.error}>{error}</Text> : null}

        <Boton texto={enviando ? 'Ingresando…' : 'Ingresar'} onPress={ingresar} deshabilitado={enviando} />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

// ---------------------------------------------------------------------------
// Elegir evento
// ---------------------------------------------------------------------------

function PantallaEventos({
  sesion,
  onElegir,
  onSalir,
}: {
  sesion: Sesion;
  onElegir: (e: EventoDTO) => void;
  onSalir: (mensaje?: string) => void;
}) {
  const [eventos, setEventos] = useState<EventoDTO[] | null>(null);
  const [error, setError] = useState('');

  const cargar = useCallback(() => {
    setEventos(null);
    setError('');
    listarEventosParaValidar(sesion)
      .then(setEventos)
      .catch((causa) => {
        if (causa instanceof SesionVencidaError) onSalir(causa.message);
        else setError(causa instanceof Error ? causa.message : 'No se pudieron cargar los eventos.');
      });
  }, [sesion, onSalir]);

  useEffect(cargar, [cargar]);

  return (
    <View style={estilos.pantalla}>
      <View style={estilos.encabezado}>
        <View style={{ flex: 1 }}>
          <Text style={estilos.titulo}>Elegí el evento</Text>
          <Text style={estilos.secundario}>Hola, {sesion.nombre}</Text>
        </View>
        <Pressable onPress={() => onSalir()} hitSlop={12}>
          <Text style={estilos.enlace}>Salir</Text>
        </Pressable>
      </View>

      {error ? (
        <View style={{ gap: 12 }}>
          <Text style={estilos.error}>{error}</Text>
          <Boton texto="Reintentar" onPress={cargar} />
        </View>
      ) : eventos === null ? (
        <ActivityIndicator color={colores.primario} size="large" style={{ marginTop: 40 }} />
      ) : eventos.length === 0 ? (
        <Text style={estilos.secundario}>No hay eventos publicados de tus productoras para validar.</Text>
      ) : (
        <FlatList
          data={eventos}
          keyExtractor={(e) => String(e.id)}
          contentContainerStyle={{ gap: 12, paddingBottom: 24 }}
          renderItem={({ item }) => (
            <Pressable
              style={({ pressed }) => [estilos.tarjeta, pressed && { opacity: 0.7 }]}
              onPress={() => onElegir(item)}
            >
              <Text style={estilos.tarjetaTitulo}>{item.nombre}</Text>
              <Text style={estilos.secundario}>
                {formatearFecha(item.fechaHora)} · {item.lugar}
              </Text>
            </Pressable>
          )}
        />
      )}
    </View>
  );
}

// ---------------------------------------------------------------------------
// Escanear
// ---------------------------------------------------------------------------

function PantallaEscaner({
  sesion,
  evento,
  onVolver,
  onSesionVencida,
}: {
  sesion: Sesion;
  evento: EventoDTO;
  onVolver: () => void;
  onSesionVencida: (mensaje?: string) => void;
}) {
  const [permiso, pedirPermiso] = useCameraPermissions();
  const [resultado, setResultado] = useState<ValidacionDTO | null>(null);
  const [error, setError] = useState('');
  const [validando, setValidando] = useState(false);
  const [manual, setManual] = useState('');
  const ocupado = useRef(false);

  const validar = useCallback(
    async (contenido: string) => {
      if (ocupado.current || !contenido.trim()) return;
      ocupado.current = true;
      setValidando(true);
      setError('');
      try {
        setResultado(await validarEntrada(sesion, evento.id, contenido.trim()));
      } catch (causa) {
        if (causa instanceof SesionVencidaError) {
          onSesionVencida(causa.message);
          return;
        }
        setError(causa instanceof Error ? causa.message : 'No se pudo validar la entrada.');
      } finally {
        setValidando(false);
      }
    },
    [sesion, evento.id, onSesionVencida],
  );

  function siguiente() {
    setResultado(null);
    setError('');
    setManual('');
    ocupado.current = false;
  }

  if (resultado || error) {
    return <PantallaResultado resultado={resultado} error={error} onSiguiente={siguiente} />;
  }

  return (
    <View style={estilos.pantalla}>
      <View style={estilos.encabezado}>
        <View style={{ flex: 1 }}>
          <Text style={estilos.titulo} numberOfLines={1}>{evento.nombre}</Text>
          <Text style={estilos.secundario}>Apuntá al QR de la entrada</Text>
        </View>
        <Pressable onPress={onVolver} hitSlop={12}>
          <Text style={estilos.enlace}>Cambiar</Text>
        </Pressable>
      </View>

      <View style={estilos.marcoCamara}>
        {!permiso ? (
          <ActivityIndicator color={colores.primario} />
        ) : !permiso.granted ? (
          <View style={{ padding: 20, gap: 12, alignItems: 'center' }}>
            <Text style={estilos.secundario}>Passly necesita la cámara para leer los QR.</Text>
            <Boton texto="Permitir cámara" onPress={pedirPermiso} />
          </View>
        ) : (
          <CameraView
            style={StyleSheet.absoluteFill}
            facing="back"
            barcodeScannerSettings={{ barcodeTypes: ['qr'] }}
            onBarcodeScanned={validando ? undefined : ({ data }) => { void validar(data); }}
          />
        )}
        {validando ? (
          <View style={estilos.velo}>
            <ActivityIndicator color="#fff" size="large" />
            <Text style={estilos.textoVelo}>Verificando la entrada…</Text>
          </View>
        ) : null}
      </View>

      <Text style={[estilos.etiqueta, { marginTop: 16 }]}>Ingreso manual del QR</Text>
      <View style={{ flexDirection: 'row', gap: 8 }}>
        <TextInput
          style={[estilos.campo, { flex: 1 }]}
          value={manual}
          onChangeText={setManual}
          placeholder="PASSLY:v1:…"
          placeholderTextColor={colores.secundario}
          autoCapitalize="none"
          autoCorrect={false}
        />
        <Boton texto="Validar" onPress={() => validar(manual)} deshabilitado={validando || !manual.trim()} />
      </View>
    </View>
  );
}

function PantallaResultado({
  resultado,
  error,
  onSiguiente,
}: {
  resultado: ValidacionDTO | null;
  error: string;
  onSiguiente: () => void;
}) {
  let fondo = colores.invalido;
  let titulo = 'No se pudo validar';
  let detalle = error;

  if (resultado) {
    switch (resultado.resultado) {
      case 'VALIDO':
        fondo = colores.valido;
        titulo = '✓ VÁLIDO';
        detalle = resultado.ticket?.nombreTipoEntrada ?? resultado.mensaje;
        break;
      case 'YA_USADO':
        fondo = colores.yaUsado;
        titulo = '⚠ YA USADO';
        detalle = resultado.ticket?.usadoEn
          ? `Ingresó ${formatearFecha(resultado.ticket.usadoEn)}`
          : resultado.mensaje;
        break;
      case 'OTRO_EVENTO':
        titulo = '✕ OTRO EVENTO';
        detalle = resultado.mensaje;
        break;
      default:
        titulo = '✕ INVÁLIDO';
        detalle = resultado.mensaje;
    }
  }

  return (
    <View style={[estilos.resultado, { backgroundColor: fondo }]}>
      <Text style={estilos.resultadoTitulo}>{titulo}</Text>
      {detalle ? <Text style={estilos.resultadoDetalle}>{detalle}</Text> : null}
      <Pressable style={estilos.botonBlanco} onPress={onSiguiente}>
        <Text style={[estilos.botonTexto, { color: fondo }]}>Validar siguiente entrada</Text>
      </Pressable>
    </View>
  );
}

// ---------------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------------

function Boton({ texto, onPress, deshabilitado }: { texto: string; onPress: () => void; deshabilitado?: boolean }) {
  return (
    <Pressable
      onPress={onPress}
      disabled={deshabilitado}
      style={({ pressed }) => [estilos.boton, (pressed || deshabilitado) && { opacity: 0.6 }]}
    >
      <Text style={estilos.botonTexto}>{texto}</Text>
    </Pressable>
  );
}

function formatearFecha(iso: string): string {
  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) return iso;
  const dd = String(fecha.getDate()).padStart(2, '0');
  const mm = String(fecha.getMonth() + 1).padStart(2, '0');
  const hh = String(fecha.getHours()).padStart(2, '0');
  const mi = String(fecha.getMinutes()).padStart(2, '0');
  return `${dd}/${mm} ${hh}:${mi}`;
}

const estilos = StyleSheet.create({
  raiz: { flex: 1, backgroundColor: colores.fondo },
  pantalla: { flex: 1, paddingHorizontal: 20, paddingTop: 16, paddingBottom: 16 },
  pantallaCentrada: { flexGrow: 1, justifyContent: 'center', padding: 24, gap: 8 },
  marca: { color: colores.texto, fontSize: 40, fontWeight: '800', textAlign: 'center' },
  subtitulo: { color: colores.secundario, fontSize: 16, textAlign: 'center', marginBottom: 24 },
  encabezado: { flexDirection: 'row', alignItems: 'center', marginBottom: 20, gap: 12 },
  titulo: { color: colores.texto, fontSize: 24, fontWeight: '700' },
  secundario: { color: colores.secundario, fontSize: 15 },
  enlace: { color: colores.primario, fontSize: 16, fontWeight: '600' },
  etiqueta: { color: colores.secundario, fontSize: 13, marginTop: 8 },
  campo: {
    backgroundColor: colores.tarjeta,
    borderColor: colores.borde,
    borderWidth: 1,
    borderRadius: 10,
    color: colores.texto,
    fontSize: 16,
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  error: { color: '#FCA5A5', fontSize: 15, marginTop: 8 },
  boton: {
    backgroundColor: colores.primario,
    borderRadius: 10,
    paddingVertical: 14,
    paddingHorizontal: 18,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8,
  },
  botonTexto: { color: '#fff', fontSize: 16, fontWeight: '700' },
  tarjeta: {
    backgroundColor: colores.tarjeta,
    borderColor: colores.borde,
    borderWidth: 1,
    borderRadius: 12,
    padding: 16,
    gap: 4,
  },
  tarjetaTitulo: { color: colores.texto, fontSize: 18, fontWeight: '600' },
  marcoCamara: {
    flex: 1,
    borderRadius: 16,
    overflow: 'hidden',
    backgroundColor: '#000',
    alignItems: 'center',
    justifyContent: 'center',
  },
  velo: {
    ...StyleSheet.absoluteFill,
    backgroundColor: 'rgba(0,0,0,0.6)',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  textoVelo: { color: '#fff', fontSize: 16 },
  resultado: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32, gap: 16 },
  resultadoTitulo: { color: '#fff', fontSize: 44, fontWeight: '900', textAlign: 'center' },
  resultadoDetalle: { color: '#fff', fontSize: 20, textAlign: 'center' },
  botonBlanco: {
    backgroundColor: '#fff',
    borderRadius: 12,
    paddingVertical: 16,
    paddingHorizontal: 24,
    marginTop: 24,
  },
});
