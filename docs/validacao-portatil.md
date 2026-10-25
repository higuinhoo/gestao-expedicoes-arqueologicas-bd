# Validação da execução portátil

Verificação realizada em 25/10/2026, em Windows, com Docker Desktop/Compose e MySQL 8.4.

## Inicialização

- Preparação a partir de um volume novo do Compose.
- Download automático do Temurin JRE 17 e verificação SHA-256 antes da execução.
- Execução dos seis scripts do banco e provisionamento das três contas de demonstração.
- Conferência de 18 tabelas, cinco views, 25 triggers e 100 artefatos na carga inicial.
- Conferência de acentos na carga, com SET NAMES utf8mb4 explícito em cada script SQL e um teste de regressão no banco real.
- Parada e reinicialização do banco, seguidas de nova preparação. Um registro de teste foi preservado e removido após a conferência; as contas permaneceram em três, sem duplicação.
- Configuração JDBC gerada para 127.0.0.1:33319; a conexão anterior foi preservada como cópia em local/.
- ZIP extraído em outra pasta com espaços no nome, sem configuração privada prévia. Inicialização completa em projeto Compose novo e porta alternativa 33320; os três logins reais e suas roles foram conferidos. O ambiente temporário foi removido após o teste.

## Aplicação

Compilação com JDK 25 direcionada a Java 17; execução dos testes em processo Java 17 usando o mesmo runtime portátil obtido pelo inicializador.

| Classe | Testes | Cobertura |
|---|---:|---|
| IntegrationTest | 11 | Quatro CRUDs, autenticação/perfis, parametrização, integridade, transações, auditoria, locks e codificação da carga |
| PasswordsTest | 1 | Hash, sal e verificação de senha |
| SwingSmokeTest | 2 | Login, CRUD, consultas, relatório, injeção, erro de restrição e leitor pela interface |
| TransferPanelTest | 4 | Carregamento automático, locais filtrados, preservação do preenchimento e transferência com falha/sucesso |

Resultado: **18 testes, zero falhas, zero erros e zero ignorados**. Foram usados os acessos admin/admin, catalogador/catalogador e consulta/consulta.

Log: [validacao-portatil.txt](evidencias/etapa2/validacao-portatil.txt). As capturas desta execução foram guardadas separadamente para preservar as evidências históricas.

## Limites

Linux/macOS têm inicializador POSIX, detecção de arquitetura e download próprio do Java; a execução completa nesses sistemas não foi repetida nesta verificação. O script shell foi validado sintaticamente. NetBeans precisa de JDK e permanece sujeito à configuração da IDE no computador de destino.

O ZIP é conferido quanto a integridade e conteúdo e exclui configuração JDBC privada, cache de Java, .git, bancos e backups. O primeiro uso em outra máquina requer Docker iniciado e internet para baixar a imagem/runtime ainda ausentes. Não se afirma execução offline em uma máquina nunca preparada.

Os planos e tempos de desempenho da entrega original continuam sendo os ensaios registrados nos arquivos anteriores; esses números não foram substituídos por uma medição desta base de apresentação.
