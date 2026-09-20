-- ============================================================
-- 01_ddl.sql
-- Sistema de Gestão de Expedições Arqueológicas
-- MySQL 8.x / InnoDB
-- ============================================================

CREATE DATABASE IF NOT EXISTS arqueologia_ucb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE arqueologia_ucb;

-- Onde cada regra de negócio aparece no banco:
-- RN01: instituicao — nome único e tipo restrito a UNIVERSIDADE, MUSEU, LABORATORIO, ORGAO_PUBLICO
-- RN02-RN04: pesquisador e subtipos — FK de instituição, e-mail único, discriminador e situação
-- RN05: sitio_arqueologico — campos obrigatórios e limites de latitude/longitude
-- RN06-RN09: campanha e participacao_campanha — código único, datas, coordenação e N:N com atributos
-- RN10-RN12: unidade_escavacao — código único por campanha, período e vínculo obrigatório
-- RN13-RN15: artefato — catálogo único, unidade de descoberta e autorreferência bloqueada por trigger
-- RN16: fragmento — entidade fraca, identificada pela combinação (id_artefato, numero_fragmento)
-- RN17-RN19: analise_laboratorial e artefato_analise — laboratório, período e N:N com atributos
-- RN20-RN21: intervencao_conservacao — conservador, período, situação e custo
-- RN22: local_armazenamento — código único por instituição
-- RN23-RN25: custodia_artefato — histórico datado, documento único e uma só custódia em aberto por artefato

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS custodia_artefato;
DROP TABLE IF EXISTS local_armazenamento;
DROP TABLE IF EXISTS intervencao_conservacao;
DROP TABLE IF EXISTS artefato_analise;
DROP TABLE IF EXISTS analise_laboratorial;
DROP TABLE IF EXISTS fragmento;
DROP TABLE IF EXISTS artefato;
DROP TABLE IF EXISTS unidade_escavacao;
DROP TABLE IF EXISTS participacao_campanha;
DROP TABLE IF EXISTS campanha;
DROP TABLE IF EXISTS sitio_arqueologico;
DROP TABLE IF EXISTS analista_laboratorial;
DROP TABLE IF EXISTS conservador;
DROP TABLE IF EXISTS arqueologo;
DROP TABLE IF EXISTS pesquisador;
DROP TABLE IF EXISTS instituicao;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE instituicao (
    id_instituicao INT AUTO_INCREMENT,
    nome VARCHAR(120) NOT NULL,
    sigla VARCHAR(20),
    tipo VARCHAR(20) NOT NULL,
    pais VARCHAR(60) NOT NULL DEFAULT 'Brasil',
    uf CHAR(2),
    cidade VARCHAR(80) NOT NULL,
    CONSTRAINT pk_instituicao PRIMARY KEY (id_instituicao),
    CONSTRAINT uq_instituicao_nome UNIQUE (nome),
    CONSTRAINT ck_instituicao_tipo CHECK (tipo IN ('UNIVERSIDADE','MUSEU','LABORATORIO','ORGAO_PUBLICO'))
) ENGINE = InnoDB;

CREATE TABLE pesquisador (
    id_pesquisador INT AUTO_INCREMENT,
    id_instituicao INT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    registro_profissional VARCHAR(40),
    tipo_especializacao VARCHAR(24) NOT NULL,
    situacao VARCHAR(10) NOT NULL DEFAULT 'ATIVO',
    CONSTRAINT pk_pesquisador PRIMARY KEY (id_pesquisador),
    CONSTRAINT uq_pesquisador_email UNIQUE (email),
    CONSTRAINT uq_pesquisador_registro UNIQUE (registro_profissional),
    CONSTRAINT ck_pesquisador_especializacao CHECK (tipo_especializacao IN ('ARQUEOLOGO','CONSERVADOR','ANALISTA_LABORATORIAL')),
    CONSTRAINT ck_pesquisador_situacao CHECK (situacao IN ('ATIVO','INATIVO')),
    CONSTRAINT fk_pesquisador_instituicao FOREIGN KEY (id_instituicao)
        REFERENCES instituicao (id_instituicao) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_pesquisador_instituicao (id_instituicao)
) ENGINE = InnoDB;

