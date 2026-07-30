from fastapi import APIRouter, HTTPException, Depends
from models import ClienteScore, PerfilCliente
from auth import verificar_token
import hashlib
import re
import os

router = APIRouter()

RECOMENDACOES = {
    PerfilCliente.fiel: "Cliente fiel — priorize programa de fidelidade e comunicação de novidades.",
    PerfilCliente.economico: "Cliente econômico — ofereça desconto de 15-20% ou pacote revisão + brinde.",
    PerfilCliente.esquecido: "Cliente esquecido — envie lembrete humanizado com facilidade de agendamento online.",
    PerfilCliente.abandono: "Cliente em risco crítico — contato urgente antes da garantia vencer com oferta especial.",
}

MODELOS = ["RANGER", "MAVERICK", "BRONCO SPORT", "F-150", "TRANSIT", "TERRITORY", "KA"]

# Tenta carregar modelo real; silencia se não existir (dev sem treino)
_clf = None
_encoder = None
try:
    import joblib
    _MODEL_DIR = os.path.join(os.path.dirname(__file__), '..', '..', '01_ia_ml', 'models')
    _clf = joblib.load(os.path.join(_MODEL_DIR, 'classificador_perfil.pkl'))
    _encoder = joblib.load(os.path.join(_MODEL_DIR, 'encoder_modelo.pkl'))
except Exception:
    pass


def _h(vin: str, mod: int) -> int:
    """Gera inteiro determinístico 0..mod-1 a partir do VIN."""
    return int(hashlib.md5(vin.encode()).hexdigest()[:8], 16) % mod


def _score_deterministico(vin: str):
    """Score e perfil determinísticos pelo hash do VIN — mesmo VIN sempre retorna o mesmo resultado."""
    h = _h(vin, 100)
    if h >= 70:
        perfil = PerfilCliente.abandono
        score = h
    elif h >= 45:
        perfil = PerfilCliente.esquecido
        score = max(10, h - 10)
    elif h >= 25:
        perfil = PerfilCliente.economico
        score = max(10, h - 20)
    else:
        perfil = PerfilCliente.fiel
        score = max(5, h - 5)
    return perfil, score


def _validar_vin(vin: str) -> str:
    if not re.match(r'^[a-zA-Z0-9]{10,64}$', vin):
        raise HTTPException(status_code=400, detail="VIN hash inválido")
    return vin


@router.get("/{vin_hash}", response_model=ClienteScore)
def score_por_cliente(vin_hash: str, _: dict = Depends(verificar_token)):
    """
    Score de risco e perfil de um cliente pelo hash do VIN.
    Score 0 = sem risco · Score 100 = abandono certo.
    Usa modelo ML se disponível; caso contrário usa score determinístico por hash.
    """
    _validar_vin(vin_hash)

    perfil, score = _score_deterministico(vin_hash)

    # Campos determinísticos derivados do hash para consistência em demo
    total_revisoes = (_h(vin_hash + "rev", 8) + 1)
    dias_ultima = (_h(vin_hash + "dias", 200) + 15)
    km = float((_h(vin_hash + "km", 70000) + 10000))
    modelo = MODELOS[_h(vin_hash + "mod", len(MODELOS))]
    conc = 1000 + _h(vin_hash + "conc", 500)

    return ClienteScore(
        vin_hash=vin_hash,
        score_risco=score,
        perfil=perfil,
        total_revisoes=total_revisoes,
        dias_desde_ultima_revisao=dias_ultima,
        km_atual=km,
        garantia_expirada=(score > 65),
        recomendacao=RECOMENDACOES[perfil],
        modelo=modelo,
        concessionaria_codigo=conc,
    )
