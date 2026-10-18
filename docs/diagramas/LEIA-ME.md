# Modelos renderizados

Estes arquivos estão prontos para visualização, impressão e inclusão no repositório. Não é preciso usar um site ou instalar programa.

- `../mer-conceitual.pdf`: cinco folhas em notação de Chen.
- `../modelo-logico.pdf`: seis folhas com as tabelas e chaves do SQL.
- `conceitual/*.png`: visão completa e quatro recortes com todos os atributos conceituais.
- `logico/*.png`: visão das chaves, esquema completo e quatro recortes do modelo lógico.
- `*.svg` nas mesmas pastas: as mesmas folhas como vetores, para ampliação sem perda de nitidez.

Para um único print geral, use `conceitual/00-visao-geral.png` e `logico/01-esquema-completo.png`. Para apresentar atributos e relacionamentos com mais espaço, use os demais PNGs. Os PDFs são a versão indicada para impressão e entrega; suas folhas mantêm os formatos apropriados para leitura sem redução.

No MER, a cardinalidade junto de cada lado informa a participação daquela entidade no relacionamento. Os dois N:N são relacionamentos com atributos próprios e originam as tabelas associativas no modelo lógico. `d` significa disjunto/exclusivo; a linha dupla até o círculo indica totalidade. Fragmento é fraco: seu número só o identifica no contexto do artefato. Na autorrelação, parte tem zero ou um conjunto, e conjunto tem zero ou muitas partes.

Fontes editáveis textuais: `../fontes/plantuml/`. Renderização: PlantUML 1.2025.2, Graphviz e CairoSVG. Colunas/FKs conferidas com o DDL; atributos conceituais conferidos nas folhas de detalhe. O modelo lógico representa as FKs físicas, incluindo aquelas que referenciam pesquisador e têm restrição de especialização expressa nas regras de negócio.
