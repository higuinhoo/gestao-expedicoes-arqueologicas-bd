# Sistema de Gestão de Expedições Arqueológicas

Projeto final da disciplina Laboratório de Banco de Dados — 2026/2.

## Integrantes

- Arthur Tavares Silva
- Higor Samuel Ferreira Silva
- Igor Vendramini Perini
- João Gabriel da Cruz Viana

## Sobre o projeto

O banco controla expedições arqueológicas do começo ao fim: sítios, campanhas, equipes, unidades de escavação, artefatos encontrados, fragmentos, análises em laboratório, intervenções de conservação e quem está responsável pelo artefato em cada momento. Os dados da carga são fictícios.

## Requisitos

- MySQL 8.0 ou superior
- Engine InnoDB
- Charset utf8mb4

## Como rodar

Execute os três scripts nessa ordem dentro do MySQL:

```sql
SOURCE sql/01_ddl.sql;
SOURCE sql/02_carga.sql;
SOURCE sql/03_consultas.sql;
```

O `01_ddl.sql` já dropa e recria tudo, então pode rodar de novo sem problema. O `02_carga.sql` termina mostrando a contagem de registros em cada tabela.

## Testado em

MySQL Community Server 8.4.11 — rodou sem erros do zero. Ficaram 16 tabelas, 22 chaves estrangeiras, 28 CHECKs e 2 triggers. Todas as 15 consultas retornaram resultado.

## Arquivos

```
sql/
  01_ddl.sql          criação do banco
  02_carga.sql        dados de exemplo
  03_consultas.sql    15 consultas comentadas

docs/
  relatorio-etapa1.pdf   relatório completo
  mer-conceitual.pdf     modelo entidade-relacionamento
  modelo-logico.pdf      esquema relacional
  dicionario-dados.pdf   descrição de cada tabela e atributo
  fontes/                versões editáveis (Markdown e Mermaid)
```
