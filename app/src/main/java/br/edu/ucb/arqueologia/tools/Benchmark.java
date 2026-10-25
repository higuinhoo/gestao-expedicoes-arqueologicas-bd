package br.edu.ucb.arqueologia.tools;
import br.edu.ucb.arqueologia.db.*;import java.sql.*;import java.nio.file.*;import java.time.*;import java.util.*;
/** Ensaios em banco isolado, com SELECTs da aplicação. Não remove índices da Etapa 1. */
public final class Benchmark {
 record Probe(String name,String sql,String explain,String analyze,Object[] parameters,String index,String create,String drop){}
 static final List<Probe> PROBES=List.of(
new Probe("acervo","""
        SELECT campanha,sitio,material,estado_conservacao,COUNT(*) AS quantidade,
        ROUND(AVG(profundidade_cm),2) AS profundidade_media_cm
        FROM vw_acervo_consulta
        WHERE (? IS NULL OR id_campanha=?) AND (?='' OR material=?)
        AND data_descoberta BETWEEN ? AND ?
        GROUP BY campanha,sitio,material,estado_conservacao
        ORDER BY campanha,material,estado_conservacao
""","""
EXPLAIN FORMAT=JSON
        SELECT campanha,sitio,material,estado_conservacao,COUNT(*) AS quantidade,
        ROUND(AVG(profundidade_cm),2) AS profundidade_media_cm
        FROM vw_acervo_consulta
        WHERE (? IS NULL OR id_campanha=?) AND (?='' OR material=?)
        AND data_descoberta BETWEEN ? AND ?
        GROUP BY campanha,sitio,material,estado_conservacao
        ORDER BY campanha,material,estado_conservacao
""","""
EXPLAIN ANALYZE
        SELECT campanha,sitio,material,estado_conservacao,COUNT(*) AS quantidade,
        ROUND(AVG(profundidade_cm),2) AS profundidade_media_cm
        FROM vw_acervo_consulta
        WHERE (? IS NULL OR id_campanha=?) AND (?='' OR material=?)
        AND data_descoberta BETWEEN ? AND ?
        GROUP BY campanha,sitio,material,estado_conservacao
        ORDER BY campanha,material,estado_conservacao
""",new Object[]{1,1,"Cerâmica","Cerâmica",LocalDate.of(2025,1,1),LocalDate.of(2027,12,31)},"idx_perf_artefato_consulta","CREATE INDEX idx_perf_artefato_consulta ON artefato(id_unidade,material,data_descoberta,estado_conservacao,profundidade_cm)","DROP INDEX idx_perf_artefato_consulta ON artefato"),
new Probe("analises","""
        SELECT laboratorio,COUNT(*) AS analises,SUM(amostras) AS amostras,
        SUM(massa_g) AS massa_consumida_g,SUM(custo) AS custo_total
        FROM vw_analises_consulta
        WHERE (? IS NULL OR id_laboratorio=?) AND (?='' OR situacao=?)
        AND data_inicio BETWEEN ? AND ?
        GROUP BY id_laboratorio,laboratorio ORDER BY laboratorio
""","""
EXPLAIN FORMAT=JSON
        SELECT laboratorio,COUNT(*) AS analises,SUM(amostras) AS amostras,
        SUM(massa_g) AS massa_consumida_g,SUM(custo) AS custo_total
        FROM vw_analises_consulta
        WHERE (? IS NULL OR id_laboratorio=?) AND (?='' OR situacao=?)
        AND data_inicio BETWEEN ? AND ?
        GROUP BY id_laboratorio,laboratorio ORDER BY laboratorio
""","""
EXPLAIN ANALYZE
        SELECT laboratorio,COUNT(*) AS analises,SUM(amostras) AS amostras,
        SUM(massa_g) AS massa_consumida_g,SUM(custo) AS custo_total
        FROM vw_analises_consulta
        WHERE (? IS NULL OR id_laboratorio=?) AND (?='' OR situacao=?)
        AND data_inicio BETWEEN ? AND ?
        GROUP BY id_laboratorio,laboratorio ORDER BY laboratorio
""",new Object[]{3,3,"CONCLUIDA","CONCLUIDA",LocalDate.of(2026,4,1),LocalDate.of(2026,6,30)},"idx_perf_analise_filtro","CREATE INDEX idx_perf_analise_filtro ON analise_laboratorial(id_laboratorio,situacao,data_inicio,custo)","DROP INDEX idx_perf_analise_filtro ON analise_laboratorial"),
new Probe("custodia","""
        SELECT codigo_catalogo,nome,material,estado_conservacao,situacao,
        instituicao,local_armazenamento,data_inicio
        FROM vw_relatorio_custodia ORDER BY instituicao,codigo_catalogo
""","""
EXPLAIN FORMAT=JSON
        SELECT codigo_catalogo,nome,material,estado_conservacao,situacao,
        instituicao,local_armazenamento,data_inicio
        FROM vw_relatorio_custodia ORDER BY instituicao,codigo_catalogo
""","""
EXPLAIN ANALYZE
        SELECT codigo_catalogo,nome,material,estado_conservacao,situacao,
        instituicao,local_armazenamento,data_inicio
        FROM vw_relatorio_custodia ORDER BY instituicao,codigo_catalogo
""",new Object[]{},"idx_perf_custodia_atual","CREATE INDEX idx_perf_custodia_atual ON custodia_artefato(id_artefato,data_fim,id_instituicao,id_local,data_inicio)","DROP INDEX idx_perf_custodia_atual ON custodia_artefato"));

