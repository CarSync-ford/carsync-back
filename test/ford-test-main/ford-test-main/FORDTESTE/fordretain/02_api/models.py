from pydantic import BaseModel, Field
from typing import Optional, List
from enum import Enum


class PerfilCliente(str, Enum):
    fiel = "Fiel"
    economico = "Econômico"
    esquecido = "Esquecido"
    abandono = "Abandono"


class ClienteScore(BaseModel):
    vin_hash: str
    score_risco: int = Field(ge=0, le=100, description="Score de risco de 0 a 100")
    perfil: PerfilCliente
    total_revisoes: int
    dias_desde_ultima_revisao: Optional[int] = None
    km_atual: Optional[float] = None
    garantia_expirada: bool
    recomendacao: str
    modelo: str
    concessionaria_codigo: int


class ClienteRiscoItem(BaseModel):
    vin_hash: str
    score_risco: int
    perfil: PerfilCliente
    modelo: str
    detalhe: str


class ListaRisco(BaseModel):
    concessionaria: int
    total_em_risco: int
    clientes: List[ClienteRiscoItem]


class ServiceShareConcessionaria(BaseModel):
    dealer_code: int
    service_share: float
    total_vins: int
    vins_ativos: int
    tendencia: str
    clientes_em_risco: int


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class LoginRequest(BaseModel):
    email: str
    senha: str


class AcaoConsultor(BaseModel):
    tipo: str  # "ligacao", "oferta", "agendamento", "nao_atendeu"
    resultado: Optional[str] = None
    observacao: Optional[str] = None


class AcaoRegistrada(AcaoConsultor):
    id: str
    vin_hash: str
    timestamp: str


class CampanhaRequest(BaseModel):
    concessionaria: int
    perfil: Optional[str] = None
    score_minimo: int = 60


class CampanhaResultado(BaseModel):
    total_impactados: int
    perfis: dict
    ticket_medio: float
    receita_recuperavel: float
    modelos_top: List[str]


class ROISummary(BaseModel):
    vins_em_risco: int
    ticket_medio: float
    receita_em_risco: float
    recuperacao_15pct: float
    recuperacao_20pct: float
    recuperacao_30pct: float
