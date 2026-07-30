from fastapi import APIRouter, Depends
from models import CampanhaRequest, CampanhaResultado
from auth import verificar_token

router = APIRouter()

TICKET_MEDIO = 380.0
MODELOS = ["RANGER", "MAVERICK", "BRONCO SPORT", "F-150", "TRANSIT", "TERRITORY", "KA"]


@router.post("/simular", response_model=CampanhaResultado)
def simular_campanha(req: CampanhaRequest, _: dict = Depends(verificar_token)):
    """
    Simula o impacto de uma campanha de retenção —
    quantos clientes seriam atingidos e o retorno financeiro estimado.
    """
    seed = req.concessionaria * 7 + req.score_minimo
    total = max(5, (seed % 80) + 20)

    if req.perfil:
        perfis = {req.perfil: total}
    else:
        perfis = {
            "Abandono": int(total * 0.30),
            "Esquecido": int(total * 0.35),
            "Econômico": int(total * 0.25),
            "Fiel": int(total * 0.10),
        }

    receita = round(total * TICKET_MEDIO * 0.72, 2)
    modelos_top = MODELOS[:3]

    return CampanhaResultado(
        total_impactados=total,
        perfis=perfis,
        ticket_medio=TICKET_MEDIO,
        receita_recuperavel=receita,
        modelos_top=modelos_top,
    )
