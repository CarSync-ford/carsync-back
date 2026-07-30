from fastapi import APIRouter, Depends
from models import AcaoConsultor, AcaoRegistrada
from auth import verificar_token
from datetime import datetime
import uuid

router = APIRouter()

# Armazenamento em memória para demo — em produção: BigQuery/banco
_acoes: list[dict] = []


@router.post("/{vin_hash}", response_model=AcaoRegistrada)
def registrar_acao(vin_hash: str, acao: AcaoConsultor, _: dict = Depends(verificar_token)):
    """Registra uma ação do consultor (ligação, oferta enviada, agendamento, não atendeu, etc.)."""
    registro = AcaoRegistrada(
        id=str(uuid.uuid4())[:8],
        vin_hash=vin_hash,
        tipo=acao.tipo,
        resultado=acao.resultado,
        observacao=acao.observacao,
        timestamp=datetime.utcnow().isoformat(),
    )
    _acoes.append(registro.model_dump())
    return registro


@router.get("/{vin_hash}")
def historico_acoes(vin_hash: str, _: dict = Depends(verificar_token)):
    """Retorna histórico de ações do consultor para um VIN específico."""
    historico = [a for a in _acoes if a["vin_hash"] == vin_hash]
    return {"vin_hash": vin_hash, "total": len(historico), "acoes": historico}
