# Registro de revisão da devolutiva da Etapa 1

Data da revisão: 18/10/2026. Escopo: correção de C1 a C5; a aplicação e os demais artefatos operacionais da Etapa 2 ainda não foram implementados.

| Critério | Devolutiva | Correção realizada | Evidência |
|---|---|---|---|
| C1 | Diagramas pequenos; cardinalidades ilegíveis/ausentes; especialização isolada | MER Chen vetorial, visão completa e detalhes com atributos; cardinalidades verificáveis; especialização total e disjunta por símbolo nativo; fontes PlantUML sincronizadas | docs/mer-conceitual.pdf; fontes PlantUML; seção A2 |
| C2 | Formalizar dependências; discutir 3FN versus BCNF | Dependências X -> Y por relação e por chave candidata; definições formais; análise explícita da redundância de custódia e da coluna gerada | seção A5; docs/fontes/normalizacao.md |
| C3 | Acrescentar idx_ e triggers para coordenador, laboratório e local | Quatro índices compostos (um substitui índice simples); nove triggers adicionais, incluindo proteção de alterações no lado referenciado; comentários RN corrigidos | sql/01_ddl.sql; testes de INSERT/UPDATE e alterações de pais |
| C4 | Variar descrições e contornos; melhorar consulta 5; correlação explícita | Cem descrições distintas de artefatos, materiais coerentes e datas encadeadas; contornos verificáveis; consulta 5 de triagem por risco; consulta 11 escalar correlacionada por laboratório | sql/02_carga.sql; sql/03_consultas.sql; evidências de validação |
| C5 | Rotular A1–A5, dicionário legível e estrutura do repositório | Relatório com seções A1–A5, dicionário integral com cabeçalhos repetidos e fonte legível, estrutura comentada e modelos em páginas vetoriais próprias | docs/relatorio-etapa1.pdf; README.md |

## Evidência de verificação

Os três scripts foram executados em sequência no MySQL 8.4.11, em base limpa e novamente para verificar reconstrução. As quinze consultas executaram sem erro; a consulta 15 retornou zero violações. Nove testes negativos cobrem INSERT/UPDATE dos filhos e alteração dos pais nas RN08/RN17/RN25; também foram exercitados casos válidos com ROLLBACK. Há 16 tabelas, 22 FKs, 28 CHECKs e 11 triggers. A carga mantém os mínimos exigidos e possui 119 registros de custódia: o artefato 100 ainda aguarda a primeira custódia.

O teste reproduzível está em tests/validar_etapa1.py e a saída em docs/evidencias/validacao-etapa1.txt. A criação dos PDFs está em scripts/gerar_documentos.py e scripts/gerar_modelos.py.

## Incorporação futura ao relatório final

Este registro e as seções revisadas A1–A5 devem compor o relatório final da Etapa 2. A inclusão desse registro documenta a correção da devolutiva; não afirma que B1–B8 estejam concluídos. Permanecem para a aplicação futura as validações completas de especialização e participação de pesquisadores ativos. A consulta 15 continua sendo uma verificação complementar, não um substituto dessas validações.
