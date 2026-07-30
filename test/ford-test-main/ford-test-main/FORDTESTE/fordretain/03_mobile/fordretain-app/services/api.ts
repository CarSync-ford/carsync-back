import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { router } from 'expo-router';
import { Platform } from 'react-native';

// No Vercel/web sem backend, usa dados demo
const DEMO_MODE = Platform.OS === 'web';

// ====== MOCK DATA ======
const MOCK_CLIENTES = [
  { vin_hash: 'vin_001', modelo: 'RANGER 2023', perfil: 'Abandono', score_risco: 94, detalhe: '218 dias sem revisão' },
  { vin_hash: 'vin_002', modelo: 'MAVERICK 2024', perfil: 'Abandono', score_risco: 81, detalhe: '145 dias sem revisão' },
  { vin_hash: 'vin_003', modelo: 'BRONCO SPORT 2022', perfil: 'Esquecido', score_risco: 78, detalhe: 'Garantia vence em 30 dias' },
  { vin_hash: 'vin_004', modelo: 'TERRITORY 2023', perfil: 'Esquecido', score_risco: 71, detalhe: 'Última revisão: Jun/24' },
  { vin_hash: 'vin_005', modelo: 'TRANSIT 2022', perfil: 'Econômico', score_risco: 68, detalhe: 'Sensível a preço' },
  { vin_hash: 'vin_006', modelo: 'F-150 2023', perfil: 'Econômico', score_risco: 63, detalhe: 'Revisão vencida 45 dias' },
  { vin_hash: 'vin_007', modelo: 'KA 2022', perfil: 'Fiel', score_risco: 38, detalhe: 'Retorna regularmente' },
  { vin_hash: 'vin_008', modelo: 'ECOSPORT 2021', perfil: 'Fiel', score_risco: 29, detalhe: '5 revisões na rede' },
  { vin_hash: 'vin_009', modelo: 'MUSTANG 2023', perfil: 'Abandono', score_risco: 87, detalhe: '190 dias sem revisão' },
  { vin_hash: 'vin_010', modelo: 'RANGER 2022', perfil: 'Esquecido', score_risco: 74, detalhe: 'Fora do ciclo esperado' },
];

const MOCK_SERVICE_SHARE = {
  service_share: 67.3,
  total_vins: 1247,
  vins_ativos: 839,
  clientes_em_risco: 43,
  tendencia: 'queda',
};

const MOCK_ROI = {
  receita_em_risco: 89680,
  vins_em_risco: 43,
  ticket_medio: 380,
  recuperacao_20pct: 17936,
};

const MOCK_SCORE = {
  vin_hash: '',
  score_risco: 81,
  perfil: 'Abandono',
  dias_sem_revisao: 145,
  total_revisoes: 1,
  modelo: 'MAVERICK 2024',
  recomendacao: 'Contatar antes do 5° mês pós-compra com oferta especial',
};

// ====== API REAL ======
const BASE_URL = 'http://localhost:8000';
const api = axios.create({ baseURL: BASE_URL });

api.interceptors.request.use(async (config) => {
  const token = await AsyncStorage.getItem('fordretain_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error?.response?.status === 401) {
      await AsyncStorage.removeItem('fordretain_token');
      router.replace('/login');
    }
    return Promise.reject(error);
  }
);

// ====== EXPORTS ======
export const login = async (email: string, senha: string) => {
  if (DEMO_MODE) {
    if (email === 'consultor@ford.com.br' && senha === 'secret') {
      await AsyncStorage.setItem('fordretain_token', 'demo-token');
      return { access_token: 'demo-token' };
    }
    throw new Error('Credenciais inválidas');
  }
  const { data } = await api.post('/auth/login', { email, senha });
  await AsyncStorage.setItem('fordretain_token', data.access_token);
  return data;
};

export const logout = async () => {
  await AsyncStorage.removeItem('fordretain_token');
  router.replace('/login');
};

const mock = (data: any) => Promise.resolve({ data });

export const getClientesEmRisco = (concessionaria: number, limite = 20, scoreMinimo = 60) =>
  DEMO_MODE
    ? mock({ clientes: MOCK_CLIENTES.filter(c => c.score_risco >= scoreMinimo).slice(0, limite) })
    : api.get(`/clientes/risco?concessionaria=${concessionaria}&limite=${limite}&score_minimo=${scoreMinimo}`);

export const getScoreCliente = (vin_hash: string) =>
  DEMO_MODE
    ? mock({ ...MOCK_SCORE, vin_hash })
    : api.get(`/scores/${vin_hash}`);

export const getServiceShare = (dealer_code: number) =>
  DEMO_MODE
    ? mock(MOCK_SERVICE_SHARE)
    : api.get(`/concessionarias/${dealer_code}/service-share`);

export const getRoiConcessionaria = (dealer_code: number) =>
  DEMO_MODE
    ? mock(MOCK_ROI)
    : api.get(`/concessionarias/${dealer_code}/roi`);

export const simularCampanha = (concessionaria: number, perfil?: string, score_minimo = 60) =>
  DEMO_MODE
    ? mock({ disparos: 12, perfil, score_minimo })
    : api.post('/campanhas/simular', { concessionaria, perfil, score_minimo });

export const registrarAcao = (vin_hash: string, tipo: string, resultado?: string, observacao?: string) =>
  DEMO_MODE
    ? mock({ ok: true })
    : api.post(`/acoes/${vin_hash}`, { tipo, resultado, observacao });

export default api;
