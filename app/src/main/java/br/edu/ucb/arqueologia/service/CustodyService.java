package br.edu.ucb.arqueologia.service;
import br.edu.ucb.arqueologia.db.Database;import br.edu.ucb.arqueologia.model.*;
import java.sql.*;import java.time.LocalDate;import java.util.*;
public final class CustodyService {
    public record Transfer(int artifact,int institution,Integer location,int responsible,LocalDate date,String document,String reason,String status){}
    private final Database db;public CustodyService(Database db){this.db=db;}
    public void transfer(Session s,Transfer t,boolean simulateFailure)throws SQLException{
        if(!s.role().writes())throw new BusinessException("Seu perfil permite somente consultas.");
        if(t.date()==null || t.document()==null || t.document().isBlank() || t.reason()==null || t.reason().isBlank())throw new BusinessException("Informe data, documento e motivo da transferência.");
        if(!Set.of("CATALOGADO","EM_ANALISE","EM_CONSERVACAO","ARMAZENADO","EXPOSTO").contains(t.status()))throw new BusinessException("Situação do artefato inválida.");
        try(Connection c=db.open(s)){
            c.setAutoCommit(false);
            try{
                var artifacts=Database.query(c,"SELECT id_artefato,data_descoberta FROM artefato WHERE id_artefato=? FOR UPDATE",t.artifact());
                if(artifacts.isEmpty())throw new BusinessException("Artefato não encontrado.");
                LocalDate discovery=((java.sql.Date)artifacts.get(0).get("data_descoberta")).toLocalDate();
                if(t.date().isBefore(discovery))throw new BusinessException("A custódia não pode começar antes da descoberta.");
                var current=Database.query(c,"SELECT id_custodia,data_inicio FROM custodia_artefato WHERE id_artefato=? AND data_fim IS NULL FOR UPDATE",t.artifact());
                if(!current.isEmpty()){
                    LocalDate start=((java.sql.Date)current.get(0).get("data_inicio")).toLocalDate();
                    if(!t.date().isAfter(start))throw new BusinessException("Use uma data posterior ao início da custódia vigente. O modelo permite um início por dia.");
                    Database.execute(c,"UPDATE custodia_artefato SET data_fim=? WHERE id_custodia=?",t.date(),current.get(0).get("id_custodia"));
                }
                if(simulateFailure)throw new BusinessException("Falha simulada após encerrar a custódia anterior. Todas as alterações foram desfeitas.");
                Database.execute(c,"INSERT INTO custodia_artefato (id_artefato,id_instituicao,id_local,id_responsavel,data_inicio,documento_movimentacao,motivo) VALUES (?,?,?,?,?,?,?)",t.artifact(),t.institution(),t.location(),t.responsible(),t.date(),t.document(),t.reason());
                Database.execute(c,"UPDATE artefato SET situacao=? WHERE id_artefato=?",t.status(),t.artifact());
                c.commit();
            }catch(SQLException|RuntimeException ex){try{c.rollback();}catch(SQLException rollback){ex.addSuppressed(rollback);}throw ex;}
        }
    }
    public Map<String,List<Map<String,Object>>> lookups(Session s)throws SQLException{
        if(!s.role().writes())throw new BusinessException("Seu perfil permite somente consultas.");
        try(Connection c=db.open(s)){
            Map<String,List<Map<String,Object>>> rows=new HashMap<>();
            rows.put("artifact",Database.query(c,"SELECT id_artefato,codigo_catalogo FROM artefato ORDER BY codigo_catalogo"));
            rows.put("institution",Database.query(c,"SELECT id_instituicao,nome FROM instituicao ORDER BY nome"));
            rows.put("location",Database.query(c,"SELECT l.id_local,CONCAT(i.nome,' / ',l.codigo) AS local,l.id_instituicao FROM local_armazenamento l JOIN instituicao i ON i.id_instituicao=l.id_instituicao ORDER BY i.nome,l.codigo"));
            rows.put("responsible",Database.query(c,"SELECT id_pesquisador,nome FROM vw_pesquisadores_referencia WHERE situacao='ATIVO' ORDER BY nome"));return rows;
        }
    }
}
