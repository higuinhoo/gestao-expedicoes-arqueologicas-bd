# Gestão de Expedições Arqueológicas

Relatório final — Laboratório de Banco de Dados — 2026/2.

Arthur Tavares Silva; Higor Samuel Ferreira Silva; Igor Vendramini Perini; João Gabriel da Cruz Viana.

Este documento incorpora a Etapa 1 revisada e apresenta a operação do banco em uma aplicação desktop Java/Swing/JDBC. As evidências foram coletadas em 18/10/2026 com JDK 17.0.17, MySQL 8.4.11 e display virtual Linux para testar a interface. A IDE NetBeans não estava instalada nesse ambiente: o projeto Maven e a configuração de execução foram preparados para ela, mas não se afirma um teste dentro da IDE ou no computador dos integrantes.

## Mapa de requisitos e evidências

| Item | Implementação | Verificação | Referência |
|---|---|---|---|
| B1 | Quatro CRUDs, duas consultas parametrizadas, relatório por VIEW, transferência e erros tratados | Integração real e operação de Swing | app/src; screenshots e testes.json |
| B2 | JDBC/PreparedStatement, credenciais individuais externas, PBKDF2 e teste de injeção | Entrada maliciosa retornou zero linhas; preparo observado no servidor | transacoes-seguranca-auditoria.txt |
| B3 | COMMIT/ROLLBACK em custódia/artefato; lock por artefato | Falha intermediária preservou estado; duas sessões e espera observada | concorrencia.txt; IntegrationTest |
| B4 | Três consultas/índices com planos e tempos antes/depois | Carga ampliada, EXPLAIN JSON e ANALYZE, sete medições por fase | desempenho/; medicoes.csv |
| B5 | Três roles MySQL, GRANT/REVOKE, projeções restritas e login com hash/sal | Logins reais; SELECT pessoal/UPDATE negados ao leitor | 05_perfis.sql; testes de perfis |
| B6 | Dois triggers de auditoria para UPDATE de artefato/custódia | Autor, instante e JSON antes/depois; rollback atômico | 04_operacao.sql; evidências |
| B7 | Revisão de tipos, projeção de dois anos, backup/retenção e restauração | Dump restaurado em outro servidor; views/triggers verificados | projeto-fisico-etapa2.md; restauracao.txt |
| B8 | Revisão C1–C5 preservada, relatório consolidado, IA e roteiro de quinze minutos | Documentos e ambiente ensaiado automaticamente | anexo da Etapa 1; roteiro-demonstracao.md |

A apresentação presencial, a conferência no computador do grupo e a submissão no AVA são atividades dos integrantes. Os artefatos estão preparados e os ensaios descritos neste relatório foram executados; não se confunde isso com a realização futura da apresentação.

## B1 — Aplicação e interface

A aplicação usa Java 17, Swing e JDBC, sem ORM. Maven organiza o driver MySQL e os testes. O DDL continua autoral em SQL. As telas Swing são escritas em Java, sem arquivos .form do GUI Builder; o NetBeans abre app/pom.xml como projeto Maven e nbactions.xml configura a classe principal.

Os quatro CRUDs completos são Pesquisadores, Campanhas, Artefatos e Locais de armazenamento. Pesquisadores é reservado ao administrador; os demais também são disponíveis ao catalogador. Criar, consultar, alterar e remover são operações reais sobre o banco. O leitor recebe apenas as duas consultas e o relatório. Os seletores mostram nomes/códigos, e identificadores técnicos ficam internos às listas de CRUD.

![Interface Swing real com CRUD de pesquisadores e conta administradora.](docs/evidencias/etapa2/tela-principal.png)

Consultas JDBC executam em SwingWorker; a interface não faz a espera do banco no thread de eventos. As tabelas de CRUD retornam até 500 registros por busca e orientam o usuário a refinar o filtro. O relatório consolidado lê a VIEW e inclui custódias pendentes.

### Consultas e relatório

A consulta de acervo filtra campanha, material e período, retornando quantidade por campanha/material/estado e profundidade média. A VIEW vw_acervo_consulta relaciona artefato, unidade, campanha e sítio; o SELECT parametrizado agrupa os resultados. Assim, a consulta executada inclui junções e agregação, visíveis na definição SQL da view e no plano de execução.

A consulta de análises filtra laboratório, situação e período. vw_analises_consulta junta análises, instituição e amostras e primeiro agrupa por análise; o SELECT da tela soma os totais por laboratório. O custo de cada análise é contado uma vez, mesmo que haja várias amostras. Um teste compara esse custo agregado com SUM(custo) da tabela de análises para detectar multiplicação indevida pelo N:N.

![Consulta parametrizada do acervo, com totais por campanha, material e conservação.](docs/evidencias/etapa2/tela-consulta.png)

O relatório de custódia consulta vw_relatorio_custodia, relacionando acervo, custódia vigente, instituição e local opcional. A unicidade de custódia aberta evita duplicar o artefato; LEFT JOIN preserva artefatos ainda sem custódia.

![Relatório consolidado alimentado pela VIEW de custódia.](docs/evidencias/etapa2/tela-relatorio.png)

### Restrições e regras de negócio

Erros SQL são convertidos para mensagens de domínio. Foram tratados, entre outros, duplicidade, FK inexistente, exclusão referenciada, CHECK, valores/datas inválidos, deadlock/timeout e as regras RN03/RN04/RN08/RN17/RN20/RN25. A interface não exibe a exceção bruta do driver.

![Exclusão de campanha com unidades: a FK impede a remoção e a interface informa o motivo.](docs/evidencias/etapa2/tela-restricao.png)

O cadastro de pesquisador coordena supertipo e um único subtipo em uma transação. Na alteração de especialização, o serviço bloqueia o pesquisador, remove o subtipo anterior, altera o supertipo e insere o novo; antes do COMMIT verifica exatamente um subtipo. Falha de unicidade ou de regra desfaz o conjunto, e o teste confirma a preservação do subtipo anterior. O MySQL não tem uma CHECK diferida de totalidade: a garantia de exatamente um subtipo no fim da operação é da aplicação; os triggers impedem incompatibilidade de tipo. Não se afirma que um operador com DML direto irrestrito não possa remover isoladamente um subtipo.

A RN04 é validada na entrada em campanha, e não na inativação posterior de um participante histórico. A consulta 15 foi ajustada para não acusar inativação posterior como violação; o estado atual sozinho não prova que a entrada foi irregular. Os triggers de intervenção verificam conservador e impedem mudar a especialização de quem já possui intervenções. As proteções de coordenador, laboratório e instituição/local da Etapa 1 foram preservadas.

## B2 — Acesso a dados e SQL injection

Database concentra execução/fechamento de Connection, PreparedStatement e ResultSet. Os SELECTs e comandos de negócio são SQL constante, com valores vinculados por setObject/setDate. Concatenar % ao valor de busca produz um parâmetro LIKE, não um comando SQL. MySQL Connector/J recebe useServerPrepStmts=true; o contador Com_stmt_prepare foi observado na sessão do ensaio.

A configuração externa contém db.url e é ignorada pelo Git. Cada usuário informa seu login/senha na interface; a mesma identidade autentica no MySQL e na aplicação. Não há senha de root, senha padrão ou credenciais de todos os perfis embutidas no cliente. As senhas dos ensaios são aleatórias e foram passadas fora do repositório. RegisterAccounts permite cadastrar hashes a partir de contas MySQL já criadas; a senha de manutenção não é usada pela interface.

### Tentativa registrada

Entrada maliciosa no filtro de material: `Cerâmica' OR 1=1 #`. O símbolo # é comentário válido no MySQL, inclusive sem depender de espaço final que a interface remove ao aparar texto. O resultado foi zero linhas, pois o conteúdo inteiro foi tratado como valor do parâmetro. A consulta normal de Cerâmica continua retornando resultados; as tabelas permaneceram íntegras. Também se tentou login malicioso, rejeitado pela autenticação da conta MySQL.

