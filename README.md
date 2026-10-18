# Sistema de Gestão de Expedições Arqueológicas

Projeto das Etapas 1 e 2 de Laboratório de Banco de Dados, semestre 2026/2. Aplicação desktop Java/Swing/JDBC, MySQL e documentação consolidada; revisão C1–C5 incorporada.

## Aplicação e entrega final

- [Relatório final PDF](docs/relatorio-final.pdf): Etapa 2 e Etapa 1 revisada, com evidências reais.
- [Executável Java](dist/arqueologia-2.0.0.jar): requer Java 17+, MySQL preparado e configuração externa.
- [Pacote completo para executar/abrir no NetBeans](dist/arqueologia-etapa2.zip).
- [Instruções do projeto Java](app/README.md), [roteiro de apresentação](docs/roteiro-demonstracao.md) e [projeto físico/backup](docs/projeto-fisico-etapa2.md).
- [Evidências dos testes e experimentos](docs/evidencias/etapa2/).

## Preparar e executar — caminho com Docker

Requisitos: JDK 17+; Docker; Python 3 para o provisionamento. Maven é necessário para compilar ou executar os testes; o JAR pronto já contém o driver MySQL. Os ensaios usaram Java 17.0.17 e MySQL 8.4.11.

```bash
docker run --name arqueologia-dev -p 127.0.0.1:33316:3306 -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -e MYSQL_ROOT_HOST=% -d mysql:8.4
# Aguarde SELECT 1 funcionar por TCP (a inicialização usa um servidor temporário sem rede):
docker exec arqueologia-dev mysql --protocol=TCP -h127.0.0.1 -uroot -e "SELECT 1"
python scripts/preparar_etapa2.py --container arqueologia-dev --port 33316 --recriar
./iniciar.sh
```

No Windows, use `iniciar.bat` no último passo. O provisionamento pergunta três senhas individuais, com pelo menos doze caracteres; use-as nos logins `admin`, `catalogador` e `consulta`. O banco grava hashes/sais, e o arquivo de configuração só contém a URL. O uso de root sem senha nesse comando limita-se ao container didático com porta publicada apenas em loopback; o aplicativo conecta com cada conta restrita.

`--recriar` remove e reconstrói **arqueologia_ucb no container indicado**. Use um servidor de desenvolvimento, pois os scripts 01/02 também são destrutivos/recarregam dados. A carga inicial possui 100 artefatos; a carga grande de desempenho é separada e não é necessária para usar a aplicação.

## Preparar sem Docker — MySQL instalado

1. Como administrador do seu MySQL, execute `sql/01_ddl.sql`, `02_carga.sql`, `03_consultas.sql`, `04_operacao.sql`, `05_perfis.sql` e `06_indices_desempenho.sql`, nessa ordem. Use base limpa. O arquivo 06 cria os três índices de estudo e deve rodar uma vez por reconstrução.
2. Crie no MySQL/Workbench as contas `admin`, `catalogador` e `consulta`, com senhas individuais de pelo menos doze caracteres. Use o host adequado à sua instalação, por exemplo `localhost` para uso local.
3. Atribua as roles criadas pelo arquivo 05, ajustando o host abaixo ao host das contas que acabou de criar:

```sql
GRANT 'arqueologia_admin' TO 'admin'@'localhost';
SET DEFAULT ROLE 'arqueologia_admin' TO 'admin'@'localhost';
GRANT 'arqueologia_catalogador' TO 'catalogador'@'localhost';
SET DEFAULT ROLE 'arqueologia_catalogador' TO 'catalogador'@'localhost';
GRANT 'arqueologia_consulta' TO 'consulta'@'localhost';
SET DEFAULT ROLE 'arqueologia_consulta' TO 'consulta'@'localhost';
```

4. Copie `app/config.example.properties` para `app/config.properties` e ajuste host/porta. O arquivo real não entra no Git.
5. No terminal, na raiz do projeto, registre os mesmos logins/senhas na aplicação com hash/sal:

```bash
java -Darq.config=app/config.properties -cp dist/arqueologia-2.0.0.jar br.edu.ucb.arqueologia.tools.RegisterAccounts
```

