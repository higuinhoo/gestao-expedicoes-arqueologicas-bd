# Modelo lógico relacional

PK = chave primária; FK = chave estrangeira; UQ = unicidade

## instituicao
- id_instituicao: INT [PK]
- nome: VARCHAR(120) [UQ]
- sigla: VARCHAR(20) [atributo]
- tipo: VARCHAR(20) [CK]
- pais: VARCHAR(60) [atributo]
- uf: CHAR(2) [atributo]
- cidade: VARCHAR(80) [atributo]

## pesquisador
- id_pesquisador: INT [PK]
- id_instituicao: INT [FK]
- nome: VARCHAR(120) [atributo]
- email: VARCHAR(160) [UQ]
- registro_profissional: VARCHAR(40) [UQ]
- tipo_especializacao: VARCHAR(24) [CK]
- situacao: VARCHAR(10) [CK]

## arqueologo
- id_pesquisador: INT [PK/FK]
- area_pesquisa: VARCHAR(100) [atributo]
- numero_autorizacao: VARCHAR(40) [UQ]

## conservador
- id_pesquisador: INT [PK/FK]
- especialidade_material: VARCHAR(80) [atributo]
- numero_registro: VARCHAR(40) [UQ]

## analista_laboratorial
- id_pesquisador: INT [PK/FK]
- especialidade_analise: VARCHAR(100) [atributo]
- nivel_formacao: VARCHAR(30) [atributo]

## sitio_arqueologico
- id_sitio: INT [PK]
- nome: VARCHAR(120) [UQ]
- municipio: VARCHAR(80) [atributo]
- uf: CHAR(2) [atributo]
- latitude: DECIMAL(9,6) [CK]
- longitude: DECIMAL(9,6) [CK]
- periodo_historico: VARCHAR(100) [atributo]
- nivel_protecao: VARCHAR(20) [CK]
- data_cadastro: DATE [atributo]

## campanha
- id_campanha: INT [PK]
- id_sitio: INT [FK]
- id_coordenador: INT [FK]
- codigo: VARCHAR(24) [UQ]
- nome: VARCHAR(140) [atributo]
- objetivo: TEXT [atributo]
- data_inicio: DATE [atributo]
- data_fim: DATE [CK]
- situacao: VARCHAR(16) [CK]
- orcamento: DECIMAL(12,2) [CK]

## participacao_campanha
- id_campanha: INT [PK/FK]
- id_pesquisador: INT [PK/FK]
- funcao: VARCHAR(60) [atributo]
- data_entrada: DATE [atributo]
- data_saida: DATE [CK]
- horas_previstas: SMALLINT [CK]

## unidade_escavacao
- id_unidade: INT [PK]
- id_campanha: INT [FK]
- codigo: VARCHAR(20) [UQ composto]
- setor: VARCHAR(40) [atributo]
- nivel_estratigrafico: VARCHAR(40) [atributo]
- data_abertura: DATE [atributo]
- data_encerramento: DATE [CK]

## artefato
- id_artefato: INT [PK]
- id_unidade: INT [FK]
- id_artefato_referencia: INT [FK autorreferente]
- codigo_catalogo: VARCHAR(32) [UQ]
- nome: VARCHAR(120) [atributo]
- descricao: TEXT [atributo]
- material: VARCHAR(60) [atributo]
- periodo_estimado: VARCHAR(100) [atributo]
- data_descoberta: DATE [atributo]
- profundidade_cm: DECIMAL(7,2) [CK]
- estado_conservacao: VARCHAR(16) [CK]
- situacao: VARCHAR(18) [CK]

## fragmento
- id_artefato: INT [PK/FK]
- numero_fragmento: SMALLINT [PK]
- peso_g: DECIMAL(9,3) [CK]
- comprimento_mm: DECIMAL(8,2) [CK]
- descricao: VARCHAR(240) [atributo]
- localizado: BOOLEAN [atributo]

## analise_laboratorial
- id_analise: INT [PK]
- id_laboratorio: INT [FK]
- tipo_analise: VARCHAR(60) [atributo]
- metodo: VARCHAR(120) [atributo]
- data_inicio: DATE [atributo]
- data_fim: DATE [CK]
- situacao: VARCHAR(16) [CK]
- custo: DECIMAL(10,2) [CK]

