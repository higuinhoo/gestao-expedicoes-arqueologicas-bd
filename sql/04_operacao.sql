-- Etapa 2: complementa o DDL autoral; execute depois de 01, 02 e 03.
USE arqueologia_ucb;
CREATE TABLE IF NOT EXISTS usuario_app (
 id_usuario INT AUTO_INCREMENT,
 login VARCHAR(80) NOT NULL,
 nome VARCHAR(120) NOT NULL,
 perfil VARCHAR(16) NOT NULL,
 senha_hash VARCHAR(255) NOT NULL,
 ativo BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT pk_usuario_app PRIMARY KEY(id_usuario),
 CONSTRAINT uq_usuario_app_login UNIQUE(login),
 CONSTRAINT ck_usuario_app_perfil CHECK(perfil IN ('ADMIN','CATALOGADOR','CONSULTA'))
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS auditoria (
 id_auditoria BIGINT AUTO_INCREMENT,
 usuario_aplicacao VARCHAR(120) NOT NULL,
 usuario_banco VARCHAR(288) NOT NULL,
 instante TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 tabela VARCHAR(40) NOT NULL,
 operacao VARCHAR(12) NOT NULL,
 referencia VARCHAR(80) NOT NULL,
 valores_anteriores JSON,
 valores_posteriores JSON,
 CONSTRAINT pk_auditoria PRIMARY KEY(id_auditoria),
 INDEX idx_auditoria_instante(instante),
 INDEX idx_auditoria_referencia(tabela,referencia,instante)
) ENGINE=InnoDB;

-- USER() representa o cliente, inclusive dentro de uma view SQL SECURITY DEFINER.
-- Cada login enxerga somente a própria conta; nenhuma role lê usuario_app diretamente.
CREATE OR REPLACE SQL SECURITY DEFINER VIEW vw_minha_conta AS
 SELECT id_usuario,login,nome,perfil,senha_hash,ativo FROM usuario_app
 WHERE login=SUBSTRING_INDEX(USER(),'@',1);

-- Views com SQL SECURITY DEFINER: o perfil CONSULTA recebe somente estas projeções.
CREATE OR REPLACE SQL SECURITY DEFINER VIEW vw_pesquisadores_referencia AS
 SELECT id_pesquisador,nome,tipo_especializacao,situacao FROM pesquisador;
CREATE OR REPLACE SQL SECURITY DEFINER VIEW vw_acervo_consulta AS
 SELECT a.id_artefato,a.codigo_catalogo,a.nome,a.material,a.estado_conservacao,
 a.situacao,a.data_descoberta,a.profundidade_cm,c.id_campanha,c.codigo AS campanha,
 s.id_sitio,s.nome AS sitio
 FROM artefato a JOIN unidade_escavacao u ON u.id_unidade=a.id_unidade
 JOIN campanha c ON c.id_campanha=u.id_campanha
 JOIN sitio_arqueologico s ON s.id_sitio=c.id_sitio;
CREATE OR REPLACE SQL SECURITY DEFINER VIEW vw_analises_consulta AS
 SELECT al.id_analise,al.id_laboratorio,i.nome AS laboratorio,al.situacao,
 al.data_inicio,al.custo,COUNT(aa.id_artefato) AS amostras,
 COALESCE(SUM(aa.massa_consumida_g),0) AS massa_g
 FROM analise_laboratorial al JOIN instituicao i ON i.id_instituicao=al.id_laboratorio
 LEFT JOIN artefato_analise aa ON aa.id_analise=al.id_analise
 GROUP BY al.id_analise,al.id_laboratorio,i.nome,al.situacao,al.data_inicio,al.custo;
CREATE OR REPLACE SQL SECURITY DEFINER VIEW vw_relatorio_custodia AS
 SELECT a.id_artefato,a.codigo_catalogo,a.nome,a.material,a.estado_conservacao,a.situacao,
 ca.id_instituicao,COALESCE(i.nome,'Custódia pendente') AS instituicao,
 COALESCE(l.codigo,'Local não definido') AS local_armazenamento,ca.data_inicio
 FROM artefato a LEFT JOIN custodia_artefato ca ON ca.id_artefato=a.id_artefato AND ca.data_fim IS NULL
 LEFT JOIN instituicao i ON i.id_instituicao=ca.id_instituicao
 LEFT JOIN local_armazenamento l ON l.id_local=ca.id_local;

DROP TRIGGER IF EXISTS trg_auditoria_artefato;
DROP TRIGGER IF EXISTS trg_auditoria_custodia;
DROP TRIGGER IF EXISTS trg_participacao_ativo_insert;
DROP TRIGGER IF EXISTS trg_participacao_ativo_update;
DROP TRIGGER IF EXISTS trg_intervencao_conservador_insert;
DROP TRIGGER IF EXISTS trg_intervencao_conservador_update;
DROP TRIGGER IF EXISTS trg_preservar_conservador;
DELIMITER $$
-- B6: catálogo e estado de um objeto são relevantes para preservação/rastreabilidade.
CREATE TRIGGER trg_auditoria_artefato AFTER UPDATE ON artefato FOR EACH ROW
BEGIN
 INSERT INTO auditoria(usuario_aplicacao,usuario_banco,tabela,operacao,referencia,valores_anteriores,valores_posteriores)
 VALUES(COALESCE(NULLIF(@app_actor,''),USER()),USER(),'artefato','UPDATE',CAST(NEW.id_artefato AS CHAR),
 JSON_OBJECT('id_unidade',OLD.id_unidade,'referencia',OLD.id_artefato_referencia,'catalogo',OLD.codigo_catalogo,'nome',OLD.nome,'descricao',OLD.descricao,'material',OLD.material,'periodo',OLD.periodo_estimado,'descoberta',OLD.data_descoberta,'profundidade',OLD.profundidade_cm,'estado',OLD.estado_conservacao,'situacao',OLD.situacao),
 JSON_OBJECT('id_unidade',NEW.id_unidade,'referencia',NEW.id_artefato_referencia,'catalogo',NEW.codigo_catalogo,'nome',NEW.nome,'descricao',NEW.descricao,'material',NEW.material,'periodo',NEW.periodo_estimado,'descoberta',NEW.data_descoberta,'profundidade',NEW.profundidade_cm,'estado',NEW.estado_conservacao,'situacao',NEW.situacao));
END$$
-- B6: o encerramento/alteração da custódia identifica transferência de responsabilidade.
CREATE TRIGGER trg_auditoria_custodia AFTER UPDATE ON custodia_artefato FOR EACH ROW
BEGIN
 INSERT INTO auditoria(usuario_aplicacao,usuario_banco,tabela,operacao,referencia,valores_anteriores,valores_posteriores)
 VALUES(COALESCE(NULLIF(@app_actor,''),USER()),USER(),'custodia_artefato','UPDATE',CAST(NEW.id_custodia AS CHAR),
 JSON_OBJECT('artefato',OLD.id_artefato,'instituicao',OLD.id_instituicao,'local',OLD.id_local,'responsavel',OLD.id_responsavel,'inicio',OLD.data_inicio,'fim',OLD.data_fim,'documento',OLD.documento_movimentacao,'motivo',OLD.motivo),
 JSON_OBJECT('artefato',NEW.id_artefato,'instituicao',NEW.id_instituicao,'local',NEW.id_local,'responsavel',NEW.id_responsavel,'inicio',NEW.data_inicio,'fim',NEW.data_fim,'documento',NEW.documento_movimentacao,'motivo',NEW.motivo));
END$$
CREATE TRIGGER trg_participacao_ativo_insert BEFORE INSERT ON participacao_campanha FOR EACH ROW
BEGIN
 DECLARE v_situacao VARCHAR(10);
 SELECT situacao INTO v_situacao FROM pesquisador WHERE id_pesquisador=NEW.id_pesquisador FOR SHARE;
 IF v_situacao IS NULL OR v_situacao<>'ATIVO' THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN04: apenas pesquisadores ativos entram em campanhas'; END IF;
END$$
CREATE TRIGGER trg_participacao_ativo_update BEFORE UPDATE ON participacao_campanha FOR EACH ROW
BEGIN
 DECLARE v_situacao VARCHAR(10);
 IF OLD.id_campanha<>NEW.id_campanha OR OLD.id_pesquisador<>NEW.id_pesquisador THEN
 SELECT situacao INTO v_situacao FROM pesquisador WHERE id_pesquisador=NEW.id_pesquisador FOR SHARE;
 IF v_situacao IS NULL OR v_situacao<>'ATIVO' THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN04: apenas pesquisadores ativos entram em campanhas'; END IF;
 END IF;
END$$
CREATE TRIGGER trg_intervencao_conservador_insert BEFORE INSERT ON intervencao_conservacao FOR EACH ROW
BEGIN
 DECLARE v_tipo VARCHAR(24);
 SELECT tipo_especializacao INTO v_tipo FROM pesquisador WHERE id_pesquisador=NEW.id_conservador FOR SHARE;
 IF v_tipo IS NULL OR v_tipo<>'CONSERVADOR' THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN20: intervencao exige conservador'; END IF;
END$$
CREATE TRIGGER trg_intervencao_conservador_update BEFORE UPDATE ON intervencao_conservacao FOR EACH ROW
BEGIN
 DECLARE v_tipo VARCHAR(24);
 SELECT tipo_especializacao INTO v_tipo FROM pesquisador WHERE id_pesquisador=NEW.id_conservador FOR SHARE;
 IF v_tipo IS NULL OR v_tipo<>'CONSERVADOR' THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN20: intervencao exige conservador'; END IF;
END$$
CREATE TRIGGER trg_preservar_conservador BEFORE UPDATE ON pesquisador FOR EACH ROW
BEGIN
 DECLARE v_referencia INT DEFAULT NULL;
 IF NEW.tipo_especializacao<>'CONSERVADOR' THEN
 SELECT 1 INTO v_referencia FROM intervencao_conservacao WHERE id_conservador=OLD.id_pesquisador LIMIT 1 FOR SHARE;
 IF v_referencia IS NOT NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN20: pesquisador possui intervencoes e deve permanecer conservador'; END IF;
 END IF;
END$$
DELIMITER ;

DELIMITER $$
DROP TRIGGER IF EXISTS trg_arqueologo_tipo_insert$$
CREATE TRIGGER trg_arqueologo_tipo_insert BEFORE INSERT ON arqueologo FOR EACH ROW
BEGIN
 DECLARE v_tipo VARCHAR(24);
 SELECT tipo_especializacao INTO v_tipo FROM pesquisador WHERE id_pesquisador=NEW.id_pesquisador FOR SHARE;
 IF v_tipo IS NULL OR v_tipo<>'ARQUEOLOGO' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN03: subtipo incompativel com pesquisador'; END IF;
END$$
DROP TRIGGER IF EXISTS trg_arqueologo_tipo_update$$
CREATE TRIGGER trg_arqueologo_tipo_update BEFORE UPDATE ON arqueologo FOR EACH ROW
BEGIN
 DECLARE v_tipo VARCHAR(24);
 SELECT tipo_especializacao INTO v_tipo FROM pesquisador WHERE id_pesquisador=NEW.id_pesquisador FOR SHARE;
 IF v_tipo IS NULL OR v_tipo<>'ARQUEOLOGO' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN03: subtipo incompativel com pesquisador'; END IF;
END$$
DROP TRIGGER IF EXISTS trg_conservador_tipo_insert$$
CREATE TRIGGER trg_conservador_tipo_insert BEFORE INSERT ON conservador FOR EACH ROW
BEGIN
 DECLARE v_tipo VARCHAR(24);
 SELECT tipo_especializacao INTO v_tipo FROM pesquisador WHERE id_pesquisador=NEW.id_pesquisador FOR SHARE;
 IF v_tipo IS NULL OR v_tipo<>'CONSERVADOR' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN03: subtipo incompativel com pesquisador'; END IF;
END$$
DROP TRIGGER IF EXISTS trg_conservador_tipo_update$$
CREATE TRIGGER trg_conservador_tipo_update BEFORE UPDATE ON conservador FOR EACH ROW
BEGIN
 DECLARE v_tipo VARCHAR(24);
 SELECT tipo_especializacao INTO v_tipo FROM pesquisador WHERE id_pesquisador=NEW.id_pesquisador FOR SHARE;
 IF v_tipo IS NULL OR v_tipo<>'CONSERVADOR' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN03: subtipo incompativel com pesquisador'; END IF;
END$$
DROP TRIGGER IF EXISTS trg_analista_laboratorial_tipo_insert$$
CREATE TRIGGER trg_analista_laboratorial_tipo_insert BEFORE INSERT ON analista_laboratorial FOR EACH ROW
BEGIN
 DECLARE v_tipo VARCHAR(24);
 SELECT tipo_especializacao INTO v_tipo FROM pesquisador WHERE id_pesquisador=NEW.id_pesquisador FOR SHARE;
 IF v_tipo IS NULL OR v_tipo<>'ANALISTA_LABORATORIAL' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN03: subtipo incompativel com pesquisador'; END IF;
END$$
DROP TRIGGER IF EXISTS trg_analista_laboratorial_tipo_update$$
CREATE TRIGGER trg_analista_laboratorial_tipo_update BEFORE UPDATE ON analista_laboratorial FOR EACH ROW
BEGIN
 DECLARE v_tipo VARCHAR(24);
 SELECT tipo_especializacao INTO v_tipo FROM pesquisador WHERE id_pesquisador=NEW.id_pesquisador FOR SHARE;
 IF v_tipo IS NULL OR v_tipo<>'ANALISTA_LABORATORIAL' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN03: subtipo incompativel com pesquisador'; END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_pesquisador_preservar_subtipo;
DELIMITER $$
-- RN03: o serviço apaga o subtipo anterior, altera o supertipo e insere o novo
-- dentro de uma transação. A totalidade é verificada antes do COMMIT pelo serviço.
CREATE TRIGGER trg_pesquisador_preservar_subtipo BEFORE UPDATE ON pesquisador FOR EACH ROW
BEGIN
 DECLARE v_ref INT DEFAULT NULL;
 IF NEW.tipo_especializacao<>'ARQUEOLOGO' THEN
  SELECT 1 INTO v_ref FROM arqueologo WHERE id_pesquisador=OLD.id_pesquisador LIMIT 1 FOR SHARE;
  IF v_ref IS NOT NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN03: remova o subtipo anterior na transacao'; END IF;
 END IF;
 SET v_ref=NULL;
 IF NEW.tipo_especializacao<>'CONSERVADOR' THEN
  SELECT 1 INTO v_ref FROM conservador WHERE id_pesquisador=OLD.id_pesquisador LIMIT 1 FOR SHARE;
  IF v_ref IS NOT NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN03: remova o subtipo anterior na transacao'; END IF;
 END IF;
 SET v_ref=NULL;
 IF NEW.tipo_especializacao<>'ANALISTA_LABORATORIAL' THEN
  SELECT 1 INTO v_ref FROM analista_laboratorial WHERE id_pesquisador=OLD.id_pesquisador LIMIT 1 FOR SHARE;
  IF v_ref IS NOT NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RN03: remova o subtipo anterior na transacao'; END IF;
 END IF;
END$$
DELIMITER ;
