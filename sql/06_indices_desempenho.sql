-- B4: índices estudados em base limpa (crie uma vez após 04/05).
-- A medição remove somente estes três índices antes da etapa "antes".
USE arqueologia_ucb;
-- Acervo: igualdade da unidade/material, intervalo de data e cobertura da agregação.
CREATE INDEX idx_perf_artefato_consulta ON artefato(id_unidade,material,data_descoberta,estado_conservacao,profundidade_cm);
-- Análises: laboratório/situação antes do intervalo, cobrindo custo e PK implícita.
CREATE INDEX idx_perf_analise_filtro ON analise_laboratorial(id_laboratorio,situacao,data_inicio,custo);
-- Custódia: procura vigente por artefato, evitando varrer seu histórico e cobrir a projeção.
CREATE INDEX idx_perf_custodia_atual ON custodia_artefato(id_artefato,data_fim,id_instituicao,id_local,data_inicio);
