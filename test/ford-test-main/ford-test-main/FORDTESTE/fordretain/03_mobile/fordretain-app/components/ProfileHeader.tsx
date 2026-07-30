import { View, Text, StyleSheet } from 'react-native';

interface ProfileHeaderProps {
  modelo: string;
  vinHash: string;
  concessionaria: number;
}

export default function ProfileHeader({ modelo, vinHash, concessionaria }: ProfileHeaderProps) {
  return (
    <View style={styles.card}>
      <View style={styles.avatar}>
        <Text style={styles.avatarText}>{modelo?.slice(0, 2) ?? 'VH'}</Text>
      </View>
      <View>
        <Text style={styles.name}>{modelo}</Text>
        <Text style={styles.sub}>VIN: {vinHash?.slice(0, 12)}...</Text>
        <Text style={styles.sub}>Conc. #{concessionaria}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#fff', borderRadius: 12, padding: 14,
    flexDirection: 'row', alignItems: 'center', gap: 12,
  },
  avatar: {
    width: 48, height: 48, borderRadius: 24, backgroundColor: '#D5E1F0',
    alignItems: 'center', justifyContent: 'center',
  },
  avatarText: { fontSize: 16, fontWeight: '700', color: '#1F3A6E' },
  name: { fontSize: 16, fontWeight: '700', color: '#222' },
  sub: { fontSize: 11, color: '#888', marginTop: 1 },
});
