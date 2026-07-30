import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def get_token():
    response = client.post("/auth/login", json={"email": "consultor@ford.com.br", "senha": "secret"})
    assert response.status_code == 200
    return response.json()["access_token"]


def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "healthy"


def test_root():
    r = client.get("/")
    assert r.status_code == 200
    assert "FordRetain" in r.json()["sistema"]


def test_login_sucesso():
    r = client.post("/auth/login", json={"email": "consultor@ford.com.br", "senha": "secret"})
    assert r.status_code == 200
    data = r.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"


def test_login_falha():
    r = client.post("/auth/login", json={"email": "x@x.com", "senha": "errada"})
    assert r.status_code == 401


def test_score_cliente():
    token = get_token()
    r = client.get("/scores/abc1234567890def", headers={"Authorization": f"Bearer {token}"})
    assert r.status_code == 200
    data = r.json()
    assert 0 <= data["score_risco"] <= 100
    assert "perfil" in data
    assert "recomendacao" in data


def test_score_vin_invalido():
    token = get_token()
    r = client.get("/scores/abc", headers={"Authorization": f"Bearer {token}"})
    assert r.status_code == 400


def test_clientes_risco():
    token = get_token()
    r = client.get("/clientes/risco?concessionaria=1009&limite=5", headers={"Authorization": f"Bearer {token}"})
    assert r.status_code == 200
    data = r.json()
    assert data["concessionaria"] == 1009
    assert len(data["clientes"]) == 5


def test_service_share():
    token = get_token()
    r = client.get("/concessionarias/1009/service-share", headers={"Authorization": f"Bearer {token}"})
    assert r.status_code == 200
    data = r.json()
    assert data["dealer_code"] == 1009
    assert 0 <= data["service_share"] <= 100


def test_sem_token_retorna_401():
    r = client.get("/scores/abc1234567890def")
    assert r.status_code == 401
