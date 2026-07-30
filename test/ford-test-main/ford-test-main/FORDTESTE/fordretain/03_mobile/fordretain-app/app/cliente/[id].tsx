import { useEffect, useState } from 'react';
import {
  View, Text, TouchableOpacity, StyleSheet,
  ScrollView, Linking, ActivityIndicator, Platform,
} from 'react-native';
import { useLocalSearchParams, router } from 'expo-router';
import { getScoreCliente, simularCampanha, registrarAcao } from '../../services/api';

const COR_PERFIL: Record<string, string> = {
  Fiel: '#27AE60',
  'Econômico': '#E67E22',
  Esquecido: '#3498DB',
  Abandono: '#E74C3C',
};

const SCRIPTS_PERFIL: Record<string, { titulo: string; script: string; dica: string }> = {
  Fiel: {
    titulo: 'Cliente Fiel — reforce o vínculo',
    script: 'Olá! Aqui é da concessionária Ford. Vimos que está próximo do seu próximo serviço e queremos garantir que sua experiência continue sendo a melhor. Tenho uma janela disponível essa semana — posso reservar para você?',
    dica: 'Mencione o histórico do cliente e ofereça agendamento prioritário.',
  },
  'Econômico': {
    titulo: 'Cliente Econômico — apresente o valor',
    script: 'Olá! Temos uma oferta exclusiva de revisão com 20% de desconto essa semana, só para clientes Ford. Sei que o custo importa — quero garantir que seu veículo fique em dia sem pesar no bolso.',
    dica: 'Informe o desconto logo no início. Tenha o valor final na ponta da língua.',
  },
  Esquecido: {
    titulo: 'Cliente Esquecido — remova a fricção',
    script: 'Oi! Percebemos que faz um tempo que não nos vemos. Sua revisão já está próxima e consigo agendar em menos de 5 minutos, sem precisar ir à loja. Que dia da semana funciona melhor?',
    dica: 'Ofereça datas concretas — não perguntas abertas. Facilite ao máximo.',
  },
  Abandono: {
    titulo: 'Cliente em risco — atue antes da garantia vencer',
    script: 'Olá! Seu veículo está entrando na fase final de garantia e quero garantir que você aproveite tudo a que tem direito. Posso agendar uma revisão completa ainda dentro da cobertura, sem custo adicional para os itens cobertos. Vale a pena conferir.',
    dica: '⚠ Urgência real: se a garantia vencer sem retorno, a chance de perder o cliente é quase total.',
  },
};

