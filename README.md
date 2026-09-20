# Sistema de Gestão de Expedições Arqueológicas

Projeto da Etapa 1 da disciplina Laboratório de Banco de Dados, semestre 2026/2.

## Integrantes

- Arthur Tavares Silva
- Higor Samuel Ferreira Silva
- Igor Vendramini Perini
- João Gabriel da Cruz Viana

## Domínio

O sistema registra expedições arqueológicas, unidades de escavação, artefatos, fragmentos, análises laboratoriais, intervenções de conservação e a cadeia temporal de custódia. Todos os dados da carga são fictícios.

## Requisitos

- MySQL 8.0 ou superior
- Engine InnoDB
- Conjunto de caracteres utf8mb4

## Execução

Execute os arquivos nesta ordem em uma instância MySQL 8:

```sql
SOURCE sql/01_ddl.sql;
SOURCE sql/02_carga.sql;
SOURCE sql/03_consultas.sql;
```

O script `01_ddl.sql` pode ser executado novamente: ele remove as tabelas do projeto em ordem segura e recria o esquema. O script `02_carga.sql` cria dados sintéticos e encerra a carga com consultas de contagem.

## Compatibilidade verificada

Os três scripts foram executados integralmente e em sequência no MySQL Community Server 8.4.11. A validação confirmou 16 tabelas, 22 chaves estrangeiras, 28 restrições `CHECK`, 2 triggers, as quantidades previstas da carga e a execução sem erros das quinze consultas.

## Estrutura

- `docs/relatorio-etapa1.pdf`: escopo, regras, modelos e normalização.
- `docs/mer-conceitual.pdf`: modelo conceitual em notação de Engenharia da Informação.
- `docs/modelo-logico.pdf`: esquema relacional com chaves.
- `docs/dicionario-dados.pdf`: descrição de tabelas e atributos.
- `docs/fontes/`: fontes editáveis dos diagramas e versões DOCX.
- `sql/01_ddl.sql`: criação completa do banco.
- `sql/02_carga.sql`: carga de dados fictícios.
- `sql/03_consultas.sql`: quinze consultas comentadas.

## Verificação antes da entrega

1. Executar o DDL em uma instância MySQL 8 limpa.
2. Executar a carga e conferir as contagens.
3. Executar as quinze consultas e inspecionar os resultados.
4. Ler o relatório e confirmar que todos os integrantes conseguem explicar as decisões.
5. Registrar no relatório como a equipe validou o material produzido com apoio de IA.
