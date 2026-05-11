Implementar fluxo de criação de usuário via requisição POST com validação de payload, unicidade de CPF e hash de senha.
Requisitos de Desenvolvimento:

Endpoint POST /user/ com payload contendo os dados do usuário.
Validação do formato do payload, retornando erro 400 caso a validação falhe.
Verificação de unicidade do CPF na tabela user do banco de dados.
Resposta 400 caso o CPF informado já esteja registrado no sistema.
Geração de hash da senha enviada pelo usuário antes do armazenamento.
Persistência do novo usuário no banco de dados com a senha em formato hash.
Cenários de Teste: Sucesso:

Dado que o usuário envia uma requisição POST com um payload válido e CPF não cadastrado, Quando o sistema processa a requisição, Então deve aplicar o hash na senha, salvar o novo registro e retornar status de sucesso.
Falha:

Dado que o usuário envia uma requisição POST com um CPF que já existe no banco de dados, Quando o sistema verifica a unicidade, Então deve retornar resposta 400 indicando erro de validação.


Com base na descrição do card de task acima, crie o prompt de SDD para a confecção da Task para seguir o fluxo criado no aider.
Use o diagrama de banco de dados criado como base do banco de dados.
Altere o que for necessário para se adequar ao banco.
Para os campos do payload recebido, deve-se basear no que está presente no banco de dados.

Testes:
- Um usuário que já exista o CPF no banco não deve ser cadastrado
- Usuários recém cadastrados devem ter os valores relacionados a MFA como falso
- Devem existir validações para cada um dos valores no payload recebido antes do processamento dos dados
- Senhas devem estar com hash usando bycript, o salt sera disponibilizado com variavel no .env
- Senhas menores que 8 digitos não devem ser aceitas
- Senhas sem letras maiúsiculas não devem ser aceitas
- Senhas sem caracteres especiais não devem ser aceitas
- Apenas emails válidos devem ser aceitos
- CPFs devem passar pelo cálculo de verificação para serem aceitos

DB:
user_type ||--o{ user : references

user {
		VARCHAR(255) id
		VARCHAR(255) username
		VARCHAR(255) email
		VARCHAR(255) hashed_password
		VARCHAR(255) cpf
		TIMESTAMP last_login
		VARCHAR(255) user_type
		VARCHAR(255) mfa_secret
		BOOLEAN mfa_enabled
		VARCHAR(255) refresh_token
	}

	user_type {
		VARCHAR(255) id
		VARCHAR(255) type
	}