# B7 — Projeto físico, crescimento e recuperação

## Revisão dos tipos

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

## Projeção de dois anos

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

## Backup e retenção

- Backup lógico completo diário às 23h, com --single-transaction, --routines, --triggers e --events. Inclui as dezoito tabelas, cinco views e vinte e cinco triggers atuais. A consistência do snapshot exige tabelas InnoDB e evitar DDL simultâneo.
- Versionar o código/DDL e guardar separadamente a configuração de conexão, a definição de papéis e o procedimento de reprovisionamento de contas. O dump de um esquema não inclui automaticamente usuários/papéis globais do MySQL.
- Guardar backups com acesso restrito (umask 077), pois contêm dados e hashes de autenticação. Manter uma cópia externa criptografada e uma cópia de recuperação independente da máquina da aplicação.
- Retenção proposta: sete diários, quatro semanais e doze mensais. Promover uma cópia semanal e uma mensal antes de expirar os diários; nunca apagar o último backup validado por engano.
- Objetivo inicial de perda máxima (RPO): até 24 horas. Objetivo de recuperação (RTO): uma hora, a validar no computador do grupo. Exigência menor de perda de dados demandaria coleta de binary logs e restauração ponto a ponto, que não foi implementada nem alegada neste projeto.
- Teste de restauração mensal e depois de mudanças de esquema: restaurar em servidor separado, comparar contagens, executar consultas de views, exercitar triggers e conferir usuários/permissões reprovisionados.

scripts/backup.sh gera o dump comprimido com credenciais em arquivo de cliente protegido fora do Git. scripts/backup_restauracao.py executa o ensaio em containers novos e não substitui um servidor existente. O ensaio real e suas contagens estão em docs/evidencias/etapa2/restauracao.txt: backup de cerca de 2 MB gzip, restauração em 4,455 s no ambiente de desenvolvimento, 100.131 custódias, 20.108 artefatos, views executáveis e auditoria restaurada verificada com ROLLBACK. Esse tempo observado não garante o RTO em outra máquina ou carga.

## Histórico e particionamento

No volume projetado, manter as tabelas operacionais e índices atuais. Custódias encerradas continuam necessárias para rastreabilidade, e não devem ser apagadas só para reduzir tamanho.

Se a auditoria crescer para milhões de eventos e o acesso recente dominar, avaliar arquivamento por ano e consultas por intervalo de instante. Particionamento por data da auditoria exigiria revisar a PK: no MySQL, toda chave UNIQUE deve incluir a coluna de particionamento; a PK atual apenas em id_auditoria não atende a essa condição. Não basta acrescentar PARTITION BY ao DDL existente.

Tabelas InnoDB que participam de FKs têm restrições ao particionamento definido pelo usuário no MySQL 8. Por isso não se propõe particionar custodia_artefato mantendo o mesmo conjunto de FKs. Uma alternativa futura é um repositório histórico separado, com política de leitura e restauração próprias, após aprovação da mudança no domínio e demonstração de ganho.