O comando pede usuário/senha de manutenção e as três senhas existentes sem exibi-las. O usuário de manutenção precisa poder inserir/atualizar usuario_app; essas credenciais não são usadas pela interface. Em seguida execute `iniciar.sh`/`iniciar.bat` ou abra `app/` no NetBeans. O esquema não é criado automaticamente por classes.

## Validar e reproduzir os experimentos

```bash
# Testes completos em servidor próprio descartável; --exigir-interface requer DISPLAY no Linux:
python scripts/validar_etapa2.py --recriar --exigir-interface
# Em máquina sem display, omita --exigir-interface; o teste Swing será ignorado se não houver DISPLAY.
# Carga adicional apenas em uma base descartável de ensaio:
python scripts/carga_desempenho.py --container arqueologia-dev --quantidade 20000 --confirmar-base-teste
```

Não execute a carga grande no meio da apresentação. O teste usa o nome reservado `arqueologia-teste-*`, credenciais aleatórias em memória e configuração em `local/`, sem sobrescrever a configuração normal. Para os experimentos Java, defina fora do Git `ARQ_CONFIG`, `ARQ_TEST_ADMIN_PASSWORD` e, quando necessário observar locks/criar índices, `ARQ_MAINT_USER`/`ARQ_MAINT_PASSWORD` de uma conta de manutenção da base isolada. `DemoEvidence` também usa `ARQ_TEST_CATALOGADOR_PASSWORD`/`ARQ_TEST_CONSULTA_PASSWORD`.

```bash
java -cp dist/arqueologia-2.0.0.jar br.edu.ucb.arqueologia.tools.ConcurrentExperiment 1
java -cp dist/arqueologia-2.0.0.jar br.edu.ucb.arqueologia.tools.Benchmark docs/evidencias/etapa2/desempenho
java -cp dist/arqueologia-2.0.0.jar br.edu.ucb.arqueologia.tools.DemoEvidence
```

Benchmark remove/recria somente os três índices `idx_perf_*`, preservando PKs/FKs/índices da Etapa 1. DemoEvidence cria um artefato sintético para testar a transferência. Os arquivos existentes em docs/evidencias/etapa2 foram efetivamente produzidos durante o desenvolvimento e não são estimativas.

Resultado registrado: **13 testes, zero falhas, zero erros e zero ignorados**, incluindo dois testes Swing com display. Houve espera de lock observada no servidor, três medições EXPLAIN antes/depois e restauração completa em outro servidor. Não se afirma validação da IDE NetBeans ou de computadores do grupo: antes da entrega, iniciar o projeto no computador de apresentação e ensaiar o roteiro.

## Backup

`scripts/backup.sh` usa `ARQ_BACKUP_CONFIG` para apontar um arquivo de cliente MySQL protegido e `ARQ_BACKUP_DIR` para a pasta local de saída. O arquivo real e os backups ficam fora do Git. O [plano](docs/projeto-fisico-etapa2.md) define frequência, retenção e teste de restauração.

```bash
python scripts/backup_restauracao.py --origem arqueologia-dev --destino arqueologia-restaurado --confirmar-base-teste
```

O destino precisa ser um container novo; o script não remove servidores existentes. Usuários/papéis globais precisam ser reprovisionados após restaurar o esquema. O ensaio registrado também verificou a execução de views e de um trigger com rollback.

## Modelos prontos para visualizar e imprimir

- [MER conceitual em PDF](docs/mer-conceitual.pdf): notação de Chen, visão geral e quatro folhas com atributos e relacionamentos.
- [Modelo lógico em PDF](docs/modelo-logico.pdf): visão geral das chaves, esquema completo e quatro folhas de detalhe.
- [Pacote com PDFs e PNGs em alta resolução](docs/modelos-prontos.zip): arquivos já renderizados, sem necessidade de usar um editor.
- [PNGs e SVGs por folha](docs/diagramas/): cada página disponível separadamente.

