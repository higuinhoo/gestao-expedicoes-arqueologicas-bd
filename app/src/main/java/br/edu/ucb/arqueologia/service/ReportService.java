package br.edu.ucb.arqueologia.service;
import br.edu.ucb.arqueologia.db.Database;import br.edu.ucb.arqueologia.model.Session;
import java.sql.*;import java.time.LocalDate;import java.util.*;
public final class ReportService {
    private final Database db;public ReportService(Database db){this.db=db;}
    private static void period(LocalDate start,LocalDate end){if(start==null || end==null || end.isBefore(start))throw new BusinessException("Informe um período válido, com fim igual ou posterior ao início.");}
    public List<Map<String,Object>> acervo(Session s,Integer campaign,String material,LocalDate start,LocalDate end)throws SQLException{
        period(start,end);try(Connection c=db.open(s)){return Database.query(c,Queries.ACERVO,campaign,campaign,material,material,start,end);}
    }
    public List<Map<String,Object>> analises(Session s,Integer lab,String status,LocalDate start,LocalDate end)throws SQLException{
        period(start,end);try(Connection c=db.open(s)){return Database.query(c,Queries.ANALISES,lab,lab,status,status,start,end);}
    }
    public List<Map<String,Object>> filters(Session s,boolean analyses)throws SQLException{
        try(Connection c=db.open(s)){return analyses?Database.query(c,"SELECT DISTINCT id_laboratorio,laboratorio FROM vw_analises_consulta ORDER BY laboratorio"):Database.query(c,"SELECT DISTINCT id_campanha,campanha FROM vw_acervo_consulta ORDER BY campanha");}
    }
    public List<Map<String,Object>> consolidated(Session s)throws SQLException{try(Connection c=db.open(s)){return Database.query(c,Queries.RELATORIO);}}
    public List<Map<String,Object>> audit(Session s)throws SQLException{
        if(s.role()!=br.edu.ucb.arqueologia.model.Role.ADMIN)throw new BusinessException("Auditoria disponível para administrador.");
        try(Connection c=db.open(s)){return Database.query(c,"SELECT id_auditoria,usuario_aplicacao,usuario_banco,instante,tabela,referencia,valores_anteriores,valores_posteriores FROM auditoria ORDER BY id_auditoria DESC LIMIT 500");}
    }
}
