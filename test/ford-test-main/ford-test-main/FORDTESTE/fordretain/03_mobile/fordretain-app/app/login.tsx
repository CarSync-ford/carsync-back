import { useState } from 'react';
import {
  View, Text, TextInput, TouchableOpacity,
  StyleSheet, Alert, KeyboardAvoidingView, Platform,
} from 'react-native';
import { login } from '../services/api';
import { router } from 'expo-router';

export default function LoginScreen() {
  const [email, setEmail] = useState('consultor@ford.com.br');
  const [senha, setSenha] = useState('secret');
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    if (!email || !senha) {
      Alert.alert('Erro', 'Preencha e-mail e senha.');
      return;
    }
    try {
      setLoading(true);
      await login(email, senha);
      router.replace('/(tabs)');
    } catch {
      Alert.alert('Erro', 'Credenciais inválidas. Tente novamente.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={styles.logoCircle}>
        <Text style={styles.logoText}>FR</Text>
      </View>
      <Text style={styles.title}>FordRetain</Text>
      <Text style={styles.subtitle}>Acesso para consultores e gestores</Text>

      <TextInput
        style={styles.input}
        placeholder="E-mail corporativo"
        value={email}
        onChangeText={setEmail}
        keyboardType="email-address"
        autoCapitalize="none"
        autoCorrect={false}
      />
      <TextInput
        style={styles.input}
        placeholder="Senha"
        value={senha}
        onChangeText={setSenha}
        secureTextEntry
      />

      <TouchableOpacity style={styles.btn} onPress={handleLogin} disabled={loading}>
        <Text style={styles.btnText}>{loading ? 'Entrando...' : 'Entrar'}</Text>
      </TouchableOpacity>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1, padding: 24, justifyContent: 'center', backgroundColor: '#fff',
  },
  logoCircle: {
    width: 72, height: 72, borderRadius: 36, backgroundColor: '#1F3A6E',
    alignItems: 'center', justifyContent: 'center', alignSelf: 'center', marginBottom: 16,
  },
  logoText: { color: '#fff', fontSize: 20, fontWeight: '700' },
  title: {
    fontSize: 26, fontWeight: '700', color: '#1F3A6E',
    textAlign: 'center', marginBottom: 6,
  },
  subtitle: {
    fontSize: 13, color: '#888', textAlign: 'center', marginBottom: 32,
  },
  input: {
    borderWidth: 1.5, borderColor: '#E0E4EA', borderRadius: 10,
    padding: 13, fontSize: 15, marginBottom: 14, backgroundColor: '#F5F7FA',
  },
  btn: {
    backgroundColor: '#1F3A6E', borderRadius: 10, padding: 15, alignItems: 'center',
  },
  btnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
});
