# Execução avançada

O [README](../README.md) descreve o caminho automático recomendado para apresentar. Este documento reúne alternativas de desenvolvimento.

## Conexão MySQL própria

1. Use uma base de desenvolvimento limpa e execute os scripts `sql/01_ddl.sql` a `sql/06_indices_desempenho.sql`, nessa ordem. O DDL reconstrói tabelas, e a carga insere os dados didáticos.
2. Crie as contas MySQL com senhas próprias e atribua as roles `arqueologia_admin`, `arqueologia_catalogador` e `arqueologia_consulta` usando `GRANT` e `SET DEFAULT ROLE`.
3. Copie `app/config.example.properties` para `app/config.properties` e ajuste host/porta.
4. Registre os hashes das contas na aplicação: `java -Darq.config=app/config.properties -cp dist/arqueologia-2.0.0.jar br.edu.ucb.arqueologia.tools.RegisterAccounts`. Esse caminho de manutenção exige senhas próprias de pelo menos doze caracteres.
5. Execute `iniciar.bat -ConfiguracaoExistente` ou `sh iniciar.sh --existing-config`. Essa opção preserva a conexão, não inicia o Compose e não configura as contas públicas de demonstração.

Alternativa direta com Java 17+ já instalado: `java -Darq.config=app/config.properties -jar dist/arqueologia-2.0.0.jar`.

O modo demo tem suas próprias contas em `demo/07_usuarios.sql`, separadas do provisionamento com senhas privadas. Aplicar esse arquivo intencionalmente configura as senhas iguais aos logins; não o aplique a um servidor compartilhado.

## Compilar

Com JDK 17+ e Maven, na raiz:

```sh
mvn -f app/pom.xml clean verify
```

O resultado compilado fica em `app/target/arqueologia-2.0.0.jar`. Copie para `dist/arqueologia-2.0.0.jar` antes de distribuir uma atualização.

## Testes em base isolada

O script abaixo requer Python 3, Docker, JDK e Maven. Ele cria um servidor com nome reservado `arqueologia-teste-*`, usa senhas aleatórias em memória e configuração em `local/`.

```sh
python scripts/validar_etapa2.py --recriar --exigir-interface
```

Linux: execute com um display real ou Xvfb. Windows: execute em sessão gráfica e defina `DISPLAY=windows` somente para habilitar os testes Swing; eles usam o display nativo. Não habilite essa opção em uma sessão sem interface gráfica.

Para testar o banco demo já preparado, defina `ARQ_CONFIG` com o caminho absoluto de `app/config.properties`, `ARQ_TEST_ADMIN_PASSWORD=admin`, `ARQ_TEST_CATALOGADOR_PASSWORD=catalogador` e `ARQ_TEST_CONSULTA_PASSWORD=consulta`, além de `DISPLAY` em uma sessão gráfica. Então execute Maven. Os testes geram registros sintéticos; use o ambiente de demonstração, não uma base de dados real.

`-Darq.evidenceDir=/caminho/de/evidencias` permite guardar capturas Swing fora das evidências históricas. Senhas/configurações privadas e `local/` são ignorados pelo Git.

## Estudos e backup

Os estudos anteriores estão em `docs/evidencias/etapa2/`. A carga grande é separada da carga da apresentação. Para repetir estudos, use uma base de ensaio e consulte `scripts/carga_desempenho.py`, `Benchmark.java` e `ConcurrentExperiment.java`; não execute esses processos durante a apresentação.

O [projeto físico](projeto-fisico-etapa2.md) documenta backup/retenção/restauração. `scripts/backup_restauracao.py` executa um ensaio em container novo; `scripts/backup.sh` gera backup a partir de arquivo de cliente externo ao Git. O banco demo persistente não substitui um backup.

## Gerar pacote portátil

Após compilar e atualizar o JAR em `dist/`, execute:

```sh
python scripts/empacotar.py
```

O pacote inclui fontes, JAR, scripts SQL, inicializadores e documentos. Exclui `.git`, configurações privadas, runtime baixado, bancos/volumes e backups. O usuário de destino prepara o ambiente ao executar o inicializador. O ZIP não inclui outro ZIP de si mesmo.

## Dependências do inicializador

- [Docker e Compose](https://docs.docker.com/compose/how-tos/startup-order/): o inicializador espera o banco estar saudável antes de abrir a aplicação.
- [API Adoptium](https://api.adoptium.net/): fornece o Java portátil quando não há Java 17+; o pacote baixado é validado por SHA-256.
- Linux/macOS: `sh`, `curl`, `tar` e `sha256sum` ou `shasum` para o primeiro download de Java. Se Java 17+ já existir, não é necessário baixar outro.
