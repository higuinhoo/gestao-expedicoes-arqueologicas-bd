package br.edu.ucb.arqueologia.tools;
import br.edu.ucb.arqueologia.db.*;import br.edu.ucb.arqueologia.model.*;import br.edu.ucb.arqueologia.service.*;
import java.sql.*;import java.time.*;import java.util.*;
/** Evidências reais em base de ensaio; cria um artefato sintético e o mantém para inspeção. */
public final class DemoEvidence {
    public static void main(String[] args)throws Exception{
        Database db=new Database(Config.load());AuthService auth=new AuthService(db);
        try(Session admin=auth.login("admin",ConcurrentExperiment.required("ARQ_TEST_ADMIN_PASSWORD").toCharArray());Session catalog=auth.login("catalogador",ConcurrentExperiment.required("ARQ_TEST_CATALOGADOR_PASSWORD").toCharArray());Session reader=auth.login("consulta",ConcurrentExperiment.required("ARQ_TEST_CONSULTA_PASSWORD").toCharArray())){
            System.out.println("Evidências em "+Instant.now());ReportService report=new ReportService(db);
            for(Session s:List.of(admin,catalog,reader)){
                try(Connection c=db.open(s)){
                    System.out.println("Login real: "+s.login()+"; perfil="+s.role()+"; CURRENT_ROLE="+Database.scalar(c,"SELECT CURRENT_ROLE()"));
                    System.out.println("Contas visíveis na view restrita="+Database.scalar(c,"SELECT COUNT(*) FROM vw_minha_conta"));
                    System.out.println("Linhas visíveis no relatório público="+Database.scalar(c,"SELECT COUNT(*) FROM vw_relatorio_custodia"));
                }
            }
            try(Connection c=db.open(reader)){
                try{Database.query(c,"SELECT email FROM pesquisador");throw new IllegalStateException("Privilégio indevido.");}catch(SQLException e){System.out.println("Consulta a e-mail negada ao leitor: código="+e.getErrorCode()+"; "+Errors.friendly(e));}
                try{Database.execute(c,"UPDATE artefato SET nome=? WHERE id_artefato=?","Tentativa negada",1);throw new IllegalStateException("Escrita indevida.");}catch(SQLException e){System.out.println("UPDATE negado ao leitor: código="+e.getErrorCode()+"; "+Errors.friendly(e));}
                long before=prepared(c);String attack="Cerâmica' OR 1=1 #";var rows=Database.query(c,Queries.ACERVO,null,null,attack,attack,LocalDate.of(2020,1,1),LocalDate.of(2030,1,1));long after=prepared(c);
                if(!rows.isEmpty())throw new IllegalStateException("Filtro malicioso retornou dados.");
                System.out.println("SQL injection: entrada="+attack+"; linhas obtidas="+rows.size()+"; consulta permaneceu parametrizada.");
                System.out.println("Com_stmt_prepare antes/depois="+before+"/"+after+" (preparo observado no servidor).");
            }
            String key=UUID.randomUUID().toString().substring(0,8);Map<String,Object> r=new LinkedHashMap<>();r.put("id_unidade",1);r.put("id_artefato_referencia",null);r.put("codigo_catalogo","EVID-"+key);r.put("nome","Artefato sintético para demonstração");r.put("descricao","Evidência controlada de transação.");r.put("material","Cerâmica");r.put("periodo_estimado",null);r.put("data_descoberta",LocalDate.of(2026,9,1));r.put("profundidade_cm",10);r.put("estado_conservacao","ESTAVEL");r.put("situacao","CATALOGADO");
            int id=new CatalogService(db).save(admin,EntitySpec.ARTEFATOS,null,r);int initial;
            try(Connection c=db.open(admin)){initial=Database.insert(c,"INSERT INTO custodia_artefato(id_artefato,id_instituicao,id_local,id_responsavel,data_inicio,documento_movimentacao,motivo) VALUES (?,1,1,1,'2026-10-01',?,'Recebimento sintético')",id,"INI-"+key);}
            var transfer=new CustodyService.Transfer(id,2,2,1,LocalDate.of(2026,10,18),"EVID-TR-"+key,"Transferência de ensaio","ARMAZENADO");
            System.out.println("Operação em READ COMMITTED; SELECT artefato FOR UPDATE; UPDATE custódia; INSERT custódia; UPDATE artefato; COMMIT/ROLLBACK.");
            var before=snapshot(db,admin,id);System.out.println("Estado antes="+before);
            try{new CustodyService(db).transfer(catalog,transfer,true);throw new IllegalStateException("Falha não foi provocada.");}catch(BusinessException e){System.out.println("Falha após UPDATE da custódia anterior: "+e.getMessage());}
            var after=snapshot(db,admin,id);if(!before.equals(after))throw new IllegalStateException("Rollback não preservou estado.");System.out.println("Estado após ROLLBACK="+after+"; igualdade comprovada.");
            new CustodyService(db).transfer(catalog,transfer,false);System.out.println("Estado após COMMIT="+snapshot(db,admin,id));
            try(Connection c=db.open(admin)){
                var audits=Database.query(c,"SELECT usuario_aplicacao,usuario_banco,instante,tabela,valores_anteriores,valores_posteriores FROM auditoria WHERE (tabela='artefato' AND referencia=?) OR (tabela='custodia_artefato' AND referencia=?) ORDER BY id_auditoria",String.valueOf(id),String.valueOf(initial));
                if(audits.size()!=2)throw new IllegalStateException("Esperados dois registros de auditoria confirmados.");System.out.println("Auditoria confirmada="+audits);
            }
        }
    }
    static long prepared(Connection c)throws SQLException{return Long.parseLong(Database.query(c,"SHOW SESSION STATUS LIKE 'Com_stmt_prepare'").get(0).get("Value").toString());}
    static Object snapshot(Database db,Session s,int id)throws SQLException{
        try(Connection c=db.open(s)){return List.of(Database.query(c,"SELECT situacao FROM artefato WHERE id_artefato=?",id),Database.query(c,"SELECT id_custodia,id_instituicao,data_inicio,data_fim FROM custodia_artefato WHERE id_artefato=? ORDER BY id_custodia",id));}
    }
}