 public static void main(String[] args)throws Exception{
  Path folder=Path.of(args.length>0?args[0]:"../docs/evidencias/etapa2/desempenho");Files.createDirectories(folder);
  Database db=new Database(Config.load());
  try(Connection c=db.connect(ConcurrentExperiment.required("ARQ_MAINT_USER"),ConcurrentExperiment.required("ARQ_MAINT_PASSWORD").toCharArray())){
   System.out.println("MySQL "+Database.scalar(c,"SELECT VERSION()")+"; execução "+Instant.now());
   for(var p:PROBES){Number n=(Number)Database.scalar(c,"SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND index_name=?",p.index());if(n.intValue()>0)Database.execute(c,p.drop());}
   Database.query(c,"ANALYZE TABLE artefato,analise_laboratorial,artefato_analise,custodia_artefato");
   List<String> csv=new ArrayList<>();csv.add("consulta,linhas,antes_mediana_ms,depois_mediana_ms,antes_min_ms,antes_max_ms,depois_min_ms,depois_max_ms,variacao_percentual");
   for(var p:PROBES){
    Files.writeString(folder.resolve(p.name()+"-sql.txt"),p.sql()+"\nParâmetros: "+Arrays.toString(p.parameters()));
    Files.writeString(folder.resolve(p.name()+"-antes-explain.json"),Database.scalar(c,p.explain(),p.parameters()).toString());
    Files.writeString(folder.resolve(p.name()+"-antes-analyze.txt"),Database.scalar(c,p.analyze(),p.parameters()).toString());
    long rows=run(c,p);double[] before=measure(c,p);
    Database.execute(c,p.create());Database.query(c,"ANALYZE TABLE artefato,analise_laboratorial,artefato_analise,custodia_artefato");
    Files.writeString(folder.resolve(p.name()+"-depois-explain.json"),Database.scalar(c,p.explain(),p.parameters()).toString());
    Files.writeString(folder.resolve(p.name()+"-depois-analyze.txt"),Database.scalar(c,p.analyze(),p.parameters()).toString());
    double[] after=measure(c,p);if(run(c,p)!=rows)throw new IllegalStateException("Resultados divergiram após criar índice.");
    double change=(before[3]-after[3])/before[3]*100;
    String line=String.format(Locale.ROOT,"%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f",p.name(),rows,before[3],after[3],before[0],before[6],after[0],after[6],change);csv.add(line);System.out.println(line);
   }
   Files.write(folder.resolve("medicoes.csv"),csv);
   var counts=Database.query(c,"SELECT 'artefato' AS tabela,COUNT(*) AS registros FROM artefato UNION ALL SELECT 'analise_laboratorial',COUNT(*) FROM analise_laboratorial UNION ALL SELECT 'artefato_analise',COUNT(*) FROM artefato_analise UNION ALL SELECT 'custodia_artefato',COUNT(*) FROM custodia_artefato");
   Files.writeString(folder.resolve("volume.txt"),counts.toString());
   System.out.println("Duas passagens de aquecimento e sete medições por fase; tempos JDBC incluem leitura integral do resultado. Sinal positivo indica redução da mediana.");
  }
 }
 static long run(Connection c,Probe p)throws SQLException{long count=0;try(PreparedStatement statement=Database.prepare(c,p.sql(),p.parameters());ResultSet r=statement.executeQuery()){int columns=r.getMetaData().getColumnCount();while(r.next()){for(int i=1;i<=columns;i++)r.getObject(i);count++;}}return count;}
 static double[] measure(Connection c,Probe p)throws SQLException{run(c,p);run(c,p);double[] result=new double[7];for(int i=0;i<7;i++){long start=System.nanoTime();run(c,p);result[i]=(System.nanoTime()-start)/1e6;}Arrays.sort(result);return result;}
}
