from fastapi import APIRouter, Query, Depends
from models import ListaRisco, ClienteRiscoItem, PerfilCliente
from auth import verificar_token
from typing import Optional
import random

router = APIRouter()

MODELOS = ["RANGER", "MAVERICK", "BRONCO SPORT", "F-150", "TRANSIT", "TERRITORY"]


@router.get("/risco", response_model=ListaRisco)
def clientes_em_risco(
    concessionaria: int = Query(..., description="Código da concessionária", gt=0, lt=100000),
    perfil: Optional[PerfilCliente] = Query(None, description="Filtrar por perfil"),
    limite: int = Query(20, ge=1, le=100),
    score_minimo: int = Query(60, ge=0, le=100),
    _: dict = Depends(verificar_token),
):
    """
    Lista clientes em risco para uma concessionária, ordenados por score decrescente.
    """
    clientes = []
    for i in range(limite):
        p = perfil or random.choice(list(PerfilCliente))
        score = random.randint(score_minimo, 99)
        clientes.append(ClienteRiscoItem(
            vin_hash=f"abc{i:04d}def{concessionaria:04d}",
            score_risco=score,
            perfil=p,
            modelo=random.choice(MODELOS),
            detalhe=f"Revisão vencida há {random.randint(15, 90)} dias · {random.randint(20000, 60000):,} km",
        ))

    clientes.sort(key=lambda c: c.score_risco, reverse=True)
    return ListaRisco(
        concessionaria=concessionaria,
        total_em_risco=len(clientes),
        clientes=clientes,
    )
