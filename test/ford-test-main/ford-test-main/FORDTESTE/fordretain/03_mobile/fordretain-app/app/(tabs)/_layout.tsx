import { useEffect, useState } from 'react';
import { View, ActivityIndicator, useWindowDimensions, Platform, Text } from 'react-native';
import { Tabs, router } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';

export default function TabsLayout() {
  const [verificando, setVerificando] = useState(true);
  const { width } = useWindowDimensions();
  const isTablet = width >= 768;

  useEffect(() => {
    AsyncStorage.getItem('fordretain_token').then((token) => {
      if (!token) {
        router.replace('/login');
      } else {
        setVerificando(false);
      }
    });
  }, []);

  if (verificando) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#fff' }}>
        <ActivityIndicator size="large" color="#1F3A6E" />
      </View>
    );
  }

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: '#1F3A6E',
        tabBarInactiveTintColor: '#aaa',
        tabBarStyle: {
          backgroundColor: '#fff',
          borderTopColor: '#E0E4EA',
          height: isTablet ? 68 : Platform.OS === 'ios' ? 80 : 60,
          paddingBottom: isTablet ? 12 : Platform.OS === 'ios' ? 20 : 8,
          paddingTop: 6,
          paddingHorizontal: isTablet ? 40 : 0,
        },
        tabBarLabelStyle: {
          fontSize: isTablet ? 13 : 10,
          fontWeight: '600',
          marginTop: 2,
        },
        tabBarItemStyle: {
          paddingVertical: 4,
        },
        tabBarShowIcon: false,
      }}
    >
      <Tabs.Screen name="index" options={{ title: 'Painel', tabBarIcon: () => null }} />
      <Tabs.Screen name="clientes" options={{ title: 'Clientes', tabBarIcon: () => null }} />
      <Tabs.Screen name="alertas" options={{ title: 'Alertas', tabBarIcon: () => null }} />
      <Tabs.Screen name="dashboard" options={{ title: 'Dashboard', tabBarIcon: () => null }} />
      <Tabs.Screen name="radar" options={{ title: 'Geo', tabBarIcon: () => null }} />
    </Tabs>
  );
}
