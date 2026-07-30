import { TouchableOpacity, View, Text, StyleSheet } from 'react-native';

interface RiskCardProps {
  vin_hash: string;
  score_risco: number;
  perfil: string;
  modelo: string;
  detalhe: string;
  onPress: () => void;
}

const corScore = (score: number) =>
  score >= 80 ? '#E74C3C' : score >= 60 ? '#E67E22' : '#27AE60';

export default function RiskCard({
  score_risco, perfil, modelo, detalhe, onPress,
}: RiskCardProps) {
  const cor = corScore(score_risco);
  return (
    <TouchableOpacity style={styles.card} onPress={onPress}>
      <View style={[styles.avatar, { backgroundColor: cor + '22' }]}>
        <Text style={[styles.avatarText, { color: cor }]}>{perfil?.[0] ?? '?'}</Text>
      </View>
      <View style={styles.info}>
        <Text style={styles.name}>{modelo}</Text>
        <Text style={styles.detail}>{perfil} · {detalhe}</Text>
      </View>
      <View style={[styles.pill, { backgroundColor: cor + '22' }]}>
        <Text style={[styles.pillText, { color: cor }]}>{score_risco}</Text>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#fff', borderRadius: 12, padding: 12,
    flexDirection: 'row', alignItems: 'center', gap: 10,
  },
  avatar: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  avatarText: { fontSize: 14, fontWeight: '700' },
  info: { flex: 1 },
  name: { fontSize: 14, fontWeight: '700', color: '#222' },
  detail: { fontSize: 11, color: '#888', marginTop: 2 },
  pill: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 20 },
  pillText: { fontSize: 13, fontWeight: '700' },
});
