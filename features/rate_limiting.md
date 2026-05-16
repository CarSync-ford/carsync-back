Crie um rate limiting para a API usando o Bucket4j.

1 - Criação de Testes Unitários para o teste a funcionalidade
2 - Adicionar dependência no pom.xml
3 - Criar filtro de Rate Limiting
4 - Registrar o filtro no SecurityConfig

Regras: 
- maximo de 3 requests simultaneas por ip, 10 requests por segundo
- adicione a dependência bucket4j
- todos os testes devem passar

No início da task, deve ser criada uma nova branch com base na main chamada feat/rate-limiting
A cada task feita deve ser criado um commit, seguindo conventional commits. "Ex: feat: o que foi feito"
Ao fim da task, deve ser criado um PR com base no prompt em features/PullRequestSummarizer.md