![Filtro malicioso tratado como dado: consulta sem resultados e sem exposição de exceção SQL.](docs/evidencias/etapa2/tela-injecao.png)

O ensaio de preparo e a saída real da tentativa estão em docs/evidencias/etapa2/transacoes-seguranca-auditoria.txt. Os testes de login, perfis e injeção usam MySQL real, não um banco simulado.

## B3 — Transação e concorrência

A operação central é transferir a custódia de um artefato. Ela altera custodia_artefato e artefato em uma única conexão JDBC, com autoCommit=false e READ COMMITTED. Antes de ler a custódia, bloqueia o artefato com SELECT FOR UPDATE. O bloqueio também serializa duas operações quando ainda não existe uma custódia aberta.

Sequência dos comandos, com valores vinculados:

```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SELECT id_artefato, data_descoberta
FROM artefato WHERE id_artefato = ? FOR UPDATE;
SELECT id_custodia, data_inicio
FROM custodia_artefato
WHERE id_artefato = ? AND data_fim IS NULL FOR UPDATE;
UPDATE custodia_artefato SET data_fim = ?
WHERE id_custodia = ?;
INSERT INTO custodia_artefato
(id_artefato,id_instituicao,id_local,id_responsavel,
 data_inicio,documento_movimentacao,motivo)
VALUES (?,?,?,?,?,?,?);
UPDATE artefato SET situacao = ? WHERE id_artefato = ?;
COMMIT;
-- Qualquer erro/falha intermediária: ROLLBACK.
```

O JDBC controla início, COMMIT e ROLLBACK por API; o trecho explicita os comandos equivalentes. A nova data deve ser posterior à data inicial vigente e não pode preceder a descoberta. O modelo aprovado usa DATE e uma unicidade por artefato/data inicial: não foi silenciosamente alterado para suportar movimentos intradia.

### Sucesso e falha real

Foi criado um artefato sintético com situação CATALOGADO e custódia aberta desde 01/10/2026. A falha demonstrativa ocorreu depois do UPDATE que encerrava a custódia e antes do INSERT da nova. O serviço executou ROLLBACK. A consulta posterior mostrou a mesma custódia aberta, nenhuma custódia adicional, situação CATALOGADO e nenhum registro de auditoria persistido dessa tentativa.

No teste com destino de instituição incompatível, a RN25 também falhou depois da primeira alteração, e o rollback preservou o estado. No caminho de sucesso, a custódia anterior foi encerrada em 18/10/2026, uma nova foi inserida com a mesma data de transferência e a situação passou para ARMAZENADO. Permanecem duas linhas históricas, uma única custódia aberta e dois registros de auditoria confirmados. O teste rejeita também um início na mesma data da custódia vigente.

A evidência textual inclui as linhas efetivamente obtidas antes/depois, os identificadores da execução e os snapshots JSON da auditoria. Os registros de ensaio foram mantidos apenas nas bases de teste.

### Duas sessões simultâneas

O experimento abriu duas conexões JDBC reais, confirmou CONNECTION_ID diferente e READ-COMMITTED nas duas. A sessão A adquiriu lock do artefato 1. O SELECT FOR UPDATE de B continuou pendente após 900 ms. Uma terceira conexão de observação consultou performance_schema e encontrou OBJECT_NAME=artefato, INDEX_NAME=PRIMARY, LOCK_TYPE=RECORD, LOCK_MODE=X,REC_NOT_GAP e LOCK_STATUS=WAITING.

Após COMMIT de A, B recebeu o lock depois de aproximadamente 923,660 ms e executou ROLLBACK. Não houve alteração de dados nesse experimento nem deadlock; o comportamento observado foi espera por bloqueio. Um teste de integração adicional confirma que a operação de transferência aguarda o lock e termina depois de sua liberação.

READ COMMITTED foi adotado para reduzir leituras de snapshots antigos entre comandos. Ele sozinho não protege a transferência: a exclusão mútua por artefato vem do SELECT FOR UPDATE e as restrições de unicidade/FK permanecem necessárias. Não é preciso serializar todas as leituras do sistema para esse fluxo. Deadlock e timeout ainda podem ocorrer em outras combinações de operações; a aplicação desfaz a transação e orienta atualização/nova tentativa, sem prometer ausência desses eventos.