export default function PerfilCliente() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const [cliente, setCliente] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [showScript, setShowScript] = useState(false);
  const [showCampanha, setShowCampanha] = useState(false);
  const [campanha, setCampanha] = useState<any>(null);
  const [enviando, setEnviando] = useState(false);
  const [acaoRegistrada, setAcaoRegistrada] = useState(false);

  useEffect(() => {
    if (id) {
      getScoreCliente(id)
        .then((r) => setCliente(r.data))
        .finally(() => setLoading(false));
    }
  }, [id]);

  if (loading) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator size="large" color="#1F3A6E" />
        <Text style={{ marginTop: 8, color: '#888' }}>Carregando perfil...</Text>
      </View>
    );
  }

  if (!cliente) {
    return (
      <View style={styles.loading}>
        <Text style={{ color: '#E74C3C' }}>Erro ao carregar dados.</Text>
      </View>
    );
  }

  const cor = COR_PERFIL[cliente.perfil] ?? '#888';

  const abrirCampanha = async () => {
    setShowCampanha(true);
    setEnviando(true);
    setCampanha(null);
    try {
      const r = await simularCampanha(cliente.concessionaria_codigo, cliente.perfil);
      setCampanha(r.data);
    } catch {
      setCampanha(null);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <View style={{ flex: 1 }}>

      {/* Overlay: Script de abordagem */}
      {showScript && (
        <View style={styles.modalOverlay}>
          <TouchableOpacity style={StyleSheet.absoluteFillObject} onPress={() => setShowScript(false)} />
          <View style={styles.modalBox}>
            {(() => {
              const s = SCRIPTS_PERFIL[cliente.perfil] ?? SCRIPTS_PERFIL['Fiel'];
              const c = COR_PERFIL[cliente.perfil] ?? '#888';
              return (
                <>
                  <View style={[styles.modalHeader, { borderLeftColor: c }]}>
                    <Text style={[styles.modalTitulo, { color: c }]}>{s.titulo}</Text>
                  </View>
                  <Text style={styles.modalScriptLabel}>Script sugerido</Text>
                  <View style={styles.modalScriptBox}>
                    <Text style={styles.modalScript}>{s.script}</Text>
                  </View>
                  <View style={[styles.modalDicaBox, { backgroundColor: c + '15' }]}>
                    <Text style={[styles.modalDicaText, { color: c }]}>{s.dica}</Text>
                  </View>
                  <View style={styles.modalBtnRow}>
                    <TouchableOpacity style={styles.modalBtnCancel} onPress={() => setShowScript(false)}>
                      <Text style={styles.modalBtnCancelText}>Voltar</Text>
                    </TouchableOpacity>
                    <TouchableOpacity
                      style={[styles.modalBtnCall, { backgroundColor: c }]}
                      onPress={() => {
                        setShowScript(false);
                        registrarAcao(String(id), 'ligacao').catch(() => {});
                        Linking.openURL('tel:+5511999999999');
                      }}
                    >
                      <Text style={styles.modalBtnCallText}>Ligar agora</Text>
                    </TouchableOpacity>
                  </View>
                </>
              );
            })()}
          </View>
        </View>
      )}

      {/* Overlay: Simular campanha */}
      {showCampanha && (
        <View style={styles.modalOverlay}>
          <TouchableOpacity style={StyleSheet.absoluteFillObject} onPress={() => setShowCampanha(false)} />
          <View style={styles.modalBox}>
            <Text style={[styles.modalTitulo, { color: '#1F3A6E', marginBottom: 16 }]}>
              Simular campanha
            </Text>
            {enviando ? (
              <View style={{ alignItems: 'center', paddingVertical: 24 }}>
                <ActivityIndicator color="#1F3A6E" />
                <Text style={{ color: '#888', marginTop: 8, fontSize: 12 }}>Calculando impacto...</Text>
              </View>
            ) : campanha ? (
              <>
                <View style={styles.campanhaCard}>
                  <Text style={styles.campanhaLabel}>Clientes impactados</Text>
                  <Text style={styles.campanhaNum}>{campanha.disparos ?? campanha.total_impactados}</Text>
                  <Text style={styles.campanhaSub}>Perfil: {cliente.perfil}</Text>
                </View>
                <View style={[styles.campanhaCard, { backgroundColor: '#F0FBF4' }]}>
                  <Text style={styles.campanhaLabel}>Ação automática</Text>
                  <Text style={[styles.campanhaNum, { color: '#27AE60', fontSize: 18 }]}>
                    Oferta personalizada pronta para disparo
                  </Text>
                  <Text style={styles.campanhaSub}>
                    Em produção, esta mensagem seria enviada automaticamente pelo FordPass
                  </Text>
                </View>
                <View style={styles.modalBtnRow}>
                  <TouchableOpacity style={styles.modalBtnCancel} onPress={() => setShowCampanha(false)}>
                    <Text style={styles.modalBtnCancelText}>Cancelar</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    style={[styles.modalBtnCall, { backgroundColor: '#27AE60' }]}
                    onPress={async () => {
                      try { await registrarAcao(String(id), 'oferta', 'campanha_disparada'); } catch {}
                      setAcaoRegistrada(true);
                      setShowCampanha(false);
                    }}
                  >
                    <Text style={styles.modalBtnCallText}>Disparar oferta</Text>
                  </TouchableOpacity>
                </View>
              </>
            ) : (
              <View style={{ alignItems: 'center', paddingVertical: 24 }}>
                <Text style={{ color: '#E74C3C', fontSize: 13 }}>Erro ao calcular. Tente novamente.</Text>
                <TouchableOpacity style={{ marginTop: 12 }} onPress={() => setShowCampanha(false)}>
                  <Text style={{ color: '#888' }}>Fechar</Text>
                </TouchableOpacity>
              </View>
            )}
          </View>
        </View>
      )}

    <ScrollView style={styles.container}>
      <View style={[styles.topbar, { backgroundColor: '#1F3A6E' }]}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>← Voltar</Text>
        </TouchableOpacity>
        <Text style={styles.topTitle}>Perfil do cliente</Text>
        <Text style={styles.topSub}>Score e ação recomendada</Text>
      </View>

      <View style={styles.body}>
        <View style={styles.card}>
          <View style={styles.avatarWrap}>
            <Text style={styles.avatarText}>{cliente.modelo?.slice(0, 2)}</Text>
          </View>
          <View>
            <Text style={styles.modelName}>{cliente.modelo}</Text>
            <Text style={styles.vinText}>VIN: {id?.slice(0, 12)}...</Text>
            <Text style={styles.concText}>Conc. #{cliente.concessionaria_codigo}</Text>
          </View>
        </View>

        <View style={styles.scoreCard}>
          <View style={styles.scoreHeader}>
            <Text style={styles.scoreLabel}>Risco de abandono</Text>
            <Text style={[styles.scoreNum, { color: cor }]}>
              {cliente.score_risco} / 100
            </Text>
          </View>
          <View style={styles.barBg}>
            <View
              style={[
                styles.barFill,
                { width: `${cliente.score_risco}%` as any, backgroundColor: cor },
              ]}
            />
          </View>
          <View style={[styles.perfilBadge, { backgroundColor: cor + '22' }]}>
            <Text style={[styles.perfilText, { color: cor }]}>{cliente.perfil}</Text>
          </View>
        </View>

        <View style={styles.infoCard}>
          {[
            [
              'Última revisão',
              cliente.dias_desde_ultima_revisao
                ? `${cliente.dias_desde_ultima_revisao} dias atrás`
                : 'N/D',
            ],
            [
              'Quilometragem',
              cliente.km_atual
                ? `${Math.round(cliente.km_atual).toLocaleString('pt-BR')} km`
                : 'N/D',
            ],
            ['Garantia', cliente.garantia_expirada ? 'Expirada ⚠️' : 'Ativa ✓'],
            ['Total de revisões', String(cliente.total_revisoes)],
          ].map(([k, v]) => (
            <View key={k} style={styles.infoRow}>
              <Text style={styles.infoKey}>{k}</Text>
              <Text style={styles.infoVal}>{v}</Text>
            </View>
          ))}
        </View>

        <View style={[styles.actionBox, { borderLeftColor: cor, backgroundColor: cor + '11' }]}>
          <Text style={[styles.actionTitle, { color: cor }]}>Abordagem recomendada</Text>
          <Text style={styles.actionText}>{cliente.recomendacao}</Text>
        </View>

        {acaoRegistrada && (
          <View style={styles.sucessoBadge}>
            <Text style={styles.sucessoText}>✓ Oferta disparada com sucesso</Text>
          </View>
        )}

        <View style={styles.btnRow}>
          <TouchableOpacity
            style={styles.btnPrimary}
            onPress={() => setShowScript(true)}
          >
            <Text style={styles.btnPrimaryText}>Ligar agora</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.btnSecondary} onPress={abrirCampanha}>
            <Text style={styles.btnSecondaryText}>Enviar oferta</Text>
          </TouchableOpacity>
        </View>
      </View>

    </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F7FA' },
  loading: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  topbar: { padding: 16, paddingTop: 48 },
  backBtn: { marginBottom: 8 },
  backText: { color: '#9BB3D4', fontSize: 13 },
  topTitle: { fontSize: 18, fontWeight: '700', color: '#fff' },
  topSub: { fontSize: 12, color: '#9BB3D4' },
  body: { padding: 12 },
  card: {
    backgroundColor: '#fff', borderRadius: 12, padding: 14,
    flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 10,
  },
  avatarWrap: {
    width: 48, height: 48, borderRadius: 24, backgroundColor: '#D5E1F0',
    alignItems: 'center', justifyContent: 'center',
  },
  avatarText: { fontSize: 16, fontWeight: '700', color: '#1F3A6E' },
  modelName: { fontSize: 16, fontWeight: '700', color: '#222' },
  vinText: { fontSize: 11, color: '#888' },
  concText: { fontSize: 11, color: '#888' },
  scoreCard: { backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 10 },
  scoreHeader: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  scoreLabel: { fontSize: 12, color: '#888' },
  scoreNum: { fontSize: 15, fontWeight: '700' },
  barBg: { backgroundColor: '#F0F0F0', borderRadius: 6, height: 10, marginBottom: 12 },
  barFill: { height: 10, borderRadius: 6 },
  perfilBadge: {
    alignSelf: 'flex-start', paddingHorizontal: 14, paddingVertical: 5, borderRadius: 20,
  },
  perfilText: { fontSize: 12, fontWeight: '700' },
  infoCard: { backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 10 },
  infoRow: {
    flexDirection: 'row', justifyContent: 'space-between',
    paddingVertical: 8, borderBottomWidth: 1, borderBottomColor: '#F5F5F5',
  },
  infoKey: { fontSize: 12, color: '#888' },
  infoVal: { fontSize: 12, fontWeight: '700', color: '#222' },
  actionBox: { borderLeftWidth: 4, borderRadius: 10, padding: 14, marginBottom: 10 },
  actionTitle: { fontSize: 12, fontWeight: '700', marginBottom: 6 },
  actionText: { fontSize: 12, color: '#555', lineHeight: 18 },
  sucessoBadge: {
    backgroundColor: '#E8F8EE', borderRadius: 8, padding: 10,
    marginBottom: 10, flexDirection: 'row', alignItems: 'center',
  },
  sucessoText: { color: '#27AE60', fontWeight: '700', fontSize: 12 },
  btnRow: { flexDirection: 'row', gap: 10 },
  btnPrimary: {
    flex: 1, backgroundColor: '#1F3A6E', borderRadius: 10,
    padding: 14, alignItems: 'center',
  },
  btnPrimaryText: { color: '#fff', fontWeight: '700', fontSize: 13 },
  btnSecondary: {
    flex: 1, borderWidth: 1.5, borderColor: '#1F3A6E',
    borderRadius: 10, padding: 14, alignItems: 'center',
  },
  btnSecondaryText: { color: '#1F3A6E', fontWeight: '700', fontSize: 13 },
  modalOverlay: {
    position: Platform.OS === 'web' ? ('fixed' as any) : 'absolute',
    top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.55)',
    justifyContent: 'flex-end',
    zIndex: 999,
  },
  modalBox: {
    backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20,
    padding: 20, paddingBottom: 36,
  },
  modalHeader: { borderLeftWidth: 4, paddingLeft: 10, marginBottom: 16 },
  modalTitulo: { fontSize: 14, fontWeight: '700' },
  modalScriptLabel: { fontSize: 11, color: '#888', marginBottom: 6, textTransform: 'uppercase', letterSpacing: 0.5 },
  modalScriptBox: { backgroundColor: '#F5F7FA', borderRadius: 10, padding: 14, marginBottom: 12 },
  modalScript: { fontSize: 13, color: '#222', lineHeight: 20 },
  modalDicaBox: { borderRadius: 8, padding: 12, marginBottom: 20 },
  modalDicaText: { fontSize: 12, fontWeight: '600', lineHeight: 18 },
  modalBtnRow: { flexDirection: 'row', gap: 10 },
  modalBtnCancel: {
    flex: 1, borderWidth: 1.5, borderColor: '#DDD',
    borderRadius: 10, padding: 14, alignItems: 'center',
  },
  modalBtnCancelText: { color: '#888', fontWeight: '600', fontSize: 13 },
  modalBtnCall: { flex: 2, borderRadius: 10, padding: 14, alignItems: 'center' },
  modalBtnCallText: { color: '#fff', fontWeight: '700', fontSize: 13 },
  campanhaCard: {
    backgroundColor: '#F0F4FB', borderRadius: 10, padding: 14, marginBottom: 12,
  },
  campanhaLabel: { fontSize: 12, color: '#888', marginBottom: 4 },
  campanhaNum: { fontSize: 26, fontWeight: '900', color: '#1F3A6E' },
  campanhaSub: { fontSize: 11, color: '#888', marginTop: 4 },
});
