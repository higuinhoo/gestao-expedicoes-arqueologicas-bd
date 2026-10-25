# Plano para concluir o projeto — Etapa 2

Base da conferência: PDF da disciplina, seção 6.1 (B1–B8), anexos de entrega e repositório local no commit d2f0f00. Plano elaborado em 18/10/2026. O grupo informou preferência por Java e interface no NetBeans. O planejamento inicial foi executado; consulte a atualização e as evidências abaixo para o estado atual.

## Atualização de execução

Implementação concluída em 18/10/2026: aplicação Java/Swing/JDBC, SQL 04–06, papéis reais, auditoria, 13 testes aprovados (incluindo Swing), experimento de concorrência, três estudos de EXPLAIN e ensaio de restauração. As evidências estão em docs/evidencias/etapa2/; os procedimentos de execução estão em README.md e app/README.md. O texto abaixo preserva o planejamento inicial e não é a lista de pendências atual. A entrega no AVA, execução no computador do grupo e apresentação cabem aos integrantes.

## O que já está pronto

- Etapa 1 revisada conforme C1–C5: SQL, carga, quinze consultas, dependências funcionais e documentação.
- Modelos conceitual e lógico aprovados pelo usuário, renderizados em PDF/PNG e incorporados ao relatório.
- Integridade reforçada por onze triggers e índices: RN08, RN17 e RN25 têm bloqueios em gravações e alterações de pais.
- Testes da Etapa 1 e evidências existentes em docs/evidencias/.

O teste de duas conexões usado na revisão verifica integridade contra snapshot antigo. Ele não substitui o experimento B3 da operação de negócio. Os onze triggers atuais são de integridade, não os triggers de auditoria exigidos em B6. Os índices existentes não substituem os três estudos de desempenho de B4.

## Tecnologia proposta

Aplicação desktop em Java 17, Swing e JDBC, abrindo no NetBeans. Maven organiza as dependências e a execução; não exige Spring nem um framework de interface. O MySQL 8 permanece como SGBD, conforme a regra de continuidade do enunciado. O esquema continua autoral em SQL e não será gerado por classes Java.

A interface do NetBeans normalmente é Swing. Como não há um projeto Java prévio neste repositório, a proposta é iniciar um projeto Maven padrão com janelas/painéis Swing. Não se assume uma exigência de arquivos do GUI Builder (.form) sem confirmação do grupo.

Separação sugerida: telas Swing, serviços de negócio/transações, DAOs com SQL explícito e classes de dados. A aplicação não depende dos scripts Python de geração dos PDFs para executar.

## Matriz de requisitos e entregas

| Item | Situação atual | Entrega necessária | Evidência de aceitação |
|---|---|---|---|
| B1 — Interface | Não implementada | Quatro CRUDs, duas consultas com filtros/junção/agregação, relatório por VIEW e tratamento de erros | Operações exercitadas pela interface, incluindo remoção impedida por FK com mensagem compreensível |
| B2 — Acesso a dados | Não implementado | JDBC com PreparedStatement; credenciais externas; teste de injeção | Entrada maliciosa e resultado real registrados; arquivo real de configuração não versionado |
| B3 — Transações | Falta operação de negócio e experimento específico | Transferência de custódia em duas tabelas; sucesso, falha intermediária e duas conexões | Estado antes/depois, COMMIT/ROLLBACK, espera observada e isolamento registrado |
| B4 — Desempenho | Existem índices, mas não estudos | Três consultas críticas com EXPLAIN antes/depois, justificativa e tempos | Planos brutos, interpretação, volume, procedimento e medições reproduzíveis |
| B5 — Acesso e senhas | Não implementados | Três perfis MySQL com GRANT/REVOKE, VIEW restritiva, login e hash com sal | Logins reais no banco, permissões aceitas/negadas e testes da interface |
| B6 — Auditoria | Não implementada | Dois triggers de auditoria e tabela com autor/data/valores anterior e posterior | Mudanças reais capturadas e verificação de rollback dos registros transacionais |
| B7 — Projeto físico | Falta documentação operacional | Tipos, projeção de dois anos, backup/retenção/restauração e avaliação de histórico | Decisões justificadas, premissas de crescimento e ensaio real de restauração |
| B8 — Relatório/demonstração | Revisão da Etapa 1 pronta | Consolidar B1–B7, revisão C1–C5, IA e demonstração de quinze minutos | Relatório final PDF e roteiro com aplicação/banco funcionando |

