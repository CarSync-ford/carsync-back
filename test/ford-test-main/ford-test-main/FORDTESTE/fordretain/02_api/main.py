from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from routers import clientes, concessionarias, scores, auth, campanhas, acoes
import os

limiter = Limiter(key_func=get_remote_address)

app = FastAPI(
    title="FordRetain API",
    description=(
        "Sistema inteligente de retenção de clientes Ford. "
        "Score de risco preditivo e perfis comportamentais por VIN."
    ),
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

STATIC_DIR = os.path.join(os.path.dirname(__file__), "static")
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

app.include_router(auth.router, prefix="/auth", tags=["Autenticação"])
app.include_router(clientes.router, prefix="/clientes", tags=["Clientes"])
app.include_router(concessionarias.router, prefix="/concessionarias", tags=["Concessionárias"])
app.include_router(scores.router, prefix="/scores", tags=["Scores de Risco"])
app.include_router(campanhas.router, prefix="/campanhas", tags=["Campanhas"])
app.include_router(acoes.router, prefix="/acoes", tags=["Ações"])


@app.get("/", tags=["Status"])
def root():
    return {
        "status": "online",
        "sistema": "FordRetain API v1.0",
        "docs": "/docs",
        "dashboard": "/dashboard",
    }


@app.get("/dashboard", tags=["Dashboard"], include_in_schema=False)
def dashboard():
    return FileResponse(os.path.join(STATIC_DIR, "dashboard.html"))


@app.get("/health", tags=["Status"])
def health():
    return {"status": "healthy"}
