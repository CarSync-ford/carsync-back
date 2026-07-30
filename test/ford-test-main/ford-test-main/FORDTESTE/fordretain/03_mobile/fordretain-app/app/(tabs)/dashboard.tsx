import { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, Platform, useWindowDimensions } from 'react-native';
import { getServiceShare } from '../../services/api';
import { DASHBOARD_HTML } from '../../constants/dashboardHtml';

const DEALER = 1009;

const PERFIS_MOCK = [
  { nome: 'Fiel', valor: 32, cor: '#27AE60' },
  { nome: 'Econômico', valor: 25, cor: '#E67E22' },
  { nome: 'Esquecido', valor: 28, cor: '#3498DB' },
  { nome: 'Abandono', valor: 15, cor: '#E74C3C' },
];

export default function DashboardScreen() {
  const [share, setShare] = useState<any>(null);
  const { width } = useWindowDimensions();
  const isTablet = width >= 768;

  // No web, cria um blob URL com o HTML para o iframe carregar sem restrições de CDN
  const [blobUrl, setBlobUrl] = useState('');
  useEffect(() => {
    if (Platform.OS === 'web') {
      const blob = new Blob([DASHBOARD_HTML], { type: 'text/html' });
      const url = URL.createObjectURL(blob);
      setBlobUrl(url);
      return () => URL.revokeObjectURL(url);
    }
  }, []);

  if (Platform.OS === 'web') {
    return (
      <View style={{ flex: 1 }}>
        {/* @ts-ignore — iframe é válido no react-native-web */}
        {blobUrl ? (
          <iframe
            src={blobUrl}
            style={{ width: '100%', height: '100%', border: 'none', display: 'block' }}
            title="FordRetain Dashboard"
          />
        ) : null}
      </View>
    );
  }

  useEffect(() => {
    getServiceShare(DEALER).then((r) => setShare(r.data));
  }, []);

  return (
    <ScrollView style={styles.container} contentContainerStyle={isTablet && styles.tabletContent}>
      <View style={[styles.header, isTablet && styles.headerTablet]}>
        <Text style={[styles.title, isTablet && { fontSize: 26 }]}>Dashboard</Text>
        <Text style={styles.sub}>Visão geral da concessionária #{DEALER}</Text>
      </View>

      <View style={isTablet ? styles.tabletRow : undefined}>
        {share && (
          <View style={[styles.section, isTablet && styles.sectionTablet]}>
            <Text style={styles.sectionTitle}>Métricas atuais</Text>
            {[
              ['Service Share', `${share.service_share}%`, '#E67E22'],
              ['VINs totais', `${share.total_vins}`, '#1F3A6E'],
              ['VINs ativos', `${share.vins_ativos}`, '#27AE60'],
              ['Em risco', `${share.clientes_em_risco}`, '#E74C3C'],
              ['Tendência', share.tendencia.toUpperCase(), '#3498DB'],
            ].map(([k, v, cor]) => (
              <View key={k} style={styles.metricRow}>
                <Text style={[styles.metricKey, isTablet && { fontSize: 14 }]}>{k}</Text>
                <Text style={[styles.metricVal, { color: cor }, isTablet && { fontSize: 15 }]}>{v}</Text>
              </View>
            ))}
          </View>
        )}

        <View style={[styles.section, isTablet && styles.sectionTablet]}>
          <Text style={styles.sectionTitle}>Distribuição por perfil</Text>
          {PERFIS_MOCK.map((p) => (
            <View key={p.nome} style={styles.perfilRow}>
              <Text style={[styles.perfilNome, isTablet && { width: 100, fontSize: 13 }]}>{p.nome}</Text>
              <View style={styles.barBg}>
                <View
                  style={[styles.barFill, { width: `${p.valor}%`, backgroundColor: p.cor }]}
                />
              </View>
              <Text style={[styles.perfilPct, { color: p.cor }, isTablet && { fontSize: 13 }]}>{p.valor}%</Text>
            </View>
          ))}
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Regras de automação ativas</Text>
        {[
          'Score ≥ 75 → disparo automático de oferta personalizada',
          'Garantia vencendo em 30 dias → alerta ao consultor responsável',
          'Revisão vencida há +45 dias → lembrete humanizado por WhatsApp',
        ].map((regra) => (
          <View key={regra} style={styles.regraRow}>
            <Text style={styles.regraDot}>•</Text>
            <Text style={[styles.regraText, isTablet && { fontSize: 14 }]}>{regra}</Text>
          </View>
        ))}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F7FA' },
  tabletContent: { maxWidth: 900, alignSelf: 'center', width: '100%' },
  header: { backgroundColor: '#1F3A6E', padding: 16, paddingTop: 48 },
  headerTablet: { paddingHorizontal: 28, paddingTop: 32 },
  title: { fontSize: 20, fontWeight: '700', color: '#fff' },
  sub: { fontSize: 12, color: '#9BB3D4', marginTop: 2 },
  tabletRow: { flexDirection: 'row', alignItems: 'flex-start' },
  section: { backgroundColor: '#fff', borderRadius: 12, margin: 12, padding: 14 },
  sectionTablet: { flex: 1, margin: 8 },
  sectionTitle: { fontSize: 13, fontWeight: '700', color: '#1F3A6E', marginBottom: 12 },
  metricRow: {
    flexDirection: 'row', justifyContent: 'space-between',
    paddingVertical: 8, borderBottomWidth: 1, borderBottomColor: '#F5F5F5',
  },
  metricKey: { fontSize: 13, color: '#666' },
  metricVal: { fontSize: 13, fontWeight: '700' },
  perfilRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 10 },
  perfilNome: { width: 80, fontSize: 12, color: '#444' },
  barBg: { flex: 1, backgroundColor: '#F0F0F0', borderRadius: 4, height: 10, marginHorizontal: 8 },
  barFill: { height: 10, borderRadius: 4 },
  perfilPct: { width: 36, fontSize: 12, fontWeight: '700', textAlign: 'right' },
  regraRow: { flexDirection: 'row', marginBottom: 8 },
  regraDot: { color: '#1F3A6E', fontWeight: '700', marginRight: 6 },
  regraText: { flex: 1, fontSize: 12, color: '#555', lineHeight: 18 },
});