## Escopo funcional proposto

### Quatro CRUDs completos

1. Pesquisadores e sua especialização: criar, consultar, alterar e remover supertipo/subtipo de forma coordenada. Isso permite tratar a RN03, que ainda depende da auditoria semântica.
2. Campanhas: coordenador arqueólogo, sítio, período e situação; incluir gestão da participação da equipe como operação complementar, verificando pesquisador ativo na entrada (RN04).
3. Artefatos: unidade, catálogo, material, descoberta, situação e referência a outro artefato.
4. Locais de armazenamento: instituição, código, tipo e condições de armazenamento.

Instituições, sítios e unidades existentes podem inicialmente alimentar seletores. O mínimo de quatro CRUDs será demonstrado com as quatro entidades acima. Não será necessário criar interface completa para todas as dezesseis tabelas.

A inativação posterior de um pesquisador não deve invalidar automaticamente sua participação histórica. A RN04 precisa ser aplicada à inclusão em nova campanha. O responsável por uma intervenção deve ser conservador (RN20), e a escrita dos subtipos deve preservar exatamente um subtipo compatível por pesquisador.

### Consultas parametrizadas

- Acervo por campanha: filtros de sítio/campanha, material e período; junção campanha–unidade–artefato, agrupamentos e totais.
- Análises por laboratório: filtros de instituição, situação e período; junções e agregação de quantidades/massa. Custos não podem ser somados repetidamente por causa do N:N; agregar cada análise antes do total por laboratório.

Os filtros serão valores vinculados a PreparedStatement. Nomes de tabelas/colunas e comandos SQL não serão obtidos de texto informado pelo usuário.

### Relatório consolidado

Criar uma VIEW do acervo sob custódia vigente, relacionando artefato, instituição e local opcional. A tela de relatório consumirá essa VIEW. Criar também uma VIEW destinada ao perfil de consulta, com apenas as colunas autorizadas, sem expor e-mails e registros profissionais de pesquisadores.

## Operação central: transferir custódia

Entrada: artefato, instituição/local de destino, responsável, data, documento, motivo e situação do artefato após a transferência.

1. Iniciar a transação com autoCommit desativado e isolamento definido.
2. Bloquear o artefato com SELECT ... FOR UPDATE, serializando transferências do mesmo artefato.
3. Ler e validar a custódia vigente, o destino, o responsável e as datas.
4. Encerrar a custódia anterior.
5. Inserir a nova custódia.
6. Atualizar a situação do artefato coerentemente com a operação.
7. Confirmar com COMMIT; em qualquer falha, executar ROLLBACK e restaurar a conexão.

A operação altera custodia_artefato e artefato, atendendo ao requisito de duas tabelas. Para a demonstração, escolher um caso em que a situação do artefato realmente mude.

O esquema aprovado usa DATE e unicidade (id_artefato, data_inicio): ele não representa várias custódias iniciadas no mesmo dia para um mesmo artefato. A aplicação deve respeitar essa granularidade, informar a limitação e preparar as demonstrações com datas válidas. Não converter para DATETIME silenciosamente.

A falha demonstrada deve ocorrer depois de uma primeira alteração e antes do COMMIT; não basta rejeitar um campo antes de iniciar a transação. Comparar a custódia anterior, a ausência da nova custódia e a situação do artefato após ROLLBACK.

## Segurança e auditoria