## B4 — Três estudos de desempenho

O ensaio usou 20.000 artefatos adicionais, 20.000 análises adicionais, 40.000 associações adicionais e 100.000 eventos de custódia adicionais. Incluindo dados didáticos e fixtures de integração no momento da coleta: 20.108 artefatos, 20.045 análises, 40.100 associações e 100.131 custódias. Cada SQL e seus parâmetros foram guardados junto dos planos.

Foram feitas duas passagens de aquecimento e sete medições antes/depois por consulta, com leitura integral do resultado. As medianas abaixo são tempos JDBC, que incluem ida/volta ao servidor e consumo das linhas; EXPLAIN ANALYZE contém tempos internos do plano, que não devem ser confundidos com essas medianas. A base estava aquecida; não foi um benchmark de cache frio ou de muitos usuários.

| Consulta | Antes (ms) | Depois (ms) | Redução mediana | Linhas retornadas |
|---|---|---|---|---|
| acervo | 2.1342 | 1.5911 | 25.45% | 2 |
| analises | 4.9915 | 2.6145 | 47.62% | 1 |
| custodia | 161.6017 | 124.2144 | 23.14% | 20108 |

### Consulta 1 — Acervo por campanha/material

Antes, o otimizador localizava as duas unidades da campanha pelo índice de unidade/campanha e buscava artefatos com idx_artefato_unidade. O plano observou cerca de 256 candidatos por unidade antes de filtrar Cerâmica e período; restaram 93 linhas, agrupadas em duas linhas de resultado. A consulta não faz varredura completa do acervo, mas ainda lê candidatos desnecessários e os dados da tabela.

Foi criado idx_perf_artefato_consulta(id_unidade,material,data_descoberta,estado_conservacao,profundidade_cm). Unidade e material são igualdades; data é o intervalo; estado e profundidade cobrem agrupamento/média. Depois, o plano usou covering index lookup com unidade/material e cerca de 46,5 linhas por unidade, evitando a leitura da tabela base para esses campos. Agrupamento e ordenação temporários continuaram presentes, e não foram atribuídos ganhos inexistentes a sua remoção.

A mediana passou de 2,1342 para 1,5911 ms. O intervalo mínimo/máximo observado antes foi 1,9641–3,1041 ms e depois 1,4991–2,1131 ms, com alguma sobreposição: o resultado nessa máquina não garante ganho idêntico com outras distribuições ou carga concorrente.

### Consulta 2 — Análises por laboratório/período

Antes, idx_analise_laboratorio buscava 2.005 análises do laboratório e filtrava situação/período até obter 265. A view agregava as amostras por análise e depois a consulta somava por laboratório, preservando a correção dos custos.

Foi criado idx_perf_analise_filtro(id_laboratorio,situacao,data_inicio,custo): igualdades antes do intervalo, custo para cobertura, com a PK incluída implicitamente no índice secundário do InnoDB. Depois, o plano usou covering index range scan com os limites do trimestre, examinando 265 análises. A junção de amostras continuou usando idx_artefato_analise_analise. A mediana JDBC caiu de 4,9915 para 2,6145 ms; a materialização/dupla agregação da view não desapareceu.

### Consulta 3 — Relatório de custódia

O relatório retorna todos os 20.108 artefatos, portanto ainda varre artefato e ordena instituição/catálogo. Antes, procurava cada histórico com idx_custodia_artefato_periodo e filtrava data_fim nula. Foi criado idx_perf_custodia_atual(id_artefato,data_fim,id_instituicao,id_local,data_inicio), com igualdade por artefato/vigência e cobertura das colunas de custódia.

Depois, o plano mostrou covering index lookup em (id_artefato,data_fim=NULL). As buscas de instituição/local permaneceram por PK. A mediana caiu de 161,6017 para 124,2144 ms, mas leitura/transferência de milhares de linhas e ordenação ainda dominam parte do custo. Não se alega tempo constante nem eliminação da ordenação.