CREATE TABLE arqueologo (
    id_pesquisador INT,
    area_pesquisa VARCHAR(100) NOT NULL,
    numero_autorizacao VARCHAR(40) NOT NULL,
    CONSTRAINT pk_arqueologo PRIMARY KEY (id_pesquisador),
    CONSTRAINT uq_arqueologo_autorizacao UNIQUE (numero_autorizacao),
    CONSTRAINT fk_arqueologo_pesquisador FOREIGN KEY (id_pesquisador)
        REFERENCES pesquisador (id_pesquisador) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE conservador (
    id_pesquisador INT,
    especialidade_material VARCHAR(80) NOT NULL,
    numero_registro VARCHAR(40) NOT NULL,
    CONSTRAINT pk_conservador PRIMARY KEY (id_pesquisador),
    CONSTRAINT uq_conservador_registro UNIQUE (numero_registro),
    CONSTRAINT fk_conservador_pesquisador FOREIGN KEY (id_pesquisador)
        REFERENCES pesquisador (id_pesquisador) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE analista_laboratorial (
    id_pesquisador INT,
    especialidade_analise VARCHAR(100) NOT NULL,
    nivel_formacao VARCHAR(30) NOT NULL,
    CONSTRAINT pk_analista_laboratorial PRIMARY KEY (id_pesquisador),
    CONSTRAINT fk_analista_pesquisador FOREIGN KEY (id_pesquisador)
        REFERENCES pesquisador (id_pesquisador) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE sitio_arqueologico (
    id_sitio INT AUTO_INCREMENT,
    nome VARCHAR(120) NOT NULL,
    municipio VARCHAR(80) NOT NULL,
    uf CHAR(2) NOT NULL,
    latitude DECIMAL(9,6) NOT NULL,
    longitude DECIMAL(9,6) NOT NULL,
    periodo_historico VARCHAR(100),
    nivel_protecao VARCHAR(20) NOT NULL,
    data_cadastro DATE NOT NULL,
    CONSTRAINT pk_sitio_arqueologico PRIMARY KEY (id_sitio),
    CONSTRAINT uq_sitio_nome UNIQUE (nome),
    CONSTRAINT ck_sitio_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_sitio_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_sitio_protecao CHECK (nivel_protecao IN ('SEM_PROTECAO','MUNICIPAL','ESTADUAL','FEDERAL')),
    INDEX idx_sitio_localizacao (uf, municipio)
) ENGINE = InnoDB;

CREATE TABLE campanha (
    id_campanha INT AUTO_INCREMENT,
    id_sitio INT NOT NULL,
    id_coordenador INT NOT NULL,
    codigo VARCHAR(24) NOT NULL,
    nome VARCHAR(140) NOT NULL,
    objetivo TEXT NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    situacao VARCHAR(16) NOT NULL,
    orcamento DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_campanha PRIMARY KEY (id_campanha),
    CONSTRAINT uq_campanha_codigo UNIQUE (codigo),
    CONSTRAINT ck_campanha_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio),
    CONSTRAINT ck_campanha_situacao CHECK (situacao IN ('PLANEJADA','EM_ANDAMENTO','CONCLUIDA','SUSPENSA')),
    CONSTRAINT ck_campanha_orcamento CHECK (orcamento >= 0),
    CONSTRAINT fk_campanha_sitio FOREIGN KEY (id_sitio)
        REFERENCES sitio_arqueologico (id_sitio) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_campanha_coordenador FOREIGN KEY (id_coordenador)
        REFERENCES pesquisador (id_pesquisador) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_campanha_sitio (id_sitio),
    INDEX idx_campanha_coordenador (id_coordenador),
    INDEX idx_campanha_periodo (data_inicio, data_fim)
) ENGINE = InnoDB;

CREATE TABLE participacao_campanha (
    id_campanha INT NOT NULL,
    id_pesquisador INT NOT NULL,
    funcao VARCHAR(60) NOT NULL,
    data_entrada DATE NOT NULL,
    data_saida DATE,
    horas_previstas SMALLINT NOT NULL,
    CONSTRAINT pk_participacao_campanha PRIMARY KEY (id_campanha, id_pesquisador),
    CONSTRAINT ck_participacao_periodo CHECK (data_saida IS NULL OR data_saida >= data_entrada),
    CONSTRAINT ck_participacao_horas CHECK (horas_previstas > 0),
    CONSTRAINT fk_participacao_campanha FOREIGN KEY (id_campanha)
        REFERENCES campanha (id_campanha) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_participacao_pesquisador FOREIGN KEY (id_pesquisador)
        REFERENCES pesquisador (id_pesquisador) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_participacao_pesquisador (id_pesquisador)
) ENGINE = InnoDB;

CREATE TABLE unidade_escavacao (
    id_unidade INT AUTO_INCREMENT,
    id_campanha INT NOT NULL,
    codigo VARCHAR(20) NOT NULL,
    setor VARCHAR(40) NOT NULL,
    nivel_estratigrafico VARCHAR(40),
    data_abertura DATE NOT NULL,
    data_encerramento DATE,
    CONSTRAINT pk_unidade_escavacao PRIMARY KEY (id_unidade),
    CONSTRAINT uq_unidade_codigo UNIQUE (id_campanha, codigo),
    CONSTRAINT ck_unidade_periodo CHECK (data_encerramento IS NULL OR data_encerramento >= data_abertura),
    CONSTRAINT fk_unidade_campanha FOREIGN KEY (id_campanha)
        REFERENCES campanha (id_campanha) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_unidade_campanha (id_campanha)
) ENGINE = InnoDB;

CREATE TABLE artefato (
    id_artefato INT AUTO_INCREMENT,
    id_unidade INT NOT NULL,
    id_artefato_referencia INT,
    codigo_catalogo VARCHAR(32) NOT NULL,
    nome VARCHAR(120) NOT NULL,
    descricao TEXT NOT NULL,
    material VARCHAR(60) NOT NULL,
    periodo_estimado VARCHAR(100),
    data_descoberta DATE NOT NULL,
    profundidade_cm DECIMAL(7,2) NOT NULL,
    estado_conservacao VARCHAR(16) NOT NULL,
    situacao VARCHAR(18) NOT NULL DEFAULT 'CATALOGADO',
    CONSTRAINT pk_artefato PRIMARY KEY (id_artefato),
    CONSTRAINT uq_artefato_catalogo UNIQUE (codigo_catalogo),
    CONSTRAINT ck_artefato_profundidade CHECK (profundidade_cm >= 0),
    CONSTRAINT ck_artefato_estado CHECK (estado_conservacao IN ('INTEGRO','ESTAVEL','FRAGIL','CRITICO')),
    CONSTRAINT ck_artefato_situacao CHECK (situacao IN ('CATALOGADO','EM_ANALISE','EM_CONSERVACAO','ARMAZENADO','EXPOSTO')),
    CONSTRAINT fk_artefato_unidade FOREIGN KEY (id_unidade)
        REFERENCES unidade_escavacao (id_unidade) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_artefato_referencia FOREIGN KEY (id_artefato_referencia)
        REFERENCES artefato (id_artefato) ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_artefato_unidade (id_unidade),
    INDEX idx_artefato_referencia (id_artefato_referencia),
    INDEX idx_artefato_material (material)
) ENGINE = InnoDB;

DELIMITER $$

CREATE TRIGGER trg_artefato_sem_autorreferencia_ins
BEFORE INSERT ON artefato
FOR EACH ROW
BEGIN
    IF NEW.id_artefato_referencia IS NOT NULL
       AND NEW.id_artefato_referencia = NEW.id_artefato THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Um artefato nao pode referenciar a si proprio';
    END IF;
END$$

CREATE TRIGGER trg_artefato_sem_autorreferencia_upd
BEFORE UPDATE ON artefato
FOR EACH ROW
BEGIN
    IF NEW.id_artefato_referencia IS NOT NULL
       AND NEW.id_artefato_referencia = NEW.id_artefato THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Um artefato nao pode referenciar a si proprio';
    END IF;
END$$

DELIMITER ;

CREATE TABLE fragmento (
    id_artefato INT NOT NULL,
    numero_fragmento SMALLINT NOT NULL,
    peso_g DECIMAL(9,3) NOT NULL,
    comprimento_mm DECIMAL(8,2),
    descricao VARCHAR(240),
    localizado BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_fragmento PRIMARY KEY (id_artefato, numero_fragmento),
    CONSTRAINT ck_fragmento_numero CHECK (numero_fragmento > 0),
    CONSTRAINT ck_fragmento_peso CHECK (peso_g >= 0),
    CONSTRAINT ck_fragmento_comprimento CHECK (comprimento_mm IS NULL OR comprimento_mm >= 0),
    CONSTRAINT fk_fragmento_artefato FOREIGN KEY (id_artefato)
        REFERENCES artefato (id_artefato) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE analise_laboratorial (
    id_analise INT AUTO_INCREMENT,
    id_laboratorio INT NOT NULL,
    tipo_analise VARCHAR(60) NOT NULL,
    metodo VARCHAR(120) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    situacao VARCHAR(16) NOT NULL,
    custo DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_analise_laboratorial PRIMARY KEY (id_analise),
    CONSTRAINT ck_analise_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio),
    CONSTRAINT ck_analise_situacao CHECK (situacao IN ('SOLICITADA','EM_EXECUCAO','CONCLUIDA','CANCELADA')),
    CONSTRAINT ck_analise_custo CHECK (custo >= 0),
    CONSTRAINT fk_analise_laboratorio FOREIGN KEY (id_laboratorio)
        REFERENCES instituicao (id_instituicao) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_analise_laboratorio (id_laboratorio),
    INDEX idx_analise_periodo (data_inicio, data_fim)
) ENGINE = InnoDB;

CREATE TABLE artefato_analise (
    id_artefato INT NOT NULL,
    id_analise INT NOT NULL,
    codigo_amostra VARCHAR(32) NOT NULL,
    massa_consumida_g DECIMAL(8,3) NOT NULL DEFAULT 0.000,
    resultado_resumido VARCHAR(300),
    conclusao TEXT,
    CONSTRAINT pk_artefato_analise PRIMARY KEY (id_artefato, id_analise),
    CONSTRAINT uq_artefato_analise_amostra UNIQUE (codigo_amostra),
    CONSTRAINT ck_artefato_analise_massa CHECK (massa_consumida_g >= 0),
    CONSTRAINT fk_artefato_analise_artefato FOREIGN KEY (id_artefato)
        REFERENCES artefato (id_artefato) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_artefato_analise_analise FOREIGN KEY (id_analise)
        REFERENCES analise_laboratorial (id_analise) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_artefato_analise_analise (id_analise)
) ENGINE = InnoDB;

CREATE TABLE intervencao_conservacao (
    id_intervencao INT AUTO_INCREMENT,
    id_artefato INT NOT NULL,
    id_conservador INT NOT NULL,
    tipo_intervencao VARCHAR(80) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    descricao TEXT NOT NULL,
    material_aplicado VARCHAR(160),
    situacao VARCHAR(16) NOT NULL,
    custo DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_intervencao_conservacao PRIMARY KEY (id_intervencao),
    CONSTRAINT ck_intervencao_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio),
    CONSTRAINT ck_intervencao_situacao CHECK (situacao IN ('PLANEJADA','EM_EXECUCAO','CONCLUIDA','CANCELADA')),
    CONSTRAINT ck_intervencao_custo CHECK (custo >= 0),
    CONSTRAINT fk_intervencao_artefato FOREIGN KEY (id_artefato)
        REFERENCES artefato (id_artefato) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_intervencao_conservador FOREIGN KEY (id_conservador)
        REFERENCES pesquisador (id_pesquisador) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_intervencao_artefato (id_artefato),
    INDEX idx_intervencao_conservador (id_conservador)
) ENGINE = InnoDB;

CREATE TABLE local_armazenamento (
    id_local INT AUTO_INCREMENT,
    id_instituicao INT NOT NULL,
    codigo VARCHAR(24) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    temperatura_c DECIMAL(5,2),
    umidade_percentual DECIMAL(5,2),
    capacidade INT NOT NULL,
    CONSTRAINT pk_local_armazenamento PRIMARY KEY (id_local),
    CONSTRAINT uq_local_codigo UNIQUE (id_instituicao, codigo),
    CONSTRAINT ck_local_umidade CHECK (umidade_percentual IS NULL OR umidade_percentual BETWEEN 0 AND 100),
    CONSTRAINT ck_local_capacidade CHECK (capacidade > 0),
    CONSTRAINT fk_local_instituicao FOREIGN KEY (id_instituicao)
        REFERENCES instituicao (id_instituicao) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_local_instituicao (id_instituicao)
) ENGINE = InnoDB;

CREATE TABLE custodia_artefato (
    id_custodia INT AUTO_INCREMENT,
    id_artefato INT NOT NULL,
    id_instituicao INT NOT NULL,
    id_local INT,
    id_responsavel INT NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    documento_movimentacao VARCHAR(40) NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    id_artefato_em_aberto INT GENERATED ALWAYS AS (
        CASE WHEN data_fim IS NULL THEN id_artefato ELSE NULL END
    ) STORED,
    CONSTRAINT pk_custodia_artefato PRIMARY KEY (id_custodia),
    CONSTRAINT uq_custodia_documento UNIQUE (documento_movimentacao),
    CONSTRAINT uq_custodia_inicio UNIQUE (id_artefato, data_inicio),
    CONSTRAINT uq_custodia_aberta UNIQUE (id_artefato_em_aberto),
    CONSTRAINT ck_custodia_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio),
    CONSTRAINT fk_custodia_artefato FOREIGN KEY (id_artefato)
        REFERENCES artefato (id_artefato) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_custodia_instituicao FOREIGN KEY (id_instituicao)
        REFERENCES instituicao (id_instituicao) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_custodia_local FOREIGN KEY (id_local)
        REFERENCES local_armazenamento (id_local) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_custodia_responsavel FOREIGN KEY (id_responsavel)
        REFERENCES pesquisador (id_pesquisador) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_custodia_artefato_periodo (id_artefato, data_inicio, data_fim),
    INDEX idx_custodia_instituicao (id_instituicao),
    INDEX idx_custodia_local (id_local),
    INDEX idx_custodia_responsavel (id_responsavel)
) ENGINE = InnoDB;
