import pandas as pd
import numpy as np
from datetime import datetime

def carregar_dados(caminho: str) -> pd.DataFrame:
    df = pd.read_excel(caminho)
    colunas_data = ['ServiceDate', 'SalesDate', 'DeliveryDate',
                    'WarrantyStartDate', 'ServiceOpenDate', 'ServiceClosedDate']
    for col in colunas_data:
        if col in df.columns:
            df[col] = pd.to_datetime(df[col], errors='coerce', dayfirst=False)
    return df

def construir_base1(df: pd.DataFrame) -> pd.DataFrame:
    """
    Base 1: histórico COMPLETO por VIN.
    Usada para clustering — inclui variáveis pós-compra.
    """
    grupos = df.groupby('VIN_Hash')
    REF = datetime(2025, 9, 30)

    base1 = pd.DataFrame({
        'VIN_Hash': list(grupos.groups.keys()),
        'total_revisoes': grupos['MaintenanceNumber'].max().values,
        'km_atual': grupos['KM'].max().values,
        'km_medio_por_revisao': grupos['KM'].mean().values,
        'intervalo_medio_dias': grupos.apply(
            lambda g: g.sort_values('ServiceDate')['ServiceDate'].diff().dt.days.mean()
        ).values,
        'dias_desde_ultima_revisao': grupos['ServiceDate'].max().apply(
            lambda d: (REF - d).days if pd.notna(d) else None
        ).values,
        'modelo': grupos['ModelName'].first().values,
        'ano_modelo': grupos['ModelYear'].first().values,
        'concessionaria': grupos['DealerCode'].first().values,
        'data_venda': grupos['SalesDate'].first().values,
        'inicio_garantia': grupos['WarrantyStartDate'].first().values,
    })

    base1['idade_meses'] = base1['data_venda'].apply(
        lambda d: ((REF - d).days / 30) if pd.notna(d) else None
    )
    base1['abandonou_apos_1a'] = (base1['total_revisoes'] == 1).astype(int)
    base1['garantia_expirada'] = base1['inicio_garantia'].apply(
        lambda d: 1 if pd.notna(d) and (REF - d).days > 365 else 0
    )

    return base1.dropna(subset=['total_revisoes'])

def construir_base2(df: pd.DataFrame) -> pd.DataFrame:
    """
    Base 2: apenas variáveis disponíveis NO MOMENTO DA COMPRA.
    Usada para classificação preditiva — PROIBIDO usar pós-compra.
    """
    grupos = df.groupby('VIN_Hash').first().reset_index()

    base2 = pd.DataFrame({
        'VIN_Hash': grupos['VIN_Hash'],
        'modelo': grupos['ModelName'],
        'ano_modelo': grupos['ModelYear'],
        'concessionaria': grupos['DealerCode'],
        'data_venda': pd.to_datetime(grupos['SalesDate'], errors='coerce', dayfirst=True),
        'data_entrega': pd.to_datetime(grupos['DeliveryDate'], errors='coerce', dayfirst=True),
        'inicio_garantia': pd.to_datetime(grupos['WarrantyStartDate'], errors='coerce', dayfirst=True),
    })

    base2['dias_venda_entrega'] = (base2['data_entrega'] - base2['data_venda']).dt.days.fillna(0)
    base2['mes_compra'] = base2['data_venda'].dt.month
    base2['trimestre_compra'] = base2['data_venda'].dt.quarter

    return base2