- Perfis propostos: administrador operacional, catalogador e consulta. Administrador operacional não significa usuário root ou permissão global no MySQL.
- Definir uma matriz de permissões por ação e testá-la tanto na interface quanto com credenciais reais dos perfis MySQL. Esconder botões não substitui o controle na camada de serviços/banco.
- Guardar senhas da aplicação com hash e sal, por exemplo PBKDF2 com gerador seguro de sal, parâmetros versionados e comparação apropriada.
- Arquivo de configuração real fora do Git; fornecer apenas exemplo sem segredos.
- Duas auditorias propostas: mudanças relevantes de artefato e alterações de custódia. Registrar identificação do usuário autenticado, usuário do banco, data/hora e estados anterior/posterior, justificando o interesse de rastreabilidade arqueológica.
- Garantir que a identificação do autor não vaze entre conexões reaproveitadas e que a auditoria transacional acompanhe COMMIT/ROLLBACK.
- Identificar nomes, e-mails e registros profissionais como dados pessoais; a carga continuará sintética.

As tabelas técnicas de autenticação e auditoria serão adicionadas por SQL autoral separado. O modelo de domínio aprovado será preservado; a documentação lógica final precisará incluir um adendo dessas tabelas técnicas, para continuar correspondendo ao banco real.

## Experimentos e documentação

### Concorrência

Usar duas conexões JDBC ou duas sessões MySQL efetivamente simultâneas sobre o mesmo artefato. Registrar identificadores das sessões, isolamento, operação executada e bloqueio/espera observados. READ COMMITTED com bloqueio explícito do artefato é uma proposta inicial; sua escolha deve ser justificada e confirmada nos testes. Não registrar um resultado esperado como se tivesse sido observado.

### Desempenho

Selecionar três consultas realmente usadas pela aplicação: acervo por campanha/material, análises por laboratório/período e custódias vigentes por instituição. Preparar carga de desempenho separada e reproduzível, respeitando as restrições; a pequena carga didática pode não produzir diferença de tempo significativa.

Para cada consulta, guardar SQL, parâmetros, número de linhas, EXPLAIN antes, índice proposto, EXPLAIN depois e tempos comparáveis. Não remover PKs, índices necessários às FKs ou restrições para fabricar ganho. Relatar ausência de melhoria quando ocorrer, incluindo a razão provável e os limites da medição.

### Projeto físico e backup

Revisar os tipos atuais; estimar crescimento para dois anos explicitando premissas de artefatos/expedição, análises e eventos de custódia. Definir o que entra no backup, frequência, retenção e recuperação pretendida. Executar restauração em banco separado e verificar contagens, consultas, views e triggers. Avaliar arquivamento da auditoria/histórico antes de propor particionamento; documentar as restrições de FKs do MySQL na proposta.

## Ordem de implementação

1. Projeto Java/NetBeans, configuração externa, conexão JDBC e diagnóstico de inicialização.
2. Complementos SQL: tabelas técnicas, views, perfis e auditoria; login e serviços de autorização.
3. Quatro CRUDs e validações de negócio, com erros de restrição compreensíveis.
4. Duas telas de consulta, relatório por VIEW e transferência de custódia.
5. Testes de sucesso/falha, injeção, perfis reais e concorrência; coleta das evidências.
6. Estudos EXPLAIN e tempos, backup/restauração, crescimento e projeto físico.
7. Relatório final PDF e roteiro da demonstração com ambiente reconstruível.

## Arquivos previstos

- app/pom.xml e app/src/main/java/: projeto Java com Swing, serviços e DAOs JDBC.
- app/config.example.properties: exemplo de configuração, sem credenciais reais.
- sql/04_operacao.sql: tabelas técnicas, views e gatilhos da Etapa 2.
- sql/05_perfis.sql: papéis/permissões; credenciais de usuários reais fornecidas fora do Git.
- tests/ e scripts/: verificação e experimentos reproduzíveis, com escopo destrutivo limitado a bases de teste.
- docs/evidencias/etapa2/: resultados efetivamente obtidos.
- docs/fontes/relatorio-final.md e docs/relatorio-final.pdf: consolidação da Etapa 1 revisada e da Etapa 2.
- docs/roteiro-demonstracao.md: roteiro de quinze minutos conforme seção 6.2 do enunciado.

## Critério de conclusão

O trabalho só estará completo quando uma máquina limpa conseguir reconstruir o banco e iniciar a aplicação pelo README, todos os cenários exigidos funcionarem e o relatório contiver as evidências reais. A entrega prevista no enunciado é 22/11/2026, até 23h59; apresentação em 23/11/2026.
