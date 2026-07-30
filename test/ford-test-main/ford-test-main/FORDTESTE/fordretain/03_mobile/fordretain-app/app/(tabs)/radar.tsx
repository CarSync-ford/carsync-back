import { useEffect, useRef, useState } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity,
  Animated, Dimensions,
} from 'react-native';

const { width: SCREEN_W } = Dimensions.get('window');
const MAP_H = 310;
const DOT = 20;

const DEALER_X = SCREEN_W * 0.54;
const DEALER_Y = MAP_H * 0.50;
const GEOFENCE_R = 90;

// Cliente começa no canto superior esquerdo do mapa
const START = { x: SCREEN_W * 0.09, y: MAP_H * 0.13 };
// Para perto do dealer (dentro do geofence), mas não exatamente em cima
const END = { x: DEALER_X - 28, y: DEALER_Y + 14 };

const ANIM_MS = 4400;

// Linhas do grid simulando ruas
const H_ROADS = [0.27, 0.52, 0.78].map((p) => p * MAP_H);
const V_ROADS = [0.20, 0.52, 0.76].map((p) => p * SCREEN_W);

export default function RadarDemo() {
  const clientPos = useRef(new Animated.ValueXY(START)).current;
  const geofencePulse = useRef(new Animated.Value(1)).current;
  const alertSlide = useRef(new Animated.Value(280)).current;
  const alertOpacity = useRef(new Animated.Value(0)).current;

  const [distancia, setDistancia] = useState(1240);
  const [alertaAtivo, setAlertaAtivo] = useState(false);
  const [simulando, setSimulando] = useState(false);
  const animRef = useRef<Animated.CompositeAnimation | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Pulso contínuo do geofence
  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(geofencePulse, { toValue: 1.09, duration: 1100, useNativeDriver: true }),
        Animated.timing(geofencePulse, { toValue: 1.00, duration: 1100, useNativeDriver: true }),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, []);

  const resetar = () => {
    if (timerRef.current) clearInterval(timerRef.current);
    if (animRef.current) animRef.current.stop();
    clientPos.setValue(START);
    alertSlide.setValue(280);
    alertOpacity.setValue(0);
    setDistancia(1240);
    setAlertaAtivo(false);
    setSimulando(false);
  };

  const simular = () => {
    resetar();
    setTimeout(() => {
      setSimulando(true);

      const anim = Animated.timing(clientPos, {
        toValue: END,
        duration: ANIM_MS,
        useNativeDriver: false,
      });
      animRef.current = anim;

      anim.start(({ finished }) => {
        if (!finished) return;
        setSimulando(false);
        setAlertaAtivo(true);
        setDistancia(285);
        Animated.parallel([
          Animated.spring(alertSlide, { toValue: 0, tension: 62, friction: 9, useNativeDriver: true }),
          Animated.timing(alertOpacity, { toValue: 1, duration: 280, useNativeDriver: true }),
        ]).start();
      });

      // Atualiza contador de distância ao longo da animação
      const t0 = Date.now();
      timerRef.current = setInterval(() => {
        const prog = Math.min((Date.now() - t0) / ANIM_MS, 1);
        setDistancia(Math.round(1240 - 955 * prog));
        if (prog >= 1 && timerRef.current) {
          clearInterval(timerRef.current);
          timerRef.current = null;
        }
      }, 80);
    }, 60);
  };

  return (
    <View style={s.root}>
      {/* Header */}
      <View style={s.header}>
        <Text style={s.hTitle}>Geo-Trigger  ·  Demo ao Vivo</Text>
        <Text style={s.hSub}>Simulação de proximidade e alerta automático</Text>
      </View>

      {/* Mapa simulado */}
      <View style={s.map}>
        {/* Ruas horizontais */}
        {H_ROADS.map((y) => (
          <View key={y} style={[s.road, { top: y, left: 0, right: 0, height: 1 }]} />
        ))}
        {/* Ruas verticais */}
        {V_ROADS.map((x) => (
          <View key={x} style={[s.road, { left: x, top: 0, bottom: 0, width: 1 }]} />
        ))}

        {/* Área de geofence (círculo pulsante) */}
        <Animated.View
          style={[
            s.geofence,
            {
              left: DEALER_X - GEOFENCE_R,
              top: DEALER_Y - GEOFENCE_R,
              width: GEOFENCE_R * 2,
              height: GEOFENCE_R * 2,
              borderRadius: GEOFENCE_R,
              transform: [{ scale: geofencePulse }],
            },
          ]}
        />

        {/* Marcador da concessionária */}
        <View style={[s.dealer, { left: DEALER_X - 22, top: DEALER_Y - 22 }]}>
          <Text style={s.dealerF}>F</Text>
        </View>
        <Text style={[s.dealerLabel, { left: DEALER_X - 44, top: DEALER_Y + 27 }]}>
          Concessionária
        </Text>

        {/* Ponto do cliente (animado) */}
        <Animated.View
          style={[
            s.clientWrap,
            {
              left: Animated.subtract(clientPos.x, DOT / 2) as any,
              top: Animated.subtract(clientPos.y, DOT / 2) as any,
            },
          ]}
        >
          <View style={s.clientDot} />
        </Animated.View>

        {/* Badge de distância */}
        <View style={s.distBadge}>
          <Text style={s.distVal}>{distancia.toLocaleString('pt-BR')} m</Text>
          <Text style={s.distLbl}>da concessionária</Text>
        </View>

        {/* Label do raio */}
        <Text style={[s.geoLabel, { left: DEALER_X + GEOFENCE_R - 20, top: DEALER_Y - 8 }]}>
          500 m
        </Text>
      </View>

      {/* Botão ou status */}
      {!simulando && !alertaAtivo && (
        <TouchableOpacity style={s.simBtn} onPress={simular}>
          <Text style={s.simBtnTxt}>▶   Simular aproximação</Text>
        </TouchableOpacity>
      )}
      {simulando && (
        <View style={s.scanning}>
          <Text style={s.scanTxt}>🛰  Monitorando localização em tempo real...</Text>
        </View>
      )}

      {/* Card de alerta */}
      {alertaAtivo && (
        <Animated.View
          style={[
            s.alertCard,
            { transform: [{ translateY: alertSlide }], opacity: alertOpacity },
          ]}
        >
          <View style={s.alertTop}>
            <View style={s.alertPing} />
            <Text style={s.alertTitulo}>Alerta de proximidade disparado!</Text>
          </View>

          <View style={s.alertBody}>
            <Text style={s.alertModelo}>🚗  RANGER 2023  ·  Score de risco: 78 / 100</Text>
            <Text style={s.alertInfo}>
              285 m da concessionária  ·  Última revisão: 214 dias atrás
            </Text>
            <View style={s.alertScriptTag}>
              <Text style={s.alertScriptTxt}>
                Script: perfil "Esquecido" — ofereça agendamento imediato
              </Text>
            </View>
          </View>

          <View style={s.alertBtns}>
            <TouchableOpacity style={s.alertBtnPri}>
              <Text style={s.alertBtnPriTxt}>Ver perfil + script</Text>
            </TouchableOpacity>
            <TouchableOpacity style={s.alertBtnSec} onPress={resetar}>
              <Text style={s.alertBtnSecTxt}>Reiniciar</Text>
            </TouchableOpacity>
          </View>
        </Animated.View>
      )}
    </View>
  );
}

