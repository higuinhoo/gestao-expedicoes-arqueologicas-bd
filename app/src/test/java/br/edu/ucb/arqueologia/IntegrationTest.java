package br.edu.ucb.arqueologia;
import br.edu.ucb.arqueologia.db.*;import br.edu.ucb.arqueologia.model.*;import br.edu.ucb.arqueologia.service.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;import java.time.LocalDate;import java.util.*;import java.util.concurrent.*;
@EnabledIfEnvironmentVariable(named="ARQ_TEST_ADMIN_PASSWORD",matches=".+")
class IntegrationTest {
    static Database db;static Session admin,catalog,reader;static CatalogService crud;static ReportService reports;
    static String unique(){return UUID.randomUUID().toString().substring(0,8);}
    @BeforeAll static void setup()throws Exception{
        db=new Database(Config.load());AuthService auth=new AuthService(db);
        admin=auth.login("admin",System.getenv("ARQ_TEST_ADMIN_PASSWORD").toCharArray());
        catalog=auth.login("catalogador",System.getenv("ARQ_TEST_CATALOGADOR_PASSWORD").toCharArray());
        reader=auth.login("consulta",System.getenv("ARQ_TEST_CONSULTA_PASSWORD").toCharArray());crud=new CatalogService(db);reports=new ReportService(db);
    }
    @AfterAll static void close(){for(var s:List.of(admin,catalog,reader))s.close();}
    static Map<String,Object> researcher(String name){return new LinkedHashMap<>(Map.of("id_instituicao",1,"nome",name,"email",unique()+"@example.org","registro_profissional","TEST-"+unique(),"tipo_especializacao","ARQUEOLOGO","situacao","ATIVO","detalhe","Arqueometria","credencial","AUT-"+unique()));}
    static Map<String,Object> campaign(String name){Map<String,Object> r=new LinkedHashMap<>();r.put("id_sitio",1);r.put("id_coordenador",1);r.put("codigo","T-"+unique());r.put("nome",name);r.put("objetivo","Pesquisa sintética de integração");r.put("data_inicio",LocalDate.of(2026,1,1));r.put("data_fim",null);r.put("situacao","PLANEJADA");r.put("orcamento",1000);return r;}
    static Map<String,Object> artifact(String name){Map<String,Object> r=new LinkedHashMap<>();r.put("id_unidade",1);r.put("id_artefato_referencia",null);r.put("codigo_catalogo","T-"+unique());r.put("nome",name);r.put("descricao","Objeto sintético de teste");r.put("material","Cerâmica");r.put("periodo_estimado",null);r.put("data_descoberta",LocalDate.of(2026,9,1));r.put("profundidade_cm",10);r.put("estado_conservacao","ESTAVEL");r.put("situacao","CATALOGADO");return r;}
    static Map<String,Object> location(String name){return new LinkedHashMap<>(Map.of("id_instituicao",1,"codigo",name,"tipo","Reserva técnica","temperatura_c",20,"umidade_percentual",50,"capacidade",100));}
    @Test void fourCompleteCruds()throws Exception{
        var specs=List.of(EntitySpec.PESQUISADORES,EntitySpec.CAMPANHAS,EntitySpec.ARTEFATOS,EntitySpec.LOCAIS);
        var values=List.of(researcher("Pessoa-"+unique()),campaign("Campanha-"+unique()),artifact("Objeto-"+unique()),location("L-"+unique()));
        for(int i=0;i<specs.size();i++){
            var spec=specs.get(i);var row=values.get(i);int id=crud.save(admin,spec,null,row);String field=spec==EntitySpec.LOCAIS?"codigo":"nome";
            assertTrue(crud.list(admin,spec,row.get(field).toString()).stream().anyMatch(r->((Number)r.get(spec.pk)).intValue()==id));
            row.put(field,"U-"+unique());crud.save(admin,spec,id,row);assertEquals(row.get(field),crud.list(admin,spec,row.get(field).toString()).get(0).get(field));
            crud.delete(admin,spec,id);assertTrue(crud.list(admin,spec,row.get(field).toString()).isEmpty());
        }
    }
    @Test void realProfilesAndOwnAccountView()throws Exception{
        assertEquals(Role.ADMIN,admin.role());assertEquals(Role.CATALOGADOR,catalog.role());assertEquals(Role.CONSULTA,reader.role());
        try(Connection c=db.open(reader)){
            assertEquals(1,Database.query(c,"SELECT login FROM vw_minha_conta").size());
            assertTrue(Database.query(c,"SELECT login FROM vw_minha_conta WHERE login=?","admin").isEmpty());
            assertThrows(SQLException.class,()->Database.query(c,"SELECT email FROM pesquisador"));
            assertThrows(SQLException.class,()->Database.query(c,"SELECT senha_hash FROM usuario_app"));
            assertThrows(SQLException.class,()->Database.execute(c,"UPDATE artefato SET nome=? WHERE id_artefato=?","Negado",1));
        }
        try(Connection c=db.open(catalog)){assertThrows(SQLException.class,()->Database.execute(c,"UPDATE pesquisador SET nome=? WHERE id_pesquisador=?","Negado",1));}
        assertFalse(reports.consolidated(reader).isEmpty());assertThrows(BusinessException.class,()->crud.list(reader,EntitySpec.ARTEFATOS,""));
        // Alterar o perfil em memória não amplia os privilégios da conta MySQL.
        try(Session forged=new Session(1,"consulta","Forjado",Role.ADMIN,System.getenv("ARQ_TEST_CONSULTA_PASSWORD").toCharArray())){assertThrows(SQLException.class,()->crud.list(forged,EntitySpec.PESQUISADORES,""));}
    }
    @Test void injectionIsDataAndQueriesAggregateCorrectly()throws Exception{
        String payload="Cerâmica' OR 1=1 #";
        assertTrue(reports.acervo(reader,null,payload,LocalDate.of(2020,1,1),LocalDate.of(2030,1,1)).isEmpty());
        assertFalse(reports.acervo(reader,null,"Cerâmica",LocalDate.of(2020,1,1),LocalDate.of(2030,1,1)).isEmpty());
        var totals=reports.analises(reader,null,"",LocalDate.of(2020,1,1),LocalDate.of(2030,1,1));
        java.math.BigDecimal reported=totals.stream().map(r->new java.math.BigDecimal(r.get("custo_total").toString())).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
        try(Connection c=db.open(admin)){assertEquals(0,reported.compareTo(new java.math.BigDecimal(Database.scalar(c,"SELECT SUM(custo) FROM analise_laboratorial").toString())));}
        assertThrows(SQLException.class,()->new AuthService(db).login("admin' OR '1'='1","errada".toCharArray()));
    }
    @Test void subtypeSwitchAndRollbackPreserveResearcher()throws Exception{
        var r=researcher("Especialista-"+unique());int id=crud.save(admin,EntitySpec.PESQUISADORES,null,r);
        r.put("tipo_especializacao","ANALISTA_LABORATORIAL");r.put("credencial","Mestrado");crud.save(admin,EntitySpec.PESQUISADORES,id,r);
        try(Connection c=db.open(admin)){assertEquals(0,((Number)Database.scalar(c,"SELECT COUNT(*) FROM arqueologo WHERE id_pesquisador=?",id)).intValue());assertEquals(1,((Number)Database.scalar(c,"SELECT COUNT(*) FROM analista_laboratorial WHERE id_pesquisador=?",id)).intValue());}
        r.put("tipo_especializacao","ARQUEOLOGO");r.put("credencial","AUT-A-0001"); // UNIQUE já existente; falha depois de alterar o supertipo.
        assertThrows(SQLException.class,()->crud.save(admin,EntitySpec.PESQUISADORES,id,r));
        try(Connection c=db.open(admin)){assertEquals("ANALISTA_LABORATORIAL",Database.scalar(c,"SELECT tipo_especializacao FROM pesquisador WHERE id_pesquisador=?",id));assertEquals(1,((Number)Database.scalar(c,"SELECT COUNT(*) FROM analista_laboratorial WHERE id_pesquisador=?",id)).intValue());}
        crud.delete(admin,EntitySpec.PESQUISADORES,id);
    }
    @Test void activeEntryAndHistoricalInactivation()throws Exception{
        var r=researcher("Participante-"+unique());int person=crud.save(admin,EntitySpec.PESQUISADORES,null,r);
        int one=crud.save(admin,EntitySpec.CAMPANHAS,null,campaign("Equipe-"+unique())),two=crud.save(admin,EntitySpec.CAMPANHAS,null,campaign("Equipe-"+unique()));
        TeamService team=new TeamService(db);team.add(admin,one,person,"Escavação",LocalDate.of(2026,10,1),null,40);
        r.put("situacao","INATIVO");crud.save(admin,EntitySpec.PESQUISADORES,person,r);
        assertThrows(SQLException.class,()->team.add(admin,two,person,"Escavação",LocalDate.of(2026,10,2),null,40));
        try(Connection c=db.open(admin)){assertEquals(1,((Number)Database.scalar(c,"SELECT COUNT(*) FROM participacao_campanha WHERE id_campanha=? AND id_pesquisador=?",one,person)).intValue());}
        team.remove(admin,one,person);crud.delete(admin,EntitySpec.CAMPANHAS,one);crud.delete(admin,EntitySpec.CAMPANHAS,two);crud.delete(admin,EntitySpec.PESQUISADORES,person);
    }
    @Test void databaseRestrictionsBecomeFriendlyMessages()throws Exception{
        var r=campaign("Inválida-"+unique());r.put("id_coordenador",2);SQLException sql=assertThrows(SQLException.class,()->crud.save(admin,EntitySpec.CAMPANHAS,null,r));assertTrue(Errors.friendly(sql).contains("arqueólogo"));
        SQLException fk=assertThrows(SQLException.class,()->crud.delete(admin,EntitySpec.CAMPANHAS,1));assertTrue(Errors.friendly(fk).contains("não pode ser removido"));
        try(Connection c=db.open(admin)){SQLException e=assertThrows(SQLException.class,()->Database.execute(c,"INSERT INTO intervencao_conservacao (id_artefato,id_conservador,tipo_intervencao,data_inicio,descricao,situacao,custo) VALUES (1,1,'Teste','2026-10-01','Teste','PLANEJADA',0)"));assertTrue(Errors.friendly(e).contains("conservador"));}
    }
    static int fixture()throws Exception{
        int id=crud.save(admin,EntitySpec.ARTEFATOS,null,artifact("Custódia-"+unique()));
        try(Connection c=db.open(admin)){Database.execute(c,"INSERT INTO custodia_artefato (id_artefato,id_instituicao,id_local,id_responsavel,data_inicio,documento_movimentacao,motivo) VALUES (?,1,1,1,'2026-10-01',?,'Teste sintético')",id,"BASE-"+unique());}return id;
    }
    static CustodyService.Transfer transfer(int id,Integer location){return new CustodyService.Transfer(id,2,location,1,LocalDate.of(2026,10,18),"TR-"+unique(),"Transferência sintética de teste","ARMAZENADO");}
    @Test void midwayFailureAndInvalidDestinationRollbackAllChanges()throws Exception{
        int id=fixture();CustodyService service=new CustodyService(db);long before;
        try(Connection c=db.open(admin)){before=((Number)Database.scalar(c,"SELECT COUNT(*) FROM auditoria")).longValue();}
        assertThrows(BusinessException.class,()->service.transfer(admin,transfer(id,2),true));
        assertThrows(SQLException.class,()->service.transfer(admin,transfer(id,1),false)); // RN25, após encerrar a anterior.
        try(Connection c=db.open(admin)){
            assertEquals("CATALOGADO",Database.scalar(c,"SELECT situacao FROM artefato WHERE id_artefato=?",id));
            assertEquals(1,((Number)Database.scalar(c,"SELECT COUNT(*) FROM custodia_artefato WHERE id_artefato=?",id)).intValue());
            assertEquals(1,((Number)Database.scalar(c,"SELECT COUNT(*) FROM custodia_artefato WHERE id_artefato=? AND data_fim IS NULL",id)).intValue());
            assertEquals(before,((Number)Database.scalar(c,"SELECT COUNT(*) FROM auditoria")).longValue());
        }
    }
    @Test void commitUpdatesTwoTablesAndRecordsAuthorOldAndNew()throws Exception{
        int id=fixture();new CustodyService(db).transfer(catalog,transfer(id,2),false);
        try(Connection c=db.open(admin)){
            assertEquals("ARMAZENADO",Database.scalar(c,"SELECT situacao FROM artefato WHERE id_artefato=?",id));
            assertEquals(2,((Number)Database.scalar(c,"SELECT COUNT(*) FROM custodia_artefato WHERE id_artefato=?",id)).intValue());
            assertEquals(1,((Number)Database.scalar(c,"SELECT COUNT(*) FROM custodia_artefato WHERE id_artefato=? AND data_fim IS NULL",id)).intValue());
            var rows=Database.query(c,"SELECT usuario_aplicacao,usuario_banco,instante,valores_anteriores,valores_posteriores FROM auditoria WHERE tabela='artefato' AND referencia=?",String.valueOf(id));assertEquals(1,rows.size());
            assertEquals(catalog.actor(),rows.get(0).get("usuario_aplicacao"));assertTrue(rows.get(0).get("usuario_banco").toString().startsWith("catalogador@"));assertNotNull(rows.get(0).get("instante"));assertTrue(rows.get(0).get("valores_anteriores").toString().contains("CATALOGADO"));assertTrue(rows.get(0).get("valores_posteriores").toString().contains("ARMAZENADO"));
        }
    }
    @Test void sameDayTransferIsRejected()throws Exception{
        int id=fixture();var t=new CustodyService.Transfer(id,2,2,1,LocalDate.of(2026,10,1),"DIA-"+unique(),"Teste","ARMAZENADO");
        assertThrows(BusinessException.class,()->new CustodyService(db).transfer(admin,t,false));
    }
    @Test void transferWaitsForTheArtifactLock()throws Exception{
        int id=fixture();ExecutorService worker=Executors.newSingleThreadExecutor();
        try(Connection first=db.open(admin)){
            first.setAutoCommit(false);Database.query(first,"SELECT id_artefato FROM artefato WHERE id_artefato=? FOR UPDATE",id);
            Future<?> pending=worker.submit(()->{try{new CustodyService(db).transfer(admin,transfer(id,2),false);}catch(SQLException e){throw new RuntimeException(e);}});
            assertThrows(TimeoutException.class,()->pending.get(800,TimeUnit.MILLISECONDS));first.commit();pending.get(8,TimeUnit.SECONDS);
        }finally{worker.shutdownNow();}
    }
}
