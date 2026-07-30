import { useState } from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity, RefreshControl, Platform } from 'react-native';

const ALERTAS_MOCK = [
  {
    id: '1', tipo: 'urgente',
    titulo: 'Score crítico — garantia expira em 8 dias',
    desc: 'RANGER 2023 · Score 94 · 218 dias sem revisão',
    hora: 'Agora', acao: 'Ligar agora',
  },
  {
    id: '2', tipo: 'urgente',
    titulo: 'Cliente entrou no geofence da concessionária',
    desc: 'MAVERICK 2024 · 320 m de distância · Score 81',
    hora: '12 min', acao: 'Ver perfil',
  },
  {
    id: '3', tipo: 'alerta',
    titulo: 'Garantia vencendo em 30 dias',
    desc: 'BRONCO SPORT 2022 · Score 78 · KM: 42.300',
    hora: '1h', acao: 'Enviar oferta',
  },
  {
    id: '4', tipo: 'alerta',
    titulo: 'Revisão vencida há 60+ dias',
    desc: 'TERRITORY 2023 · Score 71 · Última revisão: Jun/24',
    hora: '2h', acao: 'Agendar',
  },
  {
    id: '5', tipo: 'alerta',
    titulo: 'Padrão de abandono detectado pela IA',
    desc: 'TRANSIT 2022 · Score 68 · Histórico: 1 revisão apenas',
    hora: '3h', acao: 'Enviar oferta',
  },
  {
    id: '6', tipo: 'info',
    titulo: 'Disparo automático enviado — perfil Esquecido',
    desc: 'F-150 2023 · Score 55 · Campanha: revisão -15%',
    hora: 'Ontem', acao: null,
  },
  {
    id: '7', tipo: 'info',
    titulo: 'Cliente agendou revisão após contato',
    desc: 'KA 2022 · Score 38 · Agendado para 15/07',
    hora: 'Ontem', acao: null,
  },
  {
    id: '8', tipo: 'info',
    titulo: 'Meta de retenção atingida — semana 26',
    desc: 'VIN Share: 71,4% (+2,1 pp vs. semana anterior)',
    hora: '2 dias', acao: null,
  },
];

const COR_TIPO: Record<string, string> = {
  urgente: '#E74C3C',
  alerta: '#E67E22',
  info: '#3498DB',
};

