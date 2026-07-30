import logging
import json
from datetime import datetime

logging.basicConfig(
    filename='audit.log',
    level=logging.INFO,
    format='%(asctime)s %(message)s'
)


def log_acesso(usuario: str, endpoint: str, recurso: str, ip: str):
    """Registra acesso a recursos sensíveis sem dados pessoais."""
    entry = {
        "ts": datetime.utcnow().isoformat(),
        "usuario": usuario,
        "endpoint": endpoint,
        "recurso": recurso[:20] + "..." if len(recurso) > 20 else recurso,
        "ip": ip,
    }
    logging.info(json.dumps(entry))
