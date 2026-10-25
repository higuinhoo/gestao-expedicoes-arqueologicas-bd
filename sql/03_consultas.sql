SET NAMES utf8mb4;

-- ============================================================
-- 03_consultas.sql
-- Quinze consultas de verificação comentadas por pergunta de negócio
-- ============================================================

USE arqueologia_ucb;

-- CONSULTAS BÁSICAS

-- 1) Quais campanhas começaram no primeiro semestre de 2026 e ainda não foram encerradas?
SELECT codigo, nome, data_inicio, situacao
FROM campanha
WHERE data_inicio BETWEEN '2026-01-01' AND '2026-06-30'
  AND data_fim IS NULL
ORDER BY data_inicio, codigo;

-- 2) Quais artefatos de cerâmica ou metal estão frágeis ou críticos?
SELECT codigo_catalogo, nome, material, estado_conservacao
FROM artefato
WHERE material IN ('Cerâmica', 'Metal')
  AND estado_conservacao IN ('FRAGIL', 'CRITICO')
ORDER BY estado_conservacao, codigo_catalogo;

-- 3) Quais sítios possuem a palavra "Serra" no nome?
SELECT id_sitio, nome, municipio, uf
FROM sitio_arqueologico
WHERE nome LIKE '%Serra%'
ORDER BY uf, municipio, nome;

-- 4) Quais análises ainda não possuem data de conclusão?
SELECT id_analise, tipo_analise, data_inicio, situacao
FROM analise_laboratorial
WHERE data_fim IS NULL
ORDER BY data_inicio;

-- 5) Quais artefatos frágeis ou críticos aguardam decisão de conservação?
-- Consulta básica de triagem: prioriza risco e antiguidade da descoberta.
-- A ausência de período estimado também é exibida como pendência documental.
SELECT codigo_catalogo, nome, material, estado_conservacao,
       data_descoberta, COALESCE(periodo_estimado, 'DATAÇÃO PENDENTE') AS periodo
FROM artefato
WHERE estado_conservacao IN ('FRAGIL', 'CRITICO')
  AND situacao IN ('CATALOGADO', 'ARMAZENADO')
ORDER BY CASE estado_conservacao WHEN 'CRITICO' THEN 0 ELSE 1 END,
         data_descoberta, codigo_catalogo;

-- CONSULTAS COM JUNÇÕES E AGREGAÇÃO

-- 6) Em qual sítio e campanha cada artefato foi encontrado?
SELECT a.codigo_catalogo, a.nome AS artefato,
       c.codigo AS campanha, s.nome AS sitio
FROM artefato AS a
INNER JOIN unidade_escavacao AS u ON u.id_unidade = a.id_unidade
INNER JOIN campanha AS c ON c.id_campanha = u.id_campanha
INNER JOIN sitio_arqueologico AS s ON s.id_sitio = c.id_sitio
ORDER BY s.nome, c.codigo, a.codigo_catalogo;

-- 7) Quais instituições não possuem artefatos sob custódia vigente?
SELECT i.id_instituicao, i.nome, i.tipo
FROM instituicao AS i
LEFT JOIN custodia_artefato AS ca
       ON ca.id_instituicao = i.id_instituicao
      AND ca.data_fim IS NULL
WHERE ca.id_custodia IS NULL
ORDER BY i.nome;

-- 8) Quais sítios produziram pelo menos três artefatos e quantos foram encontrados?
SELECT s.id_sitio, s.nome, COUNT(a.id_artefato) AS quantidade_artefatos
FROM sitio_arqueologico AS s
INNER JOIN campanha AS c ON c.id_sitio = s.id_sitio
INNER JOIN unidade_escavacao AS u ON u.id_campanha = c.id_campanha
INNER JOIN artefato AS a ON a.id_unidade = u.id_unidade
GROUP BY s.id_sitio, s.nome
HAVING COUNT(a.id_artefato) >= 3
ORDER BY quantidade_artefatos DESC, s.nome;

-- 9) Qual é a equipe e a carga horária prevista de cada campanha?
SELECT c.codigo, c.nome,
       COUNT(pc.id_pesquisador) AS integrantes,
       SUM(pc.horas_previstas) AS horas_previstas
FROM campanha AS c
INNER JOIN participacao_campanha AS pc ON pc.id_campanha = c.id_campanha
GROUP BY c.id_campanha, c.codigo, c.nome
ORDER BY horas_previstas DESC, c.codigo;

-- 10) Quantas análises cada artefato recebeu e qual massa total foi consumida?
SELECT a.codigo_catalogo, a.nome,
       COUNT(aa.id_analise) AS quantidade_analises,
       ROUND(COALESCE(SUM(aa.massa_consumida_g), 0), 3) AS massa_total_consumida_g
FROM artefato AS a
LEFT JOIN artefato_analise AS aa ON aa.id_artefato = a.id_artefato
GROUP BY a.id_artefato, a.codigo_catalogo, a.nome
ORDER BY quantidade_analises DESC, massa_total_consumida_g DESC;

-- CONSULTAS AVANÇADAS

