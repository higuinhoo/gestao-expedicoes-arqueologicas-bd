# Checklist antes da entrega

- [x] Executar `sql/01_ddl.sql` em uma instância MySQL 8 limpa.
- [x] Executar `sql/02_carga.sql` e conferir as contagens exibidas ao final.
- [x] Executar as quinze consultas de `sql/03_consultas.sql`.
- [x] Confirmar que a consulta 15 não retorna inconsistências.
- [ ] Abrir os quatro PDFs e conferir nomes, páginas e legibilidade.
- [ ] Todos os integrantes devem revisar as 25 regras de negócio.
- [ ] Cada integrante deve conseguir explicar a generalização, a entidade fraca, os dois relacionamentos N:N e o histórico de custódia.
- [ ] Conferir no AVA se houve alteração de prazo ou formato.
- [ ] Criar o repositório remoto e enviar seu link no AVA junto com o relatório.
- [ ] Manter no relatório a declaração de uso de inteligência artificial e registrar as verificações realmente realizadas pela equipe.

## Validação já realizada

- Os três arquivos SQL foram analisados estaticamente no dialeto MySQL.
- Os três arquivos foram executados, na ordem, em MySQL Community Server 8.4.11.
- Foram confirmadas 16 tabelas, 22 chaves estrangeiras, 28 restrições `CHECK`, 2 triggers e 15 consultas numeradas.
- Todas as chaves estrangeiras possuem `ON UPDATE` e `ON DELETE` explícitos.
- As quantidades da carga foram verificadas no arquivo: 40 instituições, 40 pesquisadores, 40 sítios, 40 campanhas, 120 participações, 80 unidades, 100 artefatos, 150 fragmentos, 45 análises, 100 associações de análise, 60 intervenções, 40 locais e 120 registros de custódia.
- A consulta de auditoria retornou zero violações semânticas e não há artefatos com mais de uma custódia aberta.
- As triggers foram testadas e rejeitaram corretamente uma tentativa de autorreferência de artefato.

## Validação pendente pela equipe

Permanecem apenas a revisão humana do conteúdo, a conferência dos PDFs, a criação do repositório remoto e o envio no AVA.
