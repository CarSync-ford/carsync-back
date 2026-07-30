import { View, Text, StyleSheet } from 'react-native';

interface ScoreBarProps {
  score: number;
  showLabel?: boolean;
}

const corScore = (score: number) =>
  score >= 80 ? '#E74C3C' : score >= 60 ? '#E67E22' : '#27AE60';

export default function ScoreBar({ score, showLabel = true }: ScoreBarProps) {
  const cor = corScore(score);
  return (
    <View style={styles.container}>
      {showLabel && (
        <View style={styles.row}>
          <Text style={styles.label}>Risco de abandono</Text>
          <Text style={[styles.value, { color: cor }]}>{score}/100</Text>
        </View>
      )}
      <View style={styles.barBg}>
        <View style={[styles.barFill, { width: `${score}%` as any, backgroundColor: cor }]} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { marginVertical: 4 },
  row: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 },
  label: { fontSize: 11, color: '#888' },
  value: { fontSize: 11, fontWeight: '700' },
  barBg: { backgroundColor: '#F0F0F0', borderRadius: 5, height: 8 },
  barFill: { height: 8, borderRadius: 5 },
});