-- 11) Quais análises custam mais que a média das análises do mesmo laboratório?
-- Subconsulta escalar CORRELACIONADA: al2.id_laboratorio = al.id_laboratorio.
-- al pertence à consulta externa; cada laboratório fornece sua própria média.
SELECT al.id_analise, al.id_laboratorio, al.tipo_analise, al.custo
FROM analise_laboratorial AS al
WHERE al.custo > (
    SELECT AVG(al2.custo)
    FROM analise_laboratorial AS al2
    WHERE al2.id_laboratorio = al.id_laboratorio
)
ORDER BY al.id_laboratorio, al.custo DESC;

-- 12) Quais artefatos possuem ao menos uma análise concluída com resultado registrado?
SELECT a.id_artefato, a.codigo_catalogo, a.nome
FROM artefato AS a
-- EXISTS correlacionado: aa.id_artefato referencia a linha externa a.id_artefato.
WHERE EXISTS (
    SELECT 1
    FROM artefato_analise AS aa
    INNER JOIN analise_laboratorial AS al ON al.id_analise = aa.id_analise
    WHERE aa.id_artefato = a.id_artefato
      AND al.situacao = 'CONCLUIDA'
      AND aa.resultado_resumido IS NOT NULL
)
ORDER BY a.codigo_catalogo;

-- 13) Quais sítios ainda não produziram nenhum artefato catalogado?
SELECT s.id_sitio, s.nome, s.municipio, s.uf
FROM sitio_arqueologico AS s
WHERE NOT EXISTS (
    SELECT 1
    FROM campanha AS c
    INNER JOIN unidade_escavacao AS u ON u.id_campanha = c.id_campanha
    INNER JOIN artefato AS a ON a.id_unidade = u.id_unidade
    WHERE c.id_sitio = s.id_sitio
)
ORDER BY s.nome;

-- 14) Qual é a custódia mais recente de cada artefato, incluindo registros já encerrados?
WITH custodia_ordenada AS (
    SELECT ca.*,
           ROW_NUMBER() OVER (
               PARTITION BY ca.id_artefato
               ORDER BY ca.data_inicio DESC, ca.id_custodia DESC
           ) AS posicao
    FROM custodia_artefato AS ca
)
SELECT a.codigo_catalogo, a.nome,
       i.nome AS instituicao, l.codigo AS local_armazenamento,
       co.data_inicio, co.data_fim
FROM custodia_ordenada AS co
INNER JOIN artefato AS a ON a.id_artefato = co.id_artefato
INNER JOIN instituicao AS i ON i.id_instituicao = co.id_instituicao
LEFT JOIN local_armazenamento AS l ON l.id_local = co.id_local
WHERE co.posicao = 1
ORDER BY a.codigo_catalogo;

-- 15) Quais registros violam regras semânticas que dependem de mais de uma tabela?
-- Revisão operacional: inativação posterior não invalida participação histórica.
-- RN04 é validada na entrada por SQL/04_operacao.sql; o estado atual sozinho
-- não informa a situação do pesquisador na data em que ele entrou na campanha.
-- A consulta reúne coordenadores não arqueólogos, laboratórios com tipo incorreto,
-- conservadores incompatíveis e locais de custódia pertencentes a outra instituição.
SELECT 'COORDENADOR_NAO_ARQUEOLOGO' AS problema,
       c.codigo AS referencia, p.nome AS detalhe
FROM campanha AS c
INNER JOIN pesquisador AS p ON p.id_pesquisador = c.id_coordenador
WHERE p.tipo_especializacao <> 'ARQUEOLOGO'
UNION ALL
SELECT 'INSTITUICAO_NAO_LABORATORIO',
       CAST(al.id_analise AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci, i.nome
FROM analise_laboratorial AS al
INNER JOIN instituicao AS i ON i.id_instituicao = al.id_laboratorio
WHERE i.tipo <> 'LABORATORIO'
UNION ALL
SELECT 'RESPONSAVEL_NAO_CONSERVADOR',
       CAST(ic.id_intervencao AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci, p.nome
FROM intervencao_conservacao AS ic
INNER JOIN pesquisador AS p ON p.id_pesquisador = ic.id_conservador
WHERE p.tipo_especializacao <> 'CONSERVADOR'
UNION ALL
SELECT 'LOCAL_FORA_DA_INSTITUICAO',
       CAST(ca.id_custodia AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci, l.codigo
FROM custodia_artefato AS ca
INNER JOIN local_armazenamento AS l ON l.id_local = ca.id_local
WHERE l.id_instituicao <> ca.id_instituicao
UNION ALL
SELECT 'ESPECIALIZACAO_INCONSISTENTE',
       CAST(p.id_pesquisador AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci, p.nome
FROM pesquisador AS p
LEFT JOIN arqueologo AS ar ON ar.id_pesquisador = p.id_pesquisador
LEFT JOIN conservador AS co ON co.id_pesquisador = p.id_pesquisador
LEFT JOIN analista_laboratorial AS an ON an.id_pesquisador = p.id_pesquisador
WHERE (ar.id_pesquisador IS NOT NULL) +
      (co.id_pesquisador IS NOT NULL) +
      (an.id_pesquisador IS NOT NULL) <> 1
   OR (p.tipo_especializacao = 'ARQUEOLOGO' AND ar.id_pesquisador IS NULL)
   OR (p.tipo_especializacao = 'CONSERVADOR' AND co.id_pesquisador IS NULL)
   OR (p.tipo_especializacao = 'ANALISTA_LABORATORIAL' AND an.id_pesquisador IS NULL);
