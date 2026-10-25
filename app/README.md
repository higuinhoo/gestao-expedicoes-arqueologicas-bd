# Aplicação Java/Swing

Projeto Maven com Java 17+, Swing e JDBC. O banco é definido pelos scripts SQL autorais, sem geração de tabelas por ORM.

## Executar

Na raiz, execute `iniciar.bat` (Windows) ou `sh iniciar.sh` (Linux/macOS). O ambiente é preparado automaticamente; os acessos estão no [README principal](../README.md).

Para NetBeans, prepare com `preparar-netbeans.bat` ou `sh iniciar.sh --prepare-only`, abra esta pasta como projeto Maven e use um JDK 17+. A ação Executar usa `br.edu.ucb.arqueologia.App` e o diretório `app/`.

## Telas

- **Pesquisadores:** exclusivo do administrador; cadastro e edição de supertipo/subtipo na mesma transação.
- **Campanhas, Artefatos e Locais de armazenamento:** Novo, Pesquisar, Editar e Remover. As tabelas são de consulta; use Editar para alterar uma linha selecionada.
- **Consulta do acervo / Consulta de análises:** carregar filtros, informar período `AAAA-MM-DD` e consultar. Os filtros de material/situação são opcionais.
- **Custódia atual:** Atualizar para consultar o relatório por VIEW, incluindo artefatos sem custódia.
- **Transferir custódia:** as opções carregam ao abrir a aba. Escolha registros nas listas e digite data, documento e motivo. Os locais são filtrados pela instituição. Atualizar opções preserva o preenchimento. A nova data deve ser posterior à custódia vigente; a caixa de falha demonstra ROLLBACK.
- **Equipe de campanha:** Carregar para selecionar campanha/pesquisador ativo e registrar a participação.
- **Auditoria:** exclusiva do administrador; Atualizar para conferir autor, instante e valores anteriores/posteriores.
- **Consulta:** perfil apenas de leitura, com as duas consultas e o relatório.

## Compilar e testar

Com JDK 17+ e Maven: `mvn -f app/pom.xml clean verify`, a partir da raiz. O JAR sai em `app/target/arqueologia-2.0.0.jar`.

Os testes de integração exigem banco e variáveis de ambiente; os de Swing exigem um display. Uma execução que ignorou esses testes não é uma validação completa. Veja [execução avançada](../docs/execucao-avancada.md) e [validação desta versão](../docs/validacao-portatil.md).

## Código

- `db`: configuração, conexões e PreparedStatement.
- `model`: sessão, perfis e especificações dos CRUDs.
- `service`: autenticação, regras e transações.
- `ui`: janelas e formulários Swing.
- `tools`: experimentos de concorrência, desempenho e evidências.
- `src/test`: testes unitários, integração e Swing.

As telas são escritas em Java; não há arquivos `.form` do GUI Builder.
