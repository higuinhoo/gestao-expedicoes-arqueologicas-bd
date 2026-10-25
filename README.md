# Gestão de Expedições Arqueológicas

Projeto de Laboratório de Banco de Dados — UCB, 2026/2. Aplicação desktop em **Java/Swing/JDBC**, com **MySQL**, para gerenciar pesquisadores, campanhas, artefatos e custódias.

## Iniciar o projeto

1. [Instale e abra o Docker](https://docs.docker.com/get-started/get-docker/). No Linux, use Docker Engine com o plugin Compose.
2. Baixe o repositório ou o [pacote completo](dist/arqueologia-etapa2.zip) e extraia a pasta.
3. Execute:

| Sistema | Comando |
|---|---|
| Windows | Duplo clique em **`iniciar.bat`** |
| Linux / macOS | No terminal da pasta: **`sh iniciar.sh`** |

Na primeira execução, é necessária internet. O inicializador encontra Java 17+ ou baixa um runtime portátil, inicia o MySQL, cria as tabelas, carrega dados fictícios, configura os acessos e abre a interface. **Não é necessário instalar Python, Maven ou MySQL separadamente.**

Nas próximas execuções, os dados cadastrados são preservados. O banco fica acessível somente nesta máquina, na porta **33319**. A interface é desktop; não há endereço de site para abrir.

## Acessos de demonstração

| Usuário | Senha | Permissões |
|---|---|---|
| `admin` | `admin` | Quatro CRUDs, consultas, transferência e auditoria |
| `catalogador` | `catalogador` | Campanhas, artefatos, locais, equipe, transferência e consultas |
| `consulta` | `consulta` | Consultas e relatório |

Esses acessos são públicos para a apresentação com dados fictícios. As senhas do aplicativo continuam armazenadas com **hash e sal**. Para configurar outra conexão e contas próprias, veja [execução avançada](docs/execucao-avancada.md).

## Abrir no NetBeans

1. Prepare o banco: execute **`preparar-netbeans.bat`** no Windows ou **`sh iniciar.sh --prepare-only`** no Linux/macOS.
2. No NetBeans, use **Arquivo → Abrir projeto** e selecione **`app/`**, onde está `pom.xml`.
3. Configure um **JDK 17 ou superior** na IDE e execute o projeto.

O runtime baixado pelo inicializador basta para o JAR; editar e compilar na IDE exige um JDK. A configuração `app/config.properties` é gerada automaticamente e não é versionada.

## O que demonstrar

- CRUD de pesquisadores, campanhas, artefatos e locais de armazenamento.
- Duas consultas parametrizadas e relatório alimentado por VIEW.
- Transferência de custódia com COMMIT, falha simulada e ROLLBACK.
- Restrições, três perfis e auditoria de alterações.

[Roteiro da demonstração](docs/roteiro-demonstracao.md) · [Uso das telas](app/README.md)

## Parar e solucionar problemas

- **Parar o banco:** `parar.bat` ou `sh parar.sh`. Os cadastros permanecem salvos.
- **Docker indisponível:** abra o Docker Desktop, aguarde iniciar e tente novamente.
- **Porta 33319 ocupada:** veja como escolher outra em [configuração do ambiente](demo/README.md).
- **Primeira execução demorada:** aguarde o download do MySQL/Java e a carga inicial. Faça esse primeiro teste antes da aula.

## Documentos e código

| Local | Conteúdo |
|---|---|
| [Relatório final](docs/relatorio-final.pdf) | Etapa 1 revisada e Etapa 2 |
| [Modelo conceitual](docs/mer-conceitual.pdf) · [Modelo lógico](docs/modelo-logico.pdf) | Diagramas |
| [Dicionário de dados](docs/dicionario-dados.pdf) | Tabelas e atributos |
| [`sql/`](sql/) | Esquema autoral, carga, consultas, views, gatilhos, perfis e índices |
| [`app/`](app/) | Código Java e testes Maven |
| [Validação](docs/validacao-portatil.md) | Testes desta versão e limites da verificação |
| [Projeto físico](docs/projeto-fisico-etapa2.md) | Tipos, crescimento e backup |

## Integrantes

- Arthur Tavares Silva
- Higor Samuel Ferreira Silva
- Igor Vendramini Perini
- João Gabriel da Cruz Viana