## artefato_analise
- id_artefato: INT [PK/FK]
- id_analise: INT [PK/FK]
- codigo_amostra: VARCHAR(32) [UQ]
- massa_consumida_g: DECIMAL(8,3) [CK]
- resultado_resumido: VARCHAR(300) [atributo]
- conclusao: TEXT [atributo]

## intervencao_conservacao
- id_intervencao: INT [PK]
- id_artefato: INT [FK]
- id_conservador: INT [FK]
- tipo_intervencao: VARCHAR(80) [atributo]
- data_inicio: DATE [atributo]
- data_fim: DATE [CK]
- descricao: TEXT [atributo]
- material_aplicado: VARCHAR(160) [atributo]
- situacao: VARCHAR(16) [CK]
- custo: DECIMAL(10,2) [CK]

## local_armazenamento
- id_local: INT [PK]
- id_instituicao: INT [FK]
- codigo: VARCHAR(24) [UQ composto]
- tipo: VARCHAR(30) [atributo]
- temperatura_c: DECIMAL(5,2) [atributo]
- umidade_percentual: DECIMAL(5,2) [CK]
- capacidade: INT [CK]

## custodia_artefato
- id_custodia: INT [PK]
- id_artefato: INT [FK]
- id_instituicao: INT [FK]
- id_local: INT [FK]
- id_responsavel: INT [FK]
- data_inicio: DATE [atributo]
- data_fim: DATE [CK]
- documento_movimentacao: VARCHAR(40) [UQ]
- motivo: VARCHAR(200) [atributo]
- id_artefato_em_aberto: INT gerado [UQ]


### Decisões de mapeamento por relação

| Relação | Chave e estratégia | Justificativa |
|---|---|---|
| instituicao | id_instituicao substituta; nome é candidata | O nome pode sofrer ajuste institucional; a chave inteira mantém relacionamentos estáveis e UNIQUE preserva a identificação de negócio. |
| pesquisador | id_pesquisador substituta; email é candidata | Email e registro podem mudar ou ser ausentes; o identificador interno evita propagação de alterações. |
| arqueologo | PK herdada id_pesquisador | Tabela de subtipo evita nulos no supertipo e representa a especialização total e exclusiva; tipo_especializacao e consulta 15 verificam a consistência. O esquema atual não bloqueia toda violação de totalidade/exclusividade, que exige escrita coordenada da aplicação futura. |
| conservador | PK herdada id_pesquisador | A PK também é FK para o supertipo, garantindo que nenhum conservador exista sem pesquisador. |
| analista_laboratorial | PK herdada id_pesquisador | A estratégia supertipo mais subtipo preserva atributos específicos sem repetição dos atributos comuns. |
| sitio_arqueologico | id_sitio substituta; nome é candidata | O nome é legível, mas pode ser revisto; a chave substituta permanece estável durante a pesquisa. |
| campanha | id_campanha substituta; codigo é candidata | O código atende ao negócio e é UNIQUE, enquanto a PK inteira simplifica FKs e possíveis revisões administrativas. |
| participacao_campanha | PK composta campanha e pesquisador | A ocorrência só existe pela combinação dos participantes; função, período e horas dependem da associação completa. |
| unidade_escavacao | id_unidade substituta; campanha e codigo são candidatas | O código é único apenas dentro da campanha; a PK simples reduz o tamanho das referências de artefato. |
| artefato | id_artefato substituta; codigo_catalogo é candidata | O código de catálogo permanece UNIQUE, mas a PK interna sustenta autorreferência e integrações sem acoplar o banco ao formato do código. |
| fragmento | PK composta artefato e numero | É entidade fraca: o número identifica o fragmento somente no contexto de seu artefato proprietário. |
| analise_laboratorial | id_analise substituta | Não há código natural estável para todos os métodos; a chave sequencial identifica cada execução laboratorial. |
| artefato_analise | PK composta artefato e análise; amostra é candidata | A tabela resolve o N:N e armazena atributos que pertencem ao exame daquele artefato naquela análise. |
| intervencao_conservacao | id_intervencao substituta | Uma intervenção não possui identificador natural confiável; data, tipo e responsável podem se repetir. |
| local_armazenamento | id_local substituta; instituição e codigo são candidatas | O código do local é contextual à instituição; a PK simples facilita o histórico de custódia. |
| custodia_artefato | id_custodia substituta; documento e artefato mais início são candidatas | A chave interna identifica o evento; unicidades impedem documento repetido, início duplicado e mais de uma custódia aberta. |


A normalização formal está em `normalizacao.md`.
