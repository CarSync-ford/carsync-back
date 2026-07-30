import re
from fastapi import HTTPException


def validar_vin_hash(vin: str) -> str:
    if not re.match(r'^[a-zA-Z0-9]{10,64}$', vin):
        raise HTTPException(status_code=400, detail="VIN hash inválido")
    return vin


def validar_dealer_code(code: int) -> int:
    if code <= 0 or code > 99999:
        raise HTTPException(status_code=400, detail="Código de concessionária inválido")
    return code


def sanitizar_texto(texto: str, max_len: int = 200) -> str:
    sanitizado = re.sub(r'[<>"\';\\]', '', texto)
    return sanitizado[:max_len]