O MER usa relacionamentos N:N com atributos próprios, símbolo nativo de especialização total/disjunta e entidade fraca. O modelo lógico do domínio exibe as 16 tabelas e as 22 FKs do DDL inicial. O [adendo técnico da Etapa 2](docs/adendo-tecnico-etapa2.pdf) acrescenta usuario_app e auditoria, totalizando dezoito tabelas. A visão geral serve para localizar os relacionamentos; as folhas seguintes apresentam os detalhes legíveis. Cada PDF preserva o tamanho apropriado da folha, de A3 a A0; para impressão sem redução, use o tamanho indicado pelo visualizador.

Os modelos foram renderizados com PlantUML e Graphviz, a partir de fontes textuais verificáveis. Não é necessário gerar nada para usar os arquivos acima. O relatório final também incorpora o adendo técnico, preservando os modelos de domínio aprovados. A versão anterior [editável no draw.io](docs/fontes/mer-conceitual.drawio) permanece disponível como alternativa; os PDFs atuais correspondem às fontes PlantUML.

## Integrantes

- Arthur Tavares Silva
- Higor Samuel Ferreira Silva
- Igor Vendramini Perini
- João Gabriel da Cruz Viana

## Domínio

O sistema registra expedições arqueológicas, unidades de escavação, artefatos, fragmentos, análises laboratoriais, conservação e cadeia temporal de custódia. Todos os dados da carga são fictícios.

## Requisitos e execução do banco

- MySQL 8.0 ou superior com suporte efetivo a CHECK (8.0.16+); validação realizada no MySQL 8.4.11.
- Engine InnoDB e conjunto de caracteres utf8mb4.
- Permissões para criar o esquema, tabelas e triggers.

A partir da raiz do repositório, execute no cliente MySQL, nesta ordem:

```sql
SOURCE sql/01_ddl.sql;
SOURCE sql/02_carga.sql;
SOURCE sql/03_consultas.sql;
```

O DDL remove e recria as tabelas do esquema `arqueologia_ucb`. Utilize uma instância de desenvolvimento; não execute sobre uma base com dados que precisam ser preservados. A carga termina com contagens e a consulta 15 deve retornar zero violações semânticas.

## Correções da devolutiva

- **C1:** MER em notação de Chen, com visão completa, atributos em folhas de detalhe, cardinalidades e especialização total/disjunta com símbolos próprios. O relatório incorpora as folhas vetoriais e a tabela de cardinalidades.
- **C2:** dependências funcionais `X -> Y` por relação e discussão formal de 3FN versus BCNF. A redundância de instituição/local de custódia e a coluna física gerada são explicitadas; não se afirma normalização integral sem ressalvas.
- **C3:** índices compostos `idx_` e nove triggers adicionais para RN08, RN17 e RN25, inclusive alterações nos registros referenciados. Os dois triggers originais de autorreferência foram preservados.
- **C4:** descrições variadas, materiais e cronologia coerentes, casos de contorno verificáveis, consulta 5 de triagem por risco e consulta 11 com subconsulta escalar correlacionada explícita.
- **C5:** relatório com A1–A5, dicionário integral legível, estrutura do repositório e registro das revisões.

O [registro completo da revisão](docs/revisao-devolutiva-etapa1.md) está incorporado ao relatório final da Etapa 2 junto com A1–A5 revisados. A seção de uso de IA está no relatório. RN03 é coordenada transacionalmente pela aplicação; RN04/RN20 receberam reforços em SQL/04_operacao.sql. A consulta 15 é complementar e não acusa a inativação posterior de participantes históricos.

## Validação executada

Os scripts executaram duas vezes, incluindo base limpa, no MySQL Community Server 8.4.11. Foram confirmadas 16 tabelas, 22 FKs, 28 CHECKs e 11 triggers; as quinze consultas executaram sem erro e a consulta 15 retornou zero violações. Os testes incluem INSERT/UPDATE inválidos, alterações dos pais, operações válidas com ROLLBACK, proteção contra snapshot antigo e os casos de contorno da carga.