### Limites e custo dos índices

Os três índices melhoraram a mediana nesta execução; nenhum resultado negativo foi escondido. Eles ocupam espaço e aumentam o custo de INSERT/UPDATE nas colunas indexadas. Não se extrapolam percentuais para produção ou para consultas sem seletividade. O programa preservou PKs/FKs/índices da Etapa 1 e removeu/recriou somente seus próprios três índices no ensaio. Planos completos antes/depois, EXPLAIN ANALYZE, faixas de tempos, volume e SQL estão em docs/evidencias/etapa2/desempenho/.

## B5 — Segurança e menor privilégio

| Perfil | Interface | Privilégios MySQL | Limites |
|---|---|---|---|
| ADMIN | Quatro CRUDs, equipe, transferência, consultas e auditoria | DML nas tabelas do domínio, leitura de auditoria/views | Não é root; sem DDL, GRANT OPTION, leitura direta de usuario_app ou alteração da auditoria |
| CATALOGADOR | Campanhas, artefatos, locais, equipe, transferência e consultas | Escritas correspondentes e leituras de apoio/referência | Sem administração de pesquisadores, sem e-mails/registros profissionais e sem auditoria |
| CONSULTA | Duas consultas e relatório | SELECT somente nas views autorizadas | Sem DML e sem leitura direta das tabelas pessoais |

O SQL 05 cria as roles, recompõe privilégios com REVOKE e aplica GRANT por tabela/view. Usuários reais são provisionados fora do versionamento e recebem a role padrão. A conta usada na conexão é a do usuário logado: modificar um perfil apenas na memória do cliente não amplia as permissões do MySQL, o que foi verificado por um teste de tentativa de sessão forjada.

vw_minha_conta usa USER(), e não CURRENT_USER() do definer, para retornar somente a conta do cliente. Cada login viu uma linha nessa view, não a relação completa de contas. As views de consulta omitem dados pessoais de pesquisadores. O leitor recebeu código SQL 1142 ao tentar SELECT email FROM pesquisador e ao tentar UPDATE de artefato. A interface apresentou apenas consultas/relatório.

![Perfil CONSULTA em uma sessão real: apenas três telas de leitura.](docs/evidencias/etapa2/tela-perfil-consulta.png)

As senhas da aplicação usam PBKDF2-HMAC-SHA256 com 600.000 iterações, sal aleatório de 16 bytes e digest de 256 bits. O campo armazena algoritmo/parâmetros/sal/digest, nunca uma coluna de senha em texto claro. Os testes confirmam sais diferentes para a mesma senha, senha errada rejeitada e rejeição de codificações inválidas. O MySQL também autentica a conta antes da leitura da view; a aplicação verifica seu próprio hash. Isso não equivale a implementar login web ou autenticação multifator.

Credenciais são informadas na interface ou por variáveis externas durante os ensaios. O arquivo de configuração real, backups e pastas de teste são ignorados pelo Git. A senha é mantida em memória da sessão para abrir conexões individuais e seu array é limpo no logout; o driver/JVM pode manter cópias transitórias, portanto não se promete eliminação de todas as cópias em memória. Não há pooling compartilhando autores entre usuários.

Dados que seriam pessoais em uso real: nome, e-mail, registro profissional e identificação do responsável/usuário vinculada a eventos. A carga é sintética; acessos são limitados à necessidade de cada perfil e backups têm acesso restrito. Textos de descrição/motivo não devem receber dados pessoais desnecessários. Os screenshots/evidências usam exclusivamente dados fictícios.

## B6 — Gatilhos e auditoria

trg_auditoria_artefato registra UPDATE de catálogo, descrição, material, estado/situação e demais valores do artefato. trg_auditoria_custodia registra UPDATE dos vínculos, período e documento/motivo de custódia. A escolha decorre da necessidade de preservar proveniência e responsabilidade sobre o objeto; a cadeia de custódia já mantém registros históricos datados.

