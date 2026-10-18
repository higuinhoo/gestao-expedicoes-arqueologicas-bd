# A5 — Dependências funcionais e normalização

### Critérios utilizados

Uma dependência funcional X -> Y significa que duas tuplas com os mesmos valores de X têm os mesmos valores de Y. A notação {a, b} indica um determinante composto. As dependências abaixo explicitam os atributos; dependências triviais e as obtidas por transitividade estão omitidas.

Uma relação está em 3FN quando, para toda dependência não trivial X -> A, X é superchave ou A é atributo primo (pertence a alguma chave candidata). BCNF exige que X seja superchave em toda dependência não trivial: não há a exceção do atributo primo. Portanto BCNF implica 3FN, mas o inverso não é garantido.

1FN: os atributos armazenam valores atômicos e não há grupos repetidos. 2FN: nas relações com chave composta, os atributos não primos dependem da chave inteira. As relações com chave simples já não possuem dependência parcial dessa chave, mas as demais chaves candidatas também precisam ser consideradas. 3FN/BCNF: examinam-se todos os determinantes semânticos conhecidos, e não apenas a chave primária escolhida.

UNIQUE anulável não é automaticamente uma chave candidata: registro_profissional e id_artefato_em_aberto aceitam NULL no MySQL. As análises abaixo consideram as dependências do domínio declarado; não inferem dependências de coincidências da carga sintética.

### instituicao

Chaves candidatas: {id_instituicao}; {nome}.

- {id_instituicao} -> {nome, sigla, tipo, pais, uf, cidade}.
- {nome} -> {id_instituicao, sigla, tipo, pais, uf, cidade}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### pesquisador

Chaves candidatas: {id_pesquisador}; {email}.

- {id_pesquisador} -> {id_instituicao, nome, email, registro_profissional, tipo_especializacao, situacao}.
- {email} -> {id_pesquisador, id_instituicao, nome, registro_profissional, tipo_especializacao, situacao}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### arqueologo

Chaves candidatas: {id_pesquisador}; {numero_autorizacao}.

- {id_pesquisador} -> {area_pesquisa, numero_autorizacao}.
- {numero_autorizacao} -> {id_pesquisador, area_pesquisa}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### conservador

Chaves candidatas: {id_pesquisador}; {numero_registro}.

- {id_pesquisador} -> {especialidade_material, numero_registro}.
- {numero_registro} -> {id_pesquisador, especialidade_material}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### analista_laboratorial

Chaves candidatas: {id_pesquisador}.

- {id_pesquisador} -> {especialidade_analise, nivel_formacao}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### sitio_arqueologico

Chaves candidatas: {id_sitio}; {nome}.

- {id_sitio} -> {nome, municipio, uf, latitude, longitude, periodo_historico, nivel_protecao, data_cadastro}.
- {nome} -> {id_sitio, municipio, uf, latitude, longitude, periodo_historico, nivel_protecao, data_cadastro}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### campanha

Chaves candidatas: {id_campanha}; {codigo}.

- {id_campanha} -> {id_sitio, id_coordenador, codigo, nome, objetivo, data_inicio, data_fim, situacao, orcamento}.
- {codigo} -> {id_campanha, id_sitio, id_coordenador, nome, objetivo, data_inicio, data_fim, situacao, orcamento}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### participacao_campanha

Chaves candidatas: {id_campanha, id_pesquisador}.

- {id_campanha, id_pesquisador} -> {funcao, data_entrada, data_saida, horas_previstas}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### unidade_escavacao

Chaves candidatas: {id_unidade}; {id_campanha, codigo}.

- {id_unidade} -> {id_campanha, codigo, setor, nivel_estratigrafico, data_abertura, data_encerramento}.
- {id_campanha, codigo} -> {id_unidade, setor, nivel_estratigrafico, data_abertura, data_encerramento}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### artefato

Chaves candidatas: {id_artefato}; {codigo_catalogo}.