A carga inclui 100 artefatos, 150 fragmentos e 119 registros de custódia. Um artefato ainda aguarda a primeira custódia, um local está pendente e um sítio ainda não produziu artefatos. Consulte a [saída da validação](docs/evidencias/validacao-etapa1.txt).

Para reproduzir em um container descartável, sem expor portas e sem credenciais persistentes:

```bash
docker run --name arqueologia-feedback-mysql -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -d mysql:8.4
# Aguarde o servidor responder ao ping antes do teste:
docker exec arqueologia-feedback-mysql mysqladmin ping --silent
python tests/validar_etapa1.py arqueologia-feedback-mysql
docker rm -f arqueologia-feedback-mysql
```

O teste apaga/recria `arqueologia_ucb` **no container indicado**. A senha vazia destina-se somente a esse ambiente local descartável, sem porta publicada.

## Estrutura do repositório

```text
README.md
iniciar.sh / iniciar.bat       # Abrem o executável com configuração externa
dist/                         # JAR pronto e pacote completo
app/                          # Projeto Maven Java/Swing/JDBC para NetBeans
  pom.xml / nbactions.xml
  config.example.properties
  src/main/java/
  src/test/java/
sql/
  01_ddl.sql                   # Esquema autoral, restrições, índices e triggers
  02_carga.sql                 # Dados sintéticos e casos de contorno
  03_consultas.sql             # 15 consultas comentadas
  04_operacao.sql              # Autenticação, views, auditoria e integridade complementar
  05_perfis.sql                # GRANT/REVOKE e menor privilégio
  06_indices_desempenho.sql    # Índices estudados no B4
docs/
  relatorio-final.pdf          # B1–B8 e Etapa 1 revisada, 48 páginas
  adendo-tecnico-etapa2.pdf    # Tabelas técnicas de contas e auditoria
  relatorio-etapa1.pdf         # A1–A5, validação e revisão da devolutiva
  mer-conceitual.pdf           # Chen: visão geral e quatro folhas de detalhe
  modelo-logico.pdf            # Visão geral, esquema completo e detalhes
  dicionario-dados.pdf         # Também incorporado integralmente ao relatório
  revisao-devolutiva-etapa1.md  # Rastreabilidade C1–C5 para o relatório final
  fontes/                     # Fontes Markdown e PlantUML; versões anteriores preservadas
  evidencias/                 # Saída dos testes executados
scripts/
  preparar_etapa2.py           # Provisionamento local com senhas externas
  validar_etapa2.py            # Testes em servidor descartável
  carga_desempenho.py          # Massa adicional para medições
  backup.sh                   # Backup diário com configuração externa
  backup_restauracao.py        # Ensaio de restauração em outro servidor
  gerar_modelos.py             # DDL/modelo -> PlantUML -> PDF, SVG e PNG
  gerar_documentos.py          # Markdown + modelos -> PDFs paginados
  requirements.txt
tests/
  validar_etapa1.py            # Testes em MySQL descartável
```

## Regenerar os PDFs e PNGs

Os resultados prontos já estão versionados em `docs/`. A regeneração requer Python 3.10+, Java 17+, Graphviz (`dot`), Pandoc, Cairo e fontes DejaVu. Em Debian/Ubuntu: `default-jre`, `graphviz`, `pandoc`, `libcairo2` e `fonts-dejavu-core`.

```bash
python -m pip install -r scripts/requirements.txt
curl -L --fail https://github.com/plantuml/plantuml/releases/download/v1.2025.2/plantuml-1.2025.2.jar -o /tmp/plantuml.jar
PLANTUML_JAR=/tmp/plantuml.jar python scripts/gerar_modelos.py
python scripts/gerar_documentos.py
python scripts/gerar_documentos.py --final
```

As fontes canônicas dos modelos atuais estão em `docs/fontes/plantuml/`. A descrição conceitual usa relacionamentos de domínio; o lógico extrai as colunas e FKs do DDL. Arquivos `.dot`, `.mmd` e `.drawio` da revisão anterior foram preservados para consulta e não geram os PDFs atuais. O relatório incorpora todas as folhas dos dois modelos, com paginação contínua.
