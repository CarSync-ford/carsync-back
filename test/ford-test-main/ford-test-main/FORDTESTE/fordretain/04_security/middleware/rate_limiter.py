from slowapi import Limiter
from slowapi.util import get_remote_address

# 100 req/min por IP para endpoints normais
# 10 req/min por IP para endpoints de login (anti-brute-force)
limiter = Limiter(key_func=get_remote_address)

LIMITE_PADRAO = "100/minute"
LIMITE_AUTH = "10/minute"