- {id_artefato} -> {id_unidade, id_artefato_referencia, codigo_catalogo, nome, descricao, material, periodo_estimado, data_descoberta, profundidade_cm, estado_conservacao, situacao}.
- {codigo_catalogo} -> {id_artefato, id_unidade, id_artefato_referencia, nome, descricao, material, periodo_estimado, data_descoberta, profundidade_cm, estado_conservacao, situacao}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### fragmento

Chaves candidatas: {id_artefato, numero_fragmento}.

- {id_artefato, numero_fragmento} -> {peso_g, comprimento_mm, descricao, localizado}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### analise_laboratorial

Chaves candidatas: {id_analise}.

- {id_analise} -> {id_laboratorio, tipo_analise, metodo, data_inicio, data_fim, situacao, custo}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### artefato_analise

Chaves candidatas: {id_artefato, id_analise}; {codigo_amostra}.

- {id_artefato, id_analise} -> {codigo_amostra, massa_consumida_g, resultado_resumido, conclusao}.
- {codigo_amostra} -> {id_artefato, id_analise, massa_consumida_g, resultado_resumido, conclusao}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### intervencao_conservacao

Chaves candidatas: {id_intervencao}.

- {id_intervencao} -> {id_artefato, id_conservador, tipo_intervencao, data_inicio, data_fim, descricao, material_aplicado, situacao, custo}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### local_armazenamento

Chaves candidatas: {id_local}; {id_instituicao, codigo}.

- {id_local} -> {id_instituicao, codigo, tipo, temperatura_c, umidade_percentual, capacidade}.
- {id_instituicao, codigo} -> {id_local, tipo, temperatura_c, umidade_percentual, capacidade}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### custodia_artefato

Chaves candidatas: {id_custodia}; {documento_movimentacao}; {id_artefato, data_inicio}.

- {id_custodia} -> {id_artefato, id_instituicao, id_local, id_responsavel, data_inicio, data_fim, documento_movimentacao, motivo}.
- {documento_movimentacao} -> {id_custodia, id_artefato, id_instituicao, id_local, id_responsavel, data_inicio, data_fim, motivo}.
- {id_artefato, data_inicio} -> {id_custodia, id_instituicao, id_local, id_responsavel, data_fim, documento_movimentacao, motivo}.

Dependência semântica adicional da RN25, para registros cujo local está informado: {id_local} -> {id_instituicao}. O local não é superchave da custódia e id_instituicao não é atributo primo. Nesse subconjunto existe dependência transitiva a partir das chaves do evento. Por isso não se afirma que a representação física de custódia esteja integralmente em 3FN ou BCNF.

Foi mantida uma redundância controlada: a instituição responsável é guardada também quando há um local. Isso permite consultar a custódia vigente por instituição sem juntar a tabela de locais e preserva a instituição quando id_local é removido por ON DELETE SET NULL. O ganho de desempenho é uma hipótese de projeto, ainda sem medição; deve ser confrontado com planos e tempos na Etapa 2. A RN25 é protegida por triggers nas duas tabelas. Uma alternativa estritamente normalizada seria separar a custódia com local da custódia sem local e obter a instituição por junção no primeiro caso; essa alternativa aumenta a complexidade das consultas e da migração.

A coluna física id_artefato_em_aberto satisfaz {id_artefato, data_fim} -> {id_artefato_em_aberto}. Trata-se de materialização determinística para permitir a unicidade de custódia vigente no MySQL. Ela é excluída da relação lógica de eventos usada na análise das chaves, mas a exceção física é documentada; não se usa sua existência para alegar BCNF.

Conclusão: o núcleo de eventos, sem a dependência de local e sem a coluna gerada, tem determinantes candidatos; a representação física atual contém as exceções explícitas acima. Não se repete a afirmação anterior de que todo o esquema físico estaria em 3FN sem ressalvas.

