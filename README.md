# Sistema de Gestão de Expedições Arqueológicas

Projeto da Etapa 1 da disciplina Laboratório de Banco de Dados, semestre 2026/2, revisado conforme a devolutiva C1–C5 em 18/10/2026. A implementação da aplicação da Etapa 2 será realizada posteriormente.

## MER editável no draw.io

A [nova versão `.drawio`](docs/fontes/mer-conceitual.drawio) possui uma visão completa e quatro abas de detalhe, com posicionamento organizado e objetos editáveis. Consulte a [prévia em PDF](docs/mer-drawio-previa.pdf) e as [instruções para abrir no diagrams.net](docs/como-abrir-mer-drawio.md).

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

- **C1:** MER vetorial em página inteira A1, cardinalidades nos dois extremos, especialização T,X integrada ao pesquisador e fontes editáveis sincronizadas. O relatório inclui o MER sem reduzi-lo a uma miniatura A4 e uma tabela textual de todas as cardinalidades.
- **C2:** dependências funcionais `X -> Y` por relação e discussão formal de 3FN versus BCNF. A redundância de instituição/local de custódia e a coluna física gerada são explicitadas; não se afirma normalização integral sem ressalvas.
- **C3:** índices compostos `idx_` e nove triggers adicionais para RN08, RN17 e RN25, inclusive alterações nos registros referenciados. Os dois triggers originais de autorreferência foram preservados.
- **C4:** descrições variadas, materiais e cronologia coerentes, casos de contorno verificáveis, consulta 5 de triagem por risco e consulta 11 com subconsulta escalar correlacionada explícita.
- **C5:** relatório com A1–A5, dicionário integral legível, estrutura do repositório e registro das revisões.

O [registro completo da revisão](docs/revisao-devolutiva-etapa1.md) deve ser incorporado ao relatório final da Etapa 2 junto com A1–A5 revisados. A seção de uso de IA está no relatório. RN03/RN04 e a validação completa de RN20 ainda exigem tratamento pela aplicação futura; a consulta 15 continua como verificação complementar.

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
sql/
  01_ddl.sql                   # Esquema autoral, restrições, índices e triggers
  02_carga.sql                 # Dados sintéticos e casos de contorno
  03_consultas.sql             # 15 consultas comentadas
docs/
  relatorio-etapa1.pdf         # A1–A5, validação e revisão da devolutiva
  mer-conceitual.pdf           # Uma página A1 vetorial, com fontes editáveis
  modelo-logico.pdf            # Uma página A1 vetorial
  dicionario-dados.pdf         # Também incorporado integralmente ao relatório
  revisao-devolutiva-etapa1.md  # Rastreabilidade C1–C5 para o relatório final
  fontes/                     # Fontes Markdown, DOT e Mermaid
  evidencias/                 # Saída dos testes executados
scripts/
  gerar_modelos.py             # DDL -> DOT/Mermaid -> PDFs vetoriais
  gerar_documentos.py          # Markdown + modelos -> PDFs paginados
  requirements.txt
tests/
  validar_etapa1.py            # Testes em MySQL descartável
```

## Regenerar os PDFs

Use Python 3.10+, Graphviz (`dot`), Pandoc e as fontes DejaVu. Em Debian/Ubuntu, os pacotes são `graphviz`, `pandoc` e `fonts-dejavu-core`. Na raiz do repositório:

```bash
python -m pip install -r scripts/requirements.txt
python scripts/gerar_modelos.py
python scripts/gerar_documentos.py
```

As fontes DOT são a representação editável canônica dos diagramas; as fontes Mermaid são alternativas para consulta no GitHub. Ambas são regeneradas do DDL pelo script. Os PDFs vetoriais preservam nitidez na ampliação e devem ser visualizados em página inteira A1, com zoom para leitura em tela.
