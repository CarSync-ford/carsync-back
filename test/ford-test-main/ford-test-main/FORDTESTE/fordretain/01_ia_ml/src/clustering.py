import pandas as pd
import numpy as np
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
from sklearn.metrics import silhouette_score
import joblib
import os

FEATURES = [
    'total_revisoes',
    'intervalo_medio_dias',
    'dias_desde_ultima_revisao',
    'km_atual',
    'idade_meses',
    'garantia_expirada',
    'abandonou_apos_1a',
]

MAPA_PERFIS = {
    0: 'Fiel',
    1: 'Econômico',
    2: 'Esquecido',
    3: 'Abandono',
}

def treinar_clustering(base1: pd.DataFrame, k: int = 4, output_dir: str = '../models'):
    X = base1[FEATURES].dropna()
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)

    kmeans = KMeans(n_clusters=k, random_state=42, n_init=10)
    labels = kmeans.fit_predict(X_scaled)

    resultado = X.copy()
    resultado['cluster'] = labels
    resultado['perfil'] = resultado['cluster'].map(MAPA_PERFIS)

    os.makedirs(output_dir, exist_ok=True)
    joblib.dump(kmeans, f'{output_dir}/kmeans_perfis.pkl')
    joblib.dump(scaler, f'{output_dir}/scaler_base1.pkl')
    resultado.to_csv(f'{output_dir}/base1_com_perfis.csv', index=False)

    return resultado, kmeans, scaler

def escolher_k(X_scaled: np.ndarray, k_range=range(2, 8)):
    inertias, silhouettes = [], []
    for k in k_range:
        km = KMeans(n_clusters=k, random_state=42, n_init=10)
        km.fit(X_scaled)
        inertias.append(km.inertia_)
        silhouettes.append(silhouette_score(X_scaled, km.labels_))
    return list(k_range), inertias, silhouettes
