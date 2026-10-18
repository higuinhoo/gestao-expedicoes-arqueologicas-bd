# Aplicação Java — Acervo Arqueológico

Java 17+, Swing e JDBC; MySQL Connector/J 8.4.0; projeto Maven padrão, sem ORM. O esquema é criado exclusivamente pelos scripts SQL autorais. O aplicativo é desktop e não publica um serviço web.

## Abrir no NetBeans

1. Instale um JDK completo 17+ e configure-o no NetBeans.
2. Use Arquivo → Abrir projeto e selecione a pasta `app/`, que contém `pom.xml`.
3. Prepare o banco e as contas conforme o README da raiz. Copie `config.example.properties` para `config.properties` e ajuste `db.url`; o arquivo real é ignorado pelo Git.
4. Execute o projeto. `nbactions.xml` aponta a ação Executar para `br.edu.ucb.arqueologia.App`, com diretório de trabalho `app/`.
5. Entre como `admin`, `catalogador` ou `consulta`, usando a senha individual que você definiu no provisionamento. Não existe senha padrão no código ou no pacote.

As telas são Swing escritas em Java; não foram criados arquivos `.form` do GUI Builder. O projeto abre e pode ser editado normalmente no NetBeans. A compilação e a interface foram verificadas com Maven/JDK 17 e display virtual Linux; a IDE NetBeans não estava disponível no ambiente de validação.

## Executar sem IDE

Na raiz do repositório, com banco/configuração preparados, use `./iniciar.sh` ou `iniciar.bat`. Eles iniciam o JAR em `dist/`.

Para compilar:

```bash
mvn -f app/pom.xml clean verify
```

O JAR inclui o driver MySQL e sai em `app/target/arqueologia-2.0.0.jar`. Para executar esse resultado diretamente:

```bash
java -Darq.config=app/config.properties -jar app/target/arqueologia-2.0.0.jar
```

Também é possível definir `ARQ_CONFIG` com o caminho absoluto do arquivo de configuração. Ele contém a URL, não senhas. Em instalação local, o exemplo usa `sslMode=PREFERRED`; para uma implantação remota configure validação de identidade/certificado e a truststore adequada.

## Uso das telas

- Pesquisadores (administrador): cadastro e edição coordenados do supertipo/subtipo na mesma transação; identificador interno não precisa ser digitado.
- Campanhas, Artefatos e Locais: CRUDs disponíveis a administrador e catalogador. Referências aparecem em seletores com nomes/códigos; as listas usam cabeçalhos de domínio.
- Consultas: carregar filtros opcionais e informar datas `AAAA-MM-DD`; acervo agrupa campanha/material/estado, análises agrupa laboratório sem duplicar custos do N:N.
- Custódia atual: atualizar o relatório, alimentado por VIEW; inclui artefatos cuja custódia ainda está pendente.
- Transferir custódia: carregar opções, escolher destino/local compatíveis, responsável, data, documento, motivo e situação resultante. A caixa de falha demonstra ROLLBACK após uma alteração intermediária. A nova data precisa ser posterior ao início vigente; o modelo aprovado não representa dois inícios no mesmo dia.
- Equipe: adicionar participante ativo ou remover participação; a inativação posterior não apaga o histórico.
- Auditoria (administrador): consultar autor, instante e snapshots das alterações de artefato/custódia.
- Consulta: perfil somente leitura; possui apenas as duas consultas e o relatório, e o MySQL também nega acesso a dados pessoais e escritas.

## Organização

- `db`: conexão/configuração e execução JDBC com PreparedStatement.
- `model`: sessão, perfis e descrições/SQL constante dos quatro CRUDs.
- `service`: autenticação, CRUD coordenado, relatórios, equipe, transferência e mensagens de erro.
- `ui`: janelas e painéis Swing; operações JDBC executam em SwingWorker para liberar a interface.
- `tools`: cadastro de contas da aplicação e experimentos de transação/acesso/concorrência/desempenho.
- `src/test`: testes de hash, integração com MySQL real e Swing real.

Os testes de integração exigem `ARQ_TEST_ADMIN_PASSWORD`, `ARQ_TEST_CATALOGADOR_PASSWORD` e `ARQ_TEST_CONSULTA_PASSWORD`, correspondentes a uma base de teste isolada. O script `scripts/validar_etapa2.py` gera credenciais na memória e configura essa base. O teste Swing depende de um display (`DISPLAY`, por exemplo Xvfb). Sem essas variáveis o Maven executa apenas os testes unitários e sinaliza os demais como ignorados; o ensaio completo registrado exigiu interface e teve zero ignorados.
