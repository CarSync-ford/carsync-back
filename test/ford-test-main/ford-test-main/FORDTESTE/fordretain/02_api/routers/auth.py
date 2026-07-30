from fastapi import APIRouter, HTTPException, status
from passlib.context import CryptContext
from models import LoginRequest, TokenResponse
from auth import criar_token

router = APIRouter()
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# Usuários mock dev — senha padrão: "secret"
USUARIOS_MOCK = {
    "consultor@ford.com.br": {
        "senha": "$2b$12$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW",
        "role": "consultor",
    },
    "gestor@ford.com.br": {
        "senha": "$2b$12$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW",
        "role": "gestor",
    },
}


@router.post("/login", response_model=TokenResponse)
def login(body: LoginRequest):
    """Autenticação JWT. E-mail: consultor@ford.com.br · Senha: secret"""
    user = USUARIOS_MOCK.get(body.email)
    if not user or not pwd_context.verify(body.senha, user["senha"]):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Credenciais inválidas",
        )
    token = criar_token({"sub": body.email, "role": user["role"]})
    return TokenResponse(access_token=token)