Cada linha de auditoria contém autor da aplicação, usuário real de banco, TIMESTAMP(6), tabela, operação, referência e JSON de estados anterior/posterior. USER() registra a conta cliente; CURRENT_USER() dentro do trigger seria o definer e não identificaria o cliente corretamente. @app_actor é definido na conexão a partir da sessão autenticada, sem ser aceito como campo de formulário. A identificação adicional da aplicação é uma declaração do cliente; a identidade MySQL permanece registrada de forma independente.

Os dois triggers são AFTER UPDATE, não uma promessa de auditoria completa de INSERT/DELETE ou de administração global do servidor. A transferência confirma dois registros: encerramento da custódia e alteração do artefato. A tabela auditoria é InnoDB, sem FK para uma conta que possa mudar/desaparecer; guarda a identidade histórica. Nenhum perfil da aplicação tem INSERT/UPDATE/DELETE direto nessa tabela. O teste de falha confirma que as entradas intermediárias da auditoria também são desfeitas no ROLLBACK.

## Adendo lógico das tabelas técnicas

O domínio aprovado com dezesseis tabelas e vinte e duas FKs foi preservado. usuario_app e auditoria são os dois acréscimos técnicos da operação, totalizando dezoito tabelas. As cinco views e vinte e cinco triggers atuais aparecem no SQL complementar. As duas tabelas técnicas não têm uma FK entre si, pois snapshots/identificação histórica da auditoria devem sobreviver a mudanças de contas.

[INSERIR_TECNICO]

## B7 — Projeto físico e armazenamento


### Revisão dos tipos

| Decisão atual | Avaliação e mudança considerada |
|---|---|
| PKs e FKs INT | Adequadas ao volume previsto; não há motivo para ampliar todas para BIGINT. Auditoria usa BIGINT porque tem vida útil longa e eventos acumulativos. |
| Orçamento/custo/massa em DECIMAL | Manter: valores exatos evitam arredondamento binário de FLOAT/DOUBLE em somas de custos e amostras. |
| Latitude/longitude DECIMAL(9,6) | Manter: escala consistente e CHECKs de latitude/longitude já implementados. Não há consulta espacial que justifique mudar para POINT agora. |
| Profundidade DECIMAL(7,2) | Há folga em relação às medições da carga. Redução de precisão poderia economizar pouco e limitar sítios profundos; manter até medir necessidade. |
| Datas do domínio em DATE | Mantêm o domínio aprovado e a regra de um início de custódia por dia. Se houver necessidade real de transferências intradia, migrar datas de custódia para DATETIME(6), rever unicidade, formulários e histórico. A aplicação atual rejeita a segunda transferência na mesma data. |
| BOOLEAN em MySQL | É sinônimo de TINYINT(1), sem proibir outros inteiros automaticamente. Os formulários/serviços só produzem os estados previstos; uma evolução física seria acrescentar CHECK IN (0,1) aos campos booleanos. |
| usuario_app.login VARCHAR(80) | A conta MySQL suporta até 32 caracteres; nossos três logins têm menos que isso. Reduzir para VARCHAR(32) em futura migração alinharia os limites, sem alterar a autenticação. |
| usuario_app.senha_hash VARCHAR(255) | Armazena apenas a codificação PBKDF2, iterações, sal e digest; comporta evolução de algoritmo. A senha não é gravada em coluna separada. |
| Auditoria em JSON + TIMESTAMP(6) | Snapshots preservam estados de forma rastreável e o instante inclui microssegundos. Índices ficam nas colunas de busca (tabela/referência/instante), não em cada chave JSON. |
| Instituição/local na custódia | Mantida a exceção documentada em A5, com RN25 protegida por triggers e consulta de vigência coberta pelo índice de B4. Não se declara BCNF sem ressalvas. |

Os complementos físicos estão em sql/04_operacao.sql, sql/05_perfis.sql e sql/06_indices_desempenho.sql. Não houve geração de tabelas por classes ou ORM.

### Projeção de dois anos

