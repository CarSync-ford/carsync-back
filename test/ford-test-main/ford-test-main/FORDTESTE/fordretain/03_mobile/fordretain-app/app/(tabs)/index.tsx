import { useEffect, useState } from 'react';
import {
  View, Text, FlatList, TouchableOpacity,
  StyleSheet, RefreshControl, useWindowDimensions,
} from 'react-native';
import { getClientesEmRisco, getServiceShare, getRoiConcessionaria } from '../../services/api';
import { router } from 'expo-router';

const corScore = (score: number) =>
  score >= 80 ? '#E74C3C' : score >= 60 ? '#E67E22' : '#27AE60';

const DEALER = 1009;

export default function PainelPrincipal() {
  const [clientes, setClientes] = useState<any[]>([]);
  const [share, setShare] = useState<any>(null);
  const [roi, setRoi] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const { width } = useWindowDimensions();
  const isTablet = width >= 768;
  const isSmall = width < 360;

  const carregar = async () => {
    setLoading(true);
    try {
      const [r1, r2, r3] = await Promise.all([
        getClientesEmRisco(DEALER),
        getServiceShare(DEALER),
        getRoiConcessionaria(DEALER),
      ]);
      setClientes(r1.data.clientes);
      setShare(r2.data);
      setRoi(r3.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { carregar(); }, []);

  return (
    <View style={styles.container}>
      <View style={styles.topbar}>
        <Text style={styles.greeting}>Bom dia, Consultor</Text>
        <Text style={styles.dealer}>Ford SP Centro · #{DEALER}</Text>
      </View>

      {share && (
        <View style={[styles.metricsRow, isTablet && styles.metricsRowTablet]}>
          <View style={[styles.metric, isSmall && styles.metricSmall]}>
            <Text style={[styles.metricLabel, isTablet && { fontSize: 12 }]}>Service Share</Text>
            <Text style={[styles.metricValue, { color: '#E67E22' }, isTablet && { fontSize: 28 }]}>
              {share.service_share}%
            </Text>
            <Text style={styles.metricSub}>Tendência: {share.tendencia}</Text>
          </View>
          <View style={[styles.metric, isSmall && styles.metricSmall]}>
            <Text style={[styles.metricLabel, isTablet && { fontSize: 12 }]}>Em risco hoje</Text>
            <Text style={[styles.metricValue, { color: '#E74C3C' }, isTablet && { fontSize: 28 }]}>
              {share.clientes_em_risco}
            </Text>
            <Text style={styles.metricSub}>clientes</Text>
          </View>
          <View style={[styles.metric, isSmall && styles.metricSmall]}>
            <Text style={[styles.metricLabel, isTablet && { fontSize: 12 }]}>VINs ativos</Text>
            <Text style={[styles.metricValue, { color: '#27AE60' }, isTablet && { fontSize: 28 }]}>
              {share.vins_ativos}
            </Text>
            <Text style={styles.metricSub}>de {share.total_vins}</Text>
          </View>
        </View>
      )}

      {roi && (
        <View style={styles.roiCard}>
          <View style={styles.roiLeft}>
            <Text style={styles.roiLabel}>Receita em risco hoje</Text>
            <Text style={styles.roiValue}>
              R$ {roi.receita_em_risco.toLocaleString('pt-BR', { minimumFractionDigits: 0 })}
            </Text>
            <Text style={styles.roiSub}>
              {roi.vins_em_risco} VINs · ticket médio R$ {roi.ticket_medio}
            </Text>
          </View>
          <View style={styles.roiRight}>
            <Text style={styles.roiRecLabel}>Recuperar 20%</Text>
            <Text style={styles.roiRec}>
              R$ {roi.recuperacao_20pct.toLocaleString('pt-BR', { minimumFractionDigits: 0 })}
            </Text>
          </View>
        </View>
      )}

      <Text style={styles.sectionTitle}>Prioridade de contato hoje</Text>

      <FlatList
        data={clientes}
        keyExtractor={(item) => item.vin_hash}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={carregar} />}
        contentContainerStyle={{ paddingBottom: 20 }}
        renderItem={({ item }) => {
          const cor = corScore(item.score_risco);
          return (
            <TouchableOpacity
              style={styles.card}
              onPress={() =>
                router.push({
                  pathname: '/cliente/[id]',
                  params: { id: item.vin_hash },
                })
              }
            >
              <View style={[styles.avatar, { backgroundColor: cor + '22' }]}>
                <Text style={[styles.avatarText, { color: cor }]}>
                  {item.perfil?.[0] ?? '?'}
                </Text>
              </View>
              <View style={styles.cardInfo}>
                <Text style={styles.cardName}>{item.modelo}</Text>
                <Text style={styles.cardDetail}>
                  {item.perfil} · {item.detalhe}
                </Text>
              </View>
              <View style={[styles.scorePill, { backgroundColor: cor + '22' }]}>
                <Text style={[styles.scoreText, { color: cor }]}>{item.score_risco}</Text>
              </View>
            </TouchableOpacity>
          );
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F7FA' },
  topbar: { backgroundColor: '#1F3A6E', padding: 16, paddingTop: 48 },
  greeting: { fontSize: 12, color: '#9BB3D4' },
  dealer: { fontSize: 20, fontWeight: '700', color: '#fff' },
  metricsRow: { flexDirection: 'row', gap: 8, padding: 12, paddingBottom: 0 },
  metricsRowTablet: { paddingHorizontal: 20, gap: 12 },
  metric: {
    flex: 1, backgroundColor: '#fff', borderRadius: 12, padding: 12,
    alignItems: 'center',
  },
  metricSmall: { padding: 8 },
  metricLabel: { fontSize: 10, color: '#888', marginBottom: 4, textAlign: 'center' },
  metricValue: { fontSize: 22, fontWeight: '700' },
  metricSub: { fontSize: 9, color: '#aaa', marginTop: 2 },
  roiCard: {
    backgroundColor: '#1F3A6E', borderRadius: 12,
    margin: 12, marginTop: 10, padding: 14,
    flexDirection: 'row', alignItems: 'center',
  },
  roiLeft: { flex: 1 },
  roiLabel: { fontSize: 10, color: '#9BB3D4', marginBottom: 2 },
  roiValue: { fontSize: 20, fontWeight: '900', color: '#fff' },
  roiSub: { fontSize: 10, color: '#9BB3D4', marginTop: 2 },
  roiRight: { alignItems: 'flex-end' },
  roiRecLabel: { fontSize: 10, color: '#9BB3D4', marginBottom: 2 },
  roiRec: { fontSize: 16, fontWeight: '700', color: '#00e676' },
  sectionTitle: {
    fontSize: 13, fontWeight: '700', color: '#1F3A6E',
    paddingHorizontal: 12, paddingBottom: 8,
  },
  card: {
    backgroundColor: '#fff', borderRadius: 12, padding: 12,
    marginHorizontal: 12, marginBottom: 8,
    flexDirection: 'row', alignItems: 'center', gap: 10,
  },
  avatar: {
    width: 40, height: 40, borderRadius: 20,
    alignItems: 'center', justifyContent: 'center',
  },
  avatarText: { fontSize: 14, fontWeight: '700' },
  cardInfo: { flex: 1 },
  cardName: { fontSize: 14, fontWeight: '700', color: '#222' },
  cardDetail: { fontSize: 11, color: '#888', marginTop: 2 },
  scorePill: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 20 },
  scoreText: { fontSize: 13, fontWeight: '700' },
});