export default function AlertasScreen() {
  const [refreshing, setRefreshing] = useState(false);
  const [modalVisivel, setModalVisivel] = useState(false);
  const [acaoAtual, setAcaoAtual] = useState('');

  const onRefresh = () => {
    setRefreshing(true);
    setTimeout(() => setRefreshing(false), 800);
  };

  const abrirModal = (acao: string) => {
    setAcaoAtual(acao);
    setModalVisivel(true);
  };

  const urgentes = ALERTAS_MOCK.filter((a) => a.tipo === 'urgente').length;

  return (
    <View style={styles.container}>

      {/* Overlay de ação automática — compatível com web e nativo */}
      {modalVisivel && (
        <View style={styles.modalOverlay}>
          <TouchableOpacity style={StyleSheet.absoluteFillObject} onPress={() => setModalVisivel(false)} />
          <View style={styles.modalBox}>
            <View style={styles.modalIconWrap}>
              <Text style={styles.modalIcon}>⚡</Text>
            </View>
            <Text style={styles.modalTitulo}>Ação Automática</Text>
            <Text style={styles.modalAcao}>{acaoAtual}</Text>
            <Text style={styles.modalDesc}>
              No FordRetain, esta ação é disparada automaticamente pelo sistema — sem precisar de intervenção do consultor.{'\n\n'}
              O botão existe apenas para demonstrar o gatilho. Em produção, a mensagem já teria sido enviada ao cliente no canal certo, no momento certo.
            </Text>
            <TouchableOpacity style={styles.modalBtn} onPress={() => setModalVisivel(false)}>
              <Text style={styles.modalBtnText}>Entendido</Text>
            </TouchableOpacity>
          </View>
        </View>
      )}

      <View style={styles.header}>
        <Text style={styles.title}>Alertas</Text>
        <Text style={styles.sub}>Ações automáticas e notificações de risco</Text>
      </View>

      <View style={styles.banner}>
        <Text style={styles.bannerText}>
          🔴  {urgentes} clientes em zona crítica hoje — potencial de R$ 1.900 em receita
        </Text>
      </View>

      <FlatList
        data={ALERTAS_MOCK}
        keyExtractor={(item) => item.id}
        contentContainerStyle={{ padding: 12 }}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        renderItem={({ item }) => {
          const cor = COR_TIPO[item.tipo];
          return (
            <View style={[styles.card, { borderLeftColor: cor }]}>
              <View style={styles.cardHeader}>
                <Text style={[styles.tipo, { color: cor }]}>{item.tipo.toUpperCase()}</Text>
                <Text style={styles.hora}>{item.hora}</Text>
              </View>
              <Text style={styles.cardTitle}>{item.titulo}</Text>
              <Text style={styles.cardDesc}>{item.desc}</Text>
              {item.acao && (
                <TouchableOpacity
                  style={[styles.acaoBtn, { backgroundColor: cor + '20' }]}
                  onPress={() => abrirModal(item.acao!)}
                >
                  <Text style={[styles.acaoBtnText, { color: cor }]}>{item.acao} →</Text>
                </TouchableOpacity>
              )}
            </View>
          );
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F7FA' },
  header: { backgroundColor: '#1F3A6E', padding: 16, paddingTop: 48 },
  title: { fontSize: 20, fontWeight: '700', color: '#fff' },
  sub: { fontSize: 12, color: '#9BB3D4', marginTop: 2 },
  banner: {
    backgroundColor: '#E74C3C', paddingHorizontal: 14, paddingVertical: 9,
  },
  bannerText: { color: '#fff', fontSize: 12, fontWeight: '700' },
  card: {
    backgroundColor: '#fff', borderRadius: 10, padding: 12,
    marginBottom: 8, borderLeftWidth: 4,
  },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 },
  tipo: { fontSize: 10, fontWeight: '700' },
  hora: { fontSize: 10, color: '#aaa' },
  cardTitle: { fontSize: 14, fontWeight: '700', color: '#222', marginBottom: 2 },
  cardDesc: { fontSize: 11, color: '#666' },
  acaoBtn: {
    marginTop: 8, alignSelf: 'flex-start',
    borderRadius: 6, paddingHorizontal: 10, paddingVertical: 5,
  },
  acaoBtnText: { fontSize: 11, fontWeight: '700' },
  modalOverlay: {
    position: Platform.OS === 'web' ? ('fixed' as any) : 'absolute',
    top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.55)',
    alignItems: 'center', justifyContent: 'center', padding: 24,
    zIndex: 999,
  },
  modalBox: {
    backgroundColor: '#fff', borderRadius: 16,
    padding: 24, width: '100%', maxWidth: 380, alignItems: 'center',
  },
  modalIconWrap: {
    width: 56, height: 56, borderRadius: 28,
    backgroundColor: '#1F3A6E15', alignItems: 'center',
    justifyContent: 'center', marginBottom: 12,
  },
  modalIcon: { fontSize: 26 },
  modalTitulo: {
    fontSize: 17, fontWeight: '700', color: '#1F3A6E', marginBottom: 4,
  },
  modalAcao: {
    fontSize: 13, fontWeight: '700', color: '#E67E22',
    marginBottom: 14, textAlign: 'center',
  },
  modalDesc: {
    fontSize: 13, color: '#555', lineHeight: 20,
    textAlign: 'center', marginBottom: 20,
  },
  modalBtn: {
    backgroundColor: '#1F3A6E', borderRadius: 10,
    paddingVertical: 12, paddingHorizontal: 32,
  },
  modalBtnText: { color: '#fff', fontWeight: '700', fontSize: 14 },
});
