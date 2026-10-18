package br.edu.ucb.arqueologia.service;
public final class Queries {
    private Queries(){}
    public static final String ACERVO="""
        SELECT campanha,sitio,material,estado_conservacao,COUNT(*) AS quantidade,
        ROUND(AVG(profundidade_cm),2) AS profundidade_media_cm
        FROM vw_acervo_consulta
        WHERE (? IS NULL OR id_campanha=?) AND (?='' OR material=?)
        AND data_descoberta BETWEEN ? AND ?
        GROUP BY campanha,sitio,material,estado_conservacao
        ORDER BY campanha,material,estado_conservacao
        """;
    public static final String ANALISES="""
        SELECT laboratorio,COUNT(*) AS analises,SUM(amostras) AS amostras,
        SUM(massa_g) AS massa_consumida_g,SUM(custo) AS custo_total
        FROM vw_analises_consulta
        WHERE (? IS NULL OR id_laboratorio=?) AND (?='' OR situacao=?)
        AND data_inicio BETWEEN ? AND ?
        GROUP BY id_laboratorio,laboratorio ORDER BY laboratorio
        """;
    public static final String RELATORIO="""
        SELECT codigo_catalogo,nome,material,estado_conservacao,situacao,
        instituicao,local_armazenamento,data_inicio
        FROM vw_relatorio_custodia ORDER BY instituicao,codigo_catalogo
        """;
}
