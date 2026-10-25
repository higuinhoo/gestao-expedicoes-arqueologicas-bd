# B8 — Demonstração de quinze minutos

## Preparação

1. Executar iniciar.bat (Windows) ou sh iniciar.sh (Linux/macOS) antes da apresentação. O ambiente demo prepara a base, a configuração e os acessos admin/admin, catalogador/catalogador e consulta/consulta, preservando os dados nas próximas inicializações.
2. Para NetBeans, executar preparar-netbeans.bat ou sh iniciar.sh --prepare-only; abrir app/ com JDK 17+ e executar br.edu.ucb.arqueologia.App. Os arquivos de acessos pessoais não precisam ser projetados. O roteiro abaixo também pode ser executado diretamente pelo JAR iniciado automaticamente.
3. Manter duas sessões MySQL autenticadas para o experimento de concorrência. Não usar uma conexão única com duas abas que compartilhem a mesma sessão. Confirmar com SELECT CONNECTION_ID().
4. Preparar um artefato sintético novo, com situação CATALOGADO e uma custódia anterior em data menor que a transferência. Para criar essa custódia inicial, usar a tela Transferir custódia quando ainda não há custódia vigente. Datas podem ser ajustadas à apresentação; não tentar dois inícios no mesmo dia para o mesmo artefato.
5. Separar a base grande de desempenho da base didática da aplicação. Ter abertos os planos antes/depois e a tabela de medições; não executar carga de 20 mil artefatos enquanto demonstra cadastros.
6. Abrir o relatório final PDF e os arquivos em docs/evidencias/etapa2/. Repartir os momentos e ensaiar; todos os integrantes devem saber explicar qualquer parte.

## Tempo e ações

| Momento | Tempo | Demonstração |
|---|---|---|
| Contexto | 2 min | Domínio, MER aprovado, mapeamento dos N:N, entidade fraca e correções C1–C5. Mostrar o adendo das duas tabelas técnicas. |
| CRUD e consultas | 4 min | Login admin; criar/alterar/remover um local novo, abrir os demais CRUDs; executar consulta de acervo com filtros e a de análises; abrir relatório de custódia alimentado por VIEW. |
| Transação | 3 min | Primeiro marcar falha intermediária, confirmar a mensagem e mostrar custódia/situação inalteradas; depois transferir sem falha, confirmar duas tabelas alteradas e visualizar auditoria. |
| Restrições | 2 min | Tentar remover campanha com unidades; mostrar mensagem de FK tratada. Explicar RN08/RN17/RN25 e, se houver tempo, local de instituição incompatível. |
| Desempenho e acesso | 3 min | Mostrar EXPLAIN antes/depois de uma das três consultas, índice e mediana; alternar login admin/consulta para provar ausência de CRUDs e acesso só a views. Executar ou exibir o experimento de duas sessões com espera observada. |
| Arguição | 1 min | Respostas dos integrantes sorteados. |

## Duas sessões, ao vivo

Sessão A:

```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT CONNECTION_ID(), @@transaction_isolation;
START TRANSACTION;
SELECT id_artefato FROM artefato WHERE id_artefato = 1 FOR UPDATE;
-- Manter aberta enquanto a sessão B executa seu SELECT.
COMMIT;
```

Sessão B, antes do COMMIT de A:

```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT CONNECTION_ID(), @@transaction_isolation;
START TRANSACTION;
SELECT id_artefato FROM artefato WHERE id_artefato = 1 FOR UPDATE;
-- A consulta espera A liberar o lock; depois retorna.
ROLLBACK;
```

No ensaio já registrado, o servidor mostrou LOCK_STATUS=WAITING no índice PRIMARY e a segunda conexão esperou cerca de 924 ms. Na aula, relatar o tempo efetivamente observado, sem reproduzir esse número como promessa. READ COMMITTED não elimina locks de escrita: o bloqueio explícito protege a operação por artefato até o COMMIT/ROLLBACK.

## Conferência final do grupo

- Executar em outra máquina seguindo README/app/README; conferir JDK, Maven, host/porta e logins individuais.
- Verificar dados coerentes nas duas consultas e não somar custos duplicados pelo N:N.
- Garantir que o perfil consulta não veja e-mail/registro nem consiga gravar diretamente no banco.
- Conferir declaração de IA e registro de revisão da Etapa 1 no relatório final.
- Entregar link do repositório acessível ao professor e PDF no AVA até 22/11/2026 às 23h59; apresentação prevista em 23/11/2026.
