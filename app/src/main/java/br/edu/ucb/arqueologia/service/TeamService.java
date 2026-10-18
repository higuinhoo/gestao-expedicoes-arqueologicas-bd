package br.edu.ucb.arqueologia.service;
import br.edu.ucb.arqueologia.db.Database;import br.edu.ucb.arqueologia.model.*;
import java.sql.*;import java.time.LocalDate;import java.util.*;
public final class TeamService {
    private final Database db;public TeamService(Database db){this.db=db;}
    private static void permit(Session s){if(!s.role().writes())throw new BusinessException("Seu perfil permite somente consultas.");}
    public Map<String,List<Map<String,Object>>> lookups(Session s)throws SQLException{
        permit(s);try(Connection c=db.open(s)){return Map.of("campaign",Database.query(c,"SELECT id_campanha,codigo FROM campanha ORDER BY codigo"),"researcher",Database.query(c,"SELECT id_pesquisador,nome FROM vw_pesquisadores_referencia WHERE situacao='ATIVO' ORDER BY nome"));}
    }
    public List<Map<String,Object>> list(Session s)throws SQLException{
        permit(s);try(Connection c=db.open(s)){return Database.query(c,"SELECT pc.id_campanha,pc.id_pesquisador,c.codigo AS campanha,p.nome AS pesquisador,pc.funcao,pc.data_entrada,pc.data_saida,pc.horas_previstas FROM participacao_campanha pc JOIN campanha c ON c.id_campanha=pc.id_campanha JOIN vw_pesquisadores_referencia p ON p.id_pesquisador=pc.id_pesquisador ORDER BY c.codigo,p.nome");}
    }
    public void add(Session s,int campaign,int researcher,String function,LocalDate start,LocalDate end,int hours)throws SQLException{
        permit(s);if(function.isBlank() || start==null || hours<=0)throw new BusinessException("Informe função, entrada e carga horária positiva.");
        try(Connection c=db.open(s)){Database.execute(c,"INSERT INTO participacao_campanha (id_campanha,id_pesquisador,funcao,data_entrada,data_saida,horas_previstas) VALUES (?,?,?,?,?,?)",campaign,researcher,function,start,end,hours);}
    }
    public void remove(Session s,int campaign,int researcher)throws SQLException{
        permit(s);try(Connection c=db.open(s)){Database.execute(c,"DELETE FROM participacao_campanha WHERE id_campanha=? AND id_pesquisador=?",campaign,researcher);}
    }
}