Cenário de planejamento, não previsão medida: 12 campanhas novas por ano, 80 artefatos por campanha, três fragmentos por novo artefato, 0,8 análise por novo artefato e 1,2 associação artefato/análise por nova análise. Cada novo artefato tem uma custódia inicial e duas transferências no horizonte; cada artefato inicial sofre duas transferências. Além disso, cada novo artefato recebe duas atualizações de catálogo, e cada artefato inicial recebe uma. Arredondar a quantidade de associações para cima.

| Tabela | Base didática inicial | Acréscimo em dois anos | Total projetado |
|---|---|---|---|
| campanha | 40 | 12 × 2 = 24 | 64 |
| artefato | 100 | 24 × 80 = 1.920 | 2.020 |
| fragmento | 150 | 1.920 × 3 = 5.760 | 5.910 |
| analise_laboratorial | 45 | 1.920 × 0,8 = 1.536 | 1.581 |
| artefato_analise | 100 | teto(1.536 × 1,2) = 1.844 | 1.944 |
| custodia_artefato | 119 | 1.920 × 3 + 100 × 2 = 5.960 | 6.079 |
| auditoria | 0 | 2 × (1.920 × 2 + 100 × 2) + 1.920 × 2 + 100 = 12.020 | 12.020 |

As duas linhas de auditoria de cada transferência correspondem ao UPDATE da custódia anterior e ao UPDATE do artefato; a auditoria não foi projetada para registrar todo INSERT/DELETE. Atualizações de catálogo acrescentam uma linha por UPDATE. A estimativa pressupõe essas operações; se a frequência mudar, refazer as contas.

Para orçamento conservador de armazenamento, reservar 2–4 KiB por linha de auditoria (snapshots JSON variam com a descrição) gera aproximadamente 24–48 MiB em dois anos, antes de índices e folga. Isso é uma hipótese de dimensionamento, não o AVG_ROW_LENGTH medido em uma tabela quase vazia. Os dados/índices devem ser monitorados por information_schema.tables e as medidas confrontadas com o crescimento real. A carga de desempenho de B4 é maior que a projeção para exercitar os planos, e não representa demanda prevista.

### Backup e retenção

- Backup lógico completo diário às 23h, com --single-transaction, --routines, --triggers e --events. Inclui as dezoito tabelas, cinco views e vinte e cinco triggers atuais. A consistência do snapshot exige tabelas InnoDB e evitar DDL simultâneo.
- Versionar o código/DDL e guardar separadamente a configuração de conexão, a definição de papéis e o procedimento de reprovisionamento de contas. O dump de um esquema não inclui automaticamente usuários/papéis globais do MySQL.
- Guardar backups com acesso restrito (umask 077), pois contêm dados e hashes de autenticação. Manter uma cópia externa criptografada e uma cópia de recuperação independente da máquina da aplicação.
- Retenção proposta: sete diários, quatro semanais e doze mensais. Promover uma cópia semanal e uma mensal antes de expirar os diários; nunca apagar o último backup validado por engano.
- Objetivo inicial de perda máxima (RPO): até 24 horas. Objetivo de recuperação (RTO): uma hora, a validar no computador do grupo. Exigência menor de perda de dados demandaria coleta de binary logs e restauração ponto a ponto, que não foi implementada nem alegada neste projeto.
- Teste de restauração mensal e depois de mudanças de esquema: restaurar em servidor separado, comparar contagens, executar consultas de views, exercitar triggers e conferir usuários/permissões reprovisionados.

scripts/backup.sh gera o dump comprimido com credenciais em arquivo de cliente protegido fora do Git. scripts/backup_restauracao.py executa o ensaio em containers novos e não substitui um servidor existente. O ensaio real e suas contagens estão em docs/evidencias/etapa2/restauracao.txt: backup de cerca de 2 MB gzip, restauração em 4,455 s no ambiente de desenvolvimento, 100.131 custódias, 20.108 artefatos, views executáveis e auditoria restaurada verificada com ROLLBACK. Esse tempo observado não garante o RTO em outra máquina ou carga.

### Histórico e particionamento

No volume projetado, manter as tabelas operacionais e índices atuais. Custódias encerradas continuam necessárias para rastreabilidade, e não devem ser apagadas só para reduzir tamanho.

