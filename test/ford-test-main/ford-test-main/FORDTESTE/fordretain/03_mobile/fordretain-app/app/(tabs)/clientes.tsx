import { useEffect, useState } from 'react';
import {
  View, Text, FlatList, TextInput,
  StyleSheet, TouchableOpacity,
} from 'react-native';
import { getClientesEmRisco } from '../../services/api';
import { router } from 'expo-router';

const corScore = (score: number) =>
  score >= 80 ? '#E74C3C' : score >= 60 ? '#E67E22' : '#27AE60';

export default function ClientesScreen() {
  const [clientes, setClientes] = useState<any[]>([]);
  const [busca, setBusca] = useState('');

  useEffect(() => {
    getClientesEmRisco(1009, 50).then((r) => setClientes(r.data.clientes));
  }, []);

  const filtrados = clientes.filter(
    (c) =>
      c.modelo.toLowerCase().includes(busca.toLowerCase()) ||
      c.perfil.toLowerCase().includes(busca.toLowerCase())
  );

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Clientes em risco</Text>
      </View>
      <TextInput
        style={styles.busca}
        placeholder="Buscar por modelo ou perfil..."
        value={busca}
        onChangeText={setBusca}
      />
      <FlatList
        data={filtrados}
        keyExtractor={(item) => item.vin_hash}
        renderItem={({ item }) => {
          const cor = corScore(item.score_risco);
          return (
            <TouchableOpacity
              style={styles.row}
              onPress={() =>
                router.push({ pathname: '/cliente/[id]', params: { id: item.vin_hash } })
              }
            >
              <View style={[styles.badge, { backgroundColor: cor + '22' }]}>
                <Text style={[styles.badgeText, { color: cor }]}>{item.score_risco}</Text>
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.rowName}>{item.modelo}</Text>
                <Text style={styles.rowSub}>{item.perfil} · {item.detalhe}</Text>
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
  header: { backgroundColor: '#1F3A6E', padding: 16, paddingTop: 48 },
  title: { fontSize: 20, fontWeight: '700', color: '#fff' },
  busca: {
    margin: 12, padding: 10, backgroundColor: '#fff',
    borderRadius: 10, borderWidth: 1, borderColor: '#E0E4EA',
  },
  row: {
    backgroundColor: '#fff', borderRadius: 10, padding: 12,
    marginHorizontal: 12, marginBottom: 6,
    flexDirection: 'row', alignItems: 'center', gap: 12,
  },
  badge: { width: 48, height: 48, borderRadius: 10, alignItems: 'center', justifyContent: 'center' },
  badgeText: { fontWeight: '700', fontSize: 15 },
  rowName: { fontWeight: '700', color: '#222' },
  rowSub: { fontSize: 11, color: '#888', marginTop: 2 },
});
