package br.edu.ucb.arqueologia.service;
import br.edu.ucb.arqueologia.db.Database;import br.edu.ucb.arqueologia.model.*;
import java.sql.*;import java.util.*;
public final class CatalogService {
    private final Database db;
    public CatalogService(Database db){this.db=db;}
    private static void permit(Session s,EntitySpec spec){if(!spec.permits(s.role()))throw new BusinessException("Seu perfil não pode administrar "+spec.title.toLowerCase()+".");}
    public List<Map<String,Object>> list(Session s,EntitySpec spec,String search)throws SQLException{
        permit(s,spec);try(Connection c=db.open(s)){return Database.query(c,spec.listSql,"%"+search+"%","%"+search+"%");}
    }
    public Map<String,List<Map<String,Object>>> lookups(Session s,EntitySpec spec)throws SQLException{
        permit(s,spec);Map<String,List<Map<String,Object>>> result=new HashMap<>();
        try(Connection c=db.open(s)){for(var f:spec.fields)if(f.lookup()!=null)result.put(f.key(),Database.query(c,f.lookup()));}return result;
    }
    public int save(Session s,EntitySpec spec,Integer id,Map<String,Object> values)throws SQLException{
        permit(s,spec);
        for(var f:spec.fields)if(f.required() && (values.get(f.key())==null || values.get(f.key()).toString().isBlank()))throw new BusinessException("Preencha: "+f.label());
        try(Connection c=db.open(s)){
            c.setAutoCommit(false);
            try{
                Object[] args=spec.fields.stream().limit(spec==EntitySpec.PESQUISADORES?6:spec.fields.size()).map(f->values.get(f.key())).toArray();
                if(id!=null && spec==EntitySpec.PESQUISADORES){
                    if(Database.scalar(c,"SELECT id_pesquisador FROM pesquisador WHERE id_pesquisador=? FOR UPDATE",id)==null)throw new BusinessException("Pesquisador não encontrado.");
                    Database.execute(c,"DELETE FROM arqueologo WHERE id_pesquisador=?",id);
                    Database.execute(c,"DELETE FROM conservador WHERE id_pesquisador=?",id);
                    Database.execute(c,"DELETE FROM analista_laboratorial WHERE id_pesquisador=?",id);
                }
                if(id==null)id=Database.insert(c,spec.insertSql,args);
                else {
                    Object[] update=Arrays.copyOf(args,args.length+1);update[args.length]=id;
                    if(Database.execute(c,spec.updateSql,update)!=1)throw new BusinessException("Registro não encontrado. Atualize a lista.");
                }
                if(spec==EntitySpec.PESQUISADORES){
                    String type=(String)values.get("tipo_especializacao");Object detail=values.get("detalhe"),credential=values.get("credencial");
                    switch(type){
                        case "ARQUEOLOGO"->Database.execute(c,"INSERT INTO arqueologo (id_pesquisador,area_pesquisa,numero_autorizacao) VALUES (?,?,?)",id,detail,credential);
                        case "CONSERVADOR"->Database.execute(c,"INSERT INTO conservador (id_pesquisador,especialidade_material,numero_registro) VALUES (?,?,?)",id,detail,credential);
                        case "ANALISTA_LABORATORIAL"->Database.execute(c,"INSERT INTO analista_laboratorial (id_pesquisador,especialidade_analise,nivel_formacao) VALUES (?,?,?)",id,detail,credential);
                        default->throw new BusinessException("Escolha uma especialização válida.");
                    }
                    Number count=(Number)Database.scalar(c,"SELECT (SELECT COUNT(*) FROM arqueologo WHERE id_pesquisador=?)+(SELECT COUNT(*) FROM conservador WHERE id_pesquisador=?)+(SELECT COUNT(*) FROM analista_laboratorial WHERE id_pesquisador=?)",id,id,id);
                    if(count.intValue()!=1)throw new BusinessException("RN03: o pesquisador precisa ter exatamente um subtipo.");
                }
                c.commit();return id;
            }catch(SQLException|RuntimeException ex){try{c.rollback();}catch(SQLException rollback){ex.addSuppressed(rollback);}throw ex;}
        }
    }
    public void delete(Session s,EntitySpec spec,int id)throws SQLException{
        permit(s,spec);try(Connection c=db.open(s)){if(Database.execute(c,spec.deleteSql,id)!=1)throw new BusinessException("Registro não encontrado.");}
    }
}
