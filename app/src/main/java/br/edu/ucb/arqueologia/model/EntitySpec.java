package br.edu.ucb.arqueologia.model;
import java.util.*;import java.math.BigDecimal;import java.time.LocalDate;
/** SQL constante. Os valores são vinculados por JDBC; não há SQL montado com entrada do usuário. */
public enum EntitySpec {
    PESQUISADORES("Pesquisadores","id_pesquisador","SELECT p.id_pesquisador,p.id_instituicao,p.nome,p.email,p.registro_profissional,p.tipo_especializacao,p.situacao,COALESCE(a.area_pesquisa,c.especialidade_material,l.especialidade_analise) AS detalhe,COALESCE(a.numero_autorizacao,c.numero_registro,l.nivel_formacao) AS credencial,i.nome AS instituicao FROM pesquisador p JOIN instituicao i ON i.id_instituicao=p.id_instituicao LEFT JOIN arqueologo a ON a.id_pesquisador=p.id_pesquisador LEFT JOIN conservador c ON c.id_pesquisador=p.id_pesquisador LEFT JOIN analista_laboratorial l ON l.id_pesquisador=p.id_pesquisador WHERE (p.nome LIKE ? OR p.email LIKE ?) ORDER BY p.id_pesquisador DESC LIMIT 500","INSERT INTO pesquisador (id_instituicao,nome,email,registro_profissional,tipo_especializacao,situacao) VALUES (?,?,?,?,?,?)","UPDATE pesquisador SET id_instituicao=?,nome=?,email=?,registro_profissional=?,tipo_especializacao=?,situacao=? WHERE id_pesquisador=?","DELETE FROM pesquisador WHERE id_pesquisador=?",List.of(
            new Field("id_instituicao","Instituição",Kind.INTEGER,true,null,"SELECT id_instituicao,nome FROM instituicao ORDER BY nome"),
            new Field("nome","Nome",Kind.TEXT,true,null,null),
            new Field("email","E-mail",Kind.TEXT,true,null,null),
            new Field("registro_profissional","Registro profissional",Kind.TEXT,false,null,null),
            new Field("tipo_especializacao","Especialização",Kind.TEXT,true,"ARQUEOLOGO,CONSERVADOR,ANALISTA_LABORATORIAL",null),
            new Field("situacao","Situação",Kind.TEXT,true,"ATIVO,INATIVO",null),
            new Field("detalhe","Área / material / especialidade",Kind.TEXT,true,null,null),
            new Field("credencial","Autorização / registro / formação",Kind.TEXT,true,null,null))),
    CAMPANHAS("Campanhas","id_campanha","SELECT c.id_campanha,c.id_sitio,c.id_coordenador,c.codigo,c.nome,c.objetivo,c.data_inicio,c.data_fim,c.situacao,c.orcamento,s.nome AS sitio,p.nome AS coordenador FROM campanha c JOIN sitio_arqueologico s ON s.id_sitio=c.id_sitio JOIN vw_pesquisadores_referencia p ON p.id_pesquisador=c.id_coordenador WHERE (c.nome LIKE ? OR c.codigo LIKE ?) ORDER BY c.id_campanha DESC LIMIT 500","INSERT INTO campanha (id_sitio,id_coordenador,codigo,nome,objetivo,data_inicio,data_fim,situacao,orcamento) VALUES (?,?,?,?,?,?,?,?,?)","UPDATE campanha SET id_sitio=?,id_coordenador=?,codigo=?,nome=?,objetivo=?,data_inicio=?,data_fim=?,situacao=?,orcamento=? WHERE id_campanha=?","DELETE FROM campanha WHERE id_campanha=?",List.of(
            new Field("id_sitio","Sítio",Kind.INTEGER,true,null,"SELECT id_sitio,nome FROM sitio_arqueologico ORDER BY nome"),
            new Field("id_coordenador","Coordenador",Kind.INTEGER,true,null,"SELECT id_pesquisador,nome FROM vw_pesquisadores_referencia WHERE tipo_especializacao='ARQUEOLOGO' ORDER BY nome"),
            new Field("codigo","Código",Kind.TEXT,true,null,null),
            new Field("nome","Nome",Kind.TEXT,true,null,null),
            new Field("objetivo","Objetivo",Kind.TEXT,true,null,null),
            new Field("data_inicio","Início (AAAA-MM-DD)",Kind.DATE,true,null,null),
            new Field("data_fim","Fim (AAAA-MM-DD)",Kind.DATE,false,null,null),
            new Field("situacao","Situação",Kind.TEXT,true,"PLANEJADA,EM_ANDAMENTO,CONCLUIDA,SUSPENSA",null),
            new Field("orcamento","Orçamento",Kind.DECIMAL,true,null,null))),
    ARTEFATOS("Artefatos","id_artefato","SELECT a.id_artefato,a.id_unidade,a.id_artefato_referencia,a.codigo_catalogo,a.nome,a.descricao,a.material,a.periodo_estimado,a.data_descoberta,a.profundidade_cm,a.estado_conservacao,a.situacao,CONCAT(c.codigo,' / ',u.codigo) AS unidade,r.codigo_catalogo AS conjunto FROM artefato a JOIN unidade_escavacao u ON u.id_unidade=a.id_unidade JOIN campanha c ON c.id_campanha=u.id_campanha LEFT JOIN artefato r ON r.id_artefato=a.id_artefato_referencia WHERE (a.nome LIKE ? OR a.codigo_catalogo LIKE ?) ORDER BY a.id_artefato DESC LIMIT 500","INSERT INTO artefato (id_unidade,id_artefato_referencia,codigo_catalogo,nome,descricao,material,periodo_estimado,data_descoberta,profundidade_cm,estado_conservacao,situacao) VALUES (?,?,?,?,?,?,?,?,?,?,?)","UPDATE artefato SET id_unidade=?,id_artefato_referencia=?,codigo_catalogo=?,nome=?,descricao=?,material=?,periodo_estimado=?,data_descoberta=?,profundidade_cm=?,estado_conservacao=?,situacao=? WHERE id_artefato=?","DELETE FROM artefato WHERE id_artefato=?",List.of(
            new Field("id_unidade","Unidade de escavação",Kind.INTEGER,true,null,"SELECT u.id_unidade,CONCAT(c.codigo,' / ',u.codigo) FROM unidade_escavacao u JOIN campanha c ON c.id_campanha=u.id_campanha ORDER BY c.codigo,u.codigo"),
            new Field("id_artefato_referencia","Conjunto de referência",Kind.INTEGER,false,null,"SELECT id_artefato,codigo_catalogo FROM artefato ORDER BY codigo_catalogo"),
            new Field("codigo_catalogo","Código de catálogo",Kind.TEXT,true,null,null),
            new Field("nome","Nome",Kind.TEXT,true,null,null),
            new Field("descricao","Descrição",Kind.TEXT,true,null,null),
            new Field("material","Material",Kind.TEXT,true,"Cerâmica,Metal,Vidro,Pedra,Osso,Madeira,Outro",null),
            new Field("periodo_estimado","Período estimado",Kind.TEXT,false,null,null),
            new Field("data_descoberta","Descoberta (AAAA-MM-DD)",Kind.DATE,true,null,null),
            new Field("profundidade_cm","Profundidade (cm)",Kind.DECIMAL,true,null,null),
            new Field("estado_conservacao","Estado de conservação",Kind.TEXT,true,"INTEGRO,ESTAVEL,FRAGIL,CRITICO",null),
            new Field("situacao","Situação",Kind.TEXT,true,"CATALOGADO,EM_ANALISE,EM_CONSERVACAO,ARMAZENADO,EXPOSTO",null))),
    LOCAIS("Locais de armazenamento","id_local","SELECT l.id_local,l.id_instituicao,l.codigo,l.tipo,l.temperatura_c,l.umidade_percentual,l.capacidade,i.nome AS instituicao FROM local_armazenamento l JOIN instituicao i ON i.id_instituicao=l.id_instituicao WHERE (l.codigo LIKE ? OR i.nome LIKE ?) ORDER BY l.id_local DESC LIMIT 500","INSERT INTO local_armazenamento (id_instituicao,codigo,tipo,temperatura_c,umidade_percentual,capacidade) VALUES (?,?,?,?,?,?)","UPDATE local_armazenamento SET id_instituicao=?,codigo=?,tipo=?,temperatura_c=?,umidade_percentual=?,capacidade=? WHERE id_local=?","DELETE FROM local_armazenamento WHERE id_local=?",List.of(
            new Field("id_instituicao","Instituição",Kind.INTEGER,true,null,"SELECT id_instituicao,nome FROM instituicao ORDER BY nome"),
            new Field("codigo","Código",Kind.TEXT,true,null,null),
            new Field("tipo","Tipo",Kind.TEXT,true,"Reserva técnica,Laboratório,Exposição,Triagem",null),
            new Field("temperatura_c","Temperatura (°C)",Kind.DECIMAL,false,null,null),
            new Field("umidade_percentual","Umidade (%)",Kind.DECIMAL,false,null,null),
            new Field("capacidade","Capacidade",Kind.INTEGER,true,null,null)));
    public enum Kind { TEXT, INTEGER, DECIMAL, DATE }
    public record Field(String key,String label,Kind kind,boolean required,String options,String lookup) {
        public Object parse(String text){
            if(text==null || text.isBlank()) {
                if(required)throw new IllegalArgumentException("Preencha: "+label);return null;
            }
            try{return switch(kind){case TEXT->text.trim();case INTEGER->Integer.valueOf(text.trim());case DECIMAL->new BigDecimal(text.trim().replace(',','.'));case DATE->LocalDate.parse(text.trim());};}
            catch(RuntimeException ex){throw new IllegalArgumentException("Confira o formato de "+label+".");}
        }
    }
    public final String title,pk,listSql,insertSql,updateSql,deleteSql;public final List<Field> fields;
    EntitySpec(String title,String pk,String list,String insert,String update,String delete,List<Field> fields){this.title=title;this.pk=pk;this.listSql=list;this.insertSql=insert;this.updateSql=update;this.deleteSql=delete;this.fields=fields;}
    public boolean permits(Role role){return role.writes() && (this!=PESQUISADORES || role==Role.ADMIN);}
}