const s = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#F5F7FA' },

  header: {
    backgroundColor: '#1F3A6E',
    paddingTop: 48, paddingBottom: 14, paddingHorizontal: 16,
  },
  hTitle: { fontSize: 18, fontWeight: '700', color: '#fff' },
  hSub: { fontSize: 12, color: '#9BB3D4', marginTop: 2 },

  // Mapa
  map: {
    width: SCREEN_W, height: MAP_H,
    backgroundColor: '#0A1628', overflow: 'hidden', position: 'relative',
  },
  road: { position: 'absolute', backgroundColor: 'rgba(255,255,255,0.07)' },

  geofence: {
    position: 'absolute',
    backgroundColor: 'rgba(26,115,232,0.10)',
    borderWidth: 1.5,
    borderColor: 'rgba(77,166,255,0.55)',
    borderStyle: 'dashed',
  },

  dealer: {
    position: 'absolute', width: 44, height: 44, borderRadius: 22,
    backgroundColor: '#1F3A6E', alignItems: 'center', justifyContent: 'center',
    borderWidth: 2, borderColor: '#4da6ff',
    shadowColor: '#4da6ff', shadowOpacity: 0.6, shadowRadius: 8,
  },
  dealerF: { color: '#fff', fontSize: 20, fontWeight: '900' },
  dealerLabel: {
    position: 'absolute', color: 'rgba(255,255,255,0.55)',
    fontSize: 10, textAlign: 'center', width: 88,
  },

  clientWrap: {
    position: 'absolute', width: DOT, height: DOT,
    alignItems: 'center', justifyContent: 'center',
  },
  clientDot: {
    width: DOT, height: DOT, borderRadius: DOT / 2,
    backgroundColor: '#ff4444', borderWidth: 2.5, borderColor: '#fff',
    shadowColor: '#ff4444', shadowOpacity: 0.9, shadowRadius: 8, elevation: 6,
  },

  distBadge: {
    position: 'absolute', top: 10, right: 12,
    backgroundColor: 'rgba(0,0,0,0.65)',
    borderRadius: 8, paddingHorizontal: 10, paddingVertical: 6,
    borderWidth: 1, borderColor: 'rgba(255,255,255,0.12)',
  },
  distVal: { color: '#00d4ff', fontSize: 15, fontWeight: '700', textAlign: 'right' },
  distLbl: { color: 'rgba(255,255,255,0.45)', fontSize: 10, textAlign: 'right' },

  geoLabel: {
    position: 'absolute', color: 'rgba(77,166,255,0.7)',
    fontSize: 10, fontWeight: '600',
  },

  // Botões / status
  simBtn: {
    margin: 16, backgroundColor: '#1F3A6E',
    borderRadius: 12, padding: 16, alignItems: 'center',
  },
  simBtnTxt: { color: '#fff', fontWeight: '700', fontSize: 15 },

  scanning: { margin: 16, alignItems: 'center', paddingVertical: 12 },
  scanTxt: { color: '#1F3A6E', fontSize: 13, fontWeight: '600' },

  // Card de alerta
  alertCard: {
    marginHorizontal: 12, marginTop: 8,
    backgroundColor: '#fff', borderRadius: 14,
    borderLeftWidth: 5, borderLeftColor: '#E74C3C',
    overflow: 'hidden',
    shadowColor: '#000', shadowOpacity: 0.14, shadowRadius: 10, elevation: 6,
  },
  alertTop: {
    flexDirection: 'row', alignItems: 'center', gap: 8,
    paddingHorizontal: 14, paddingTop: 12, paddingBottom: 4,
  },
  alertPing: {
    width: 10, height: 10, borderRadius: 5, backgroundColor: '#E74C3C',
  },
  alertTitulo: { fontSize: 13, fontWeight: '700', color: '#C0392B' },

  alertBody: { paddingHorizontal: 14, paddingBottom: 12 },
  alertModelo: { fontSize: 14, fontWeight: '700', color: '#222', marginBottom: 4 },
  alertInfo: { fontSize: 12, color: '#666', marginBottom: 8 },
  alertScriptTag: {
    backgroundColor: '#EAF0FB', borderRadius: 6,
    paddingHorizontal: 10, paddingVertical: 5,
  },
  alertScriptTxt: { fontSize: 11, color: '#1F3A6E', fontWeight: '600' },

  alertBtns: {
    flexDirection: 'row',
    borderTopWidth: 1, borderTopColor: '#F0F0F0',
  },
  alertBtnPri: {
    flex: 2, backgroundColor: '#1F3A6E', padding: 14, alignItems: 'center',
  },
  alertBtnPriTxt: { color: '#fff', fontWeight: '700', fontSize: 13 },
  alertBtnSec: {
    flex: 1, padding: 14, alignItems: 'center',
  },
  alertBtnSecTxt: { color: '#999', fontSize: 13 },
});
