import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report
import joblib
import os

FEATURES_BASE2 = [
    'modelo_enc',
    'ano_modelo',
    'mes_compra',
    'trimestre_compra',
    'dias_venda_entrega',
]

def calcular_score_risco(modelo_clf, X_input: pd.DataFrame) -> np.ndarray:
    """Retorna score de risco 0-100 (100 = maior risco de abandono)."""
    proba = modelo_clf.predict_proba(X_input)
    classes = list(modelo_clf.classes_)
    idx = classes.index('Abandono') if 'Abandono' in classes else 0
    return (proba[:, idx] * 100).round(0).astype(int)

def treinar_classificador(base2: pd.DataFrame, base1_perfis: pd.DataFrame, output_dir: str = '../models'):
    le_modelo = LabelEncoder()
    base2 = base2.copy()
    base2['modelo_enc'] = le_modelo.fit_transform(base2['modelo'].fillna('Desconhecido'))

    df = base2.reset_index(drop=True).copy()
    df['perfil'] = base1_perfis.reset_index(drop=True)['perfil'].values

    df = df.dropna(subset=['perfil', 'mes_compra'])

    X = df[FEATURES_BASE2].fillna(0)
    y = df['perfil']

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    clf = RandomForestClassifier(n_estimators=200, random_state=42, n_jobs=-1)
    clf.fit(X_train, y_train)

    y_pred = clf.predict(X_test)
    print(classification_report(y_test, y_pred))

    os.makedirs(output_dir, exist_ok=True)
    joblib.dump(clf, f'{output_dir}/classificador_perfil.pkl')
    joblib.dump(le_modelo, f'{output_dir}/encoder_modelo.pkl')

    return clf, le_modelo, X_test, y_test