Se a auditoria crescer para milhões de eventos e o acesso recente dominar, avaliar arquivamento por ano e consultas por intervalo de instante. Particionamento por data da auditoria exigiria revisar a PK: no MySQL, toda chave UNIQUE deve incluir a coluna de particionamento; a PK atual apenas em id_auditoria não atende a essa condição. Não basta acrescentar PARTITION BY ao DDL existente.

Tabelas InnoDB que participam de FKs têm restrições ao particionamento definido pelo usuário no MySQL 8. Por isso não se propõe particionar custodia_artefato mantendo o mesmo conjunto de FKs. Uma alternativa futura é um repositório histórico separado, com política de leitura e restauração próprias, após aprovação da mudança no domínio e demonstração de ganho.

## B8 — Consolidação, validação e apresentação

O relatório incorpora, como anexo paginado, os artefatos A1–A5 revisados da Etapa 1, incluindo o registro C1–C5, os modelos aprovados e a declaração inicial de IA. O anexo preserva o contexto histórico da primeira etapa; o estado operacional atual e a alteração de interpretação da RN04 são os descritos nas seções B deste relatório.

O roteiro em docs/roteiro-demonstracao.md distribui quinze minutos conforme o enunciado e inclui preparação de ambiente, duas sessões, cenário de falha, acesso por perfis e leitura de planos. Não foram inventadas contribuições individuais, apresentação realizada ou confirmação do professor.

### Testes executados

Total: 13 testes, 0 falhas, 0 erros e 0 ignorados. Foram executados dez testes de integração em MySQL real, um de senhas e dois de Swing em display virtual. A interface exercitou login, um CRUD completo, consultas, relatório, entrada maliciosa, restrição de FK e perfil somente leitura; a integração exercitou os quatro CRUDs, alterações de subtipo, participação ativa, restrições, perfis reais, auditoria, rollback e espera de lock. Não se afirma que cada combinação possível de campo/erro tenha sido testada.

A regressão da Etapa 1 também reconstruiu o banco em servidor separado e executou os scripts/quinze consultas e testes de integridade. A restauração comparou todas as contagens de tabelas, views e triggers, executou views restauradas e confirmou auditoria 10 → 11 → 10 durante UPDATE/ROLLBACK; usuários/papéis foram reprovisionados separadamente do dump do esquema.

### Reconstrução e execução

O README da raiz cobre preparação com Docker ou MySQL instalado, execução do JAR e abertura do Maven no NetBeans. app/README.md explica as telas e configuração. scripts/preparar_etapa2.py reconstrói uma base indicada explicitamente, solicita senhas sem exibi-las e não salva senhas na configuração. scripts/validar_etapa2.py usa um servidor reservado de teste e credenciais aleatórias em memória; sem variáveis/display, o Maven comum sinaliza testes de integração/Swing ignorados, em vez de alegar aprovação.

A carga grande é separada da demonstração. As evidências de desempenho, transação e restauração foram coletadas em momentos diferentes; por isso as contagens de artefatos de ensaio podem aumentar depois da medição, sem que isso altere retroativamente o volume registrado em B4. As saídas brutas informam os dados de cada etapa.

### Uso de inteligência artificial e revisão do grupo

A ferramenta de IA auxiliou na implementação Java/Swing/JDBC, SQL complementar, testes, experimentos, organização das evidências e redação do relatório. As verificações descritas foram executadas no ambiente de desenvolvimento com banco e interface reais. Os integrantes devem revisar o código e os resultados, confirmar o funcionamento em sua máquina e saber explicar as decisões e limitações na arguição. Esta declaração não transfere ao assistente a participação individual exigida pela disciplina.

A entrega no AVA e a apresentação ainda serão realizadas pelo grupo: prazo previsto de 22/11/2026 às 23h59, com apresentação em 23/11/2026.

## Anexo — Etapa 1 revisada e modelos aprovados

A seguir, o relatório revisado da Etapa 1 é incorporado integralmente, mantendo o histórico de correção da devolutiva. A numeração de páginas deste relatório final é contínua.

[INSERIR_ETAPA1]
