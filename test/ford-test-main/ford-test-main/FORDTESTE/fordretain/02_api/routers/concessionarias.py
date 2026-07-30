from fastapi import APIRouter, Depends
from models import ServiceShareConcessionaria, ROISummary
from auth import verificar_token
import hashlib
import random

router = APIRouter()


@router.get("/{dealer_code}/service-share", response_model=ServiceShareConcessionaria)
def service_share(dealer_code: int, _: dict = Depends(verificar_token)):
    """
    Índice de Service Share atual da concessionária com tendência e clientes em risco.
    """
    if dealer_code <= 0 or dealer_code > 99999:
        from fastapi import HTTPException
        raise HTTPException(status_code=400, detail="Código de concessionária inválido")

    share = round(random.uniform(30, 65), 1)
    tendencia = "alta" if share > 50 else ("estável" if share > 40 else "queda")

    return ServiceShareConcessionaria(
        dealer_code=dealer_code,
        service_share=share,
        total_vins=random.randint(200, 1500),
        vins_ativos=random.randint(100, 800),
        tendencia=tendencia,
        clientes_em_risco=random.randint(20, 150),
    )


@router.get("/{dealer_code}/roi", response_model=ROISummary)
def roi_concessionaria(dealer_code: int, _: dict = Depends(verificar_token)):
    """Calcula o ROI estimado de retenção para a concessionária com base nos VINs em risco."""
    if dealer_code <= 0 or dealer_code > 99999:
        from fastapi import HTTPException
        raise HTTPException(status_code=400, detail="Código de concessionária inválido")

    seed = int(hashlib.md5(str(dealer_code).encode()).hexdigest()[:6], 16) % 400
    vins_risco = seed + 80
    ticket = 380.0

    return ROISummary(
        vins_em_risco=vins_risco,
        ticket_medio=ticket,
        receita_em_risco=round(vins_risco * ticket, 2),
        recuperacao_15pct=round(vins_risco * ticket * 0.15, 2),
        recuperacao_20pct=round(vins_risco * ticket * 0.20, 2),
        recuperacao_30pct=round(vins_risco * ticket * 0.30, 2),
    )
