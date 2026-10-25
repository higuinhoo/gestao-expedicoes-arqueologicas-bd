-- Somente ambiente de demonstracao. Senha igual ao login, conforme configuracao do grupo.
-- Reexecutavel: preserva dados do dominio e atualiza somente estas tres contas.
SET SESSION sql_mode='STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION,NO_BACKSLASH_ESCAPES';
USE arqueologia_ucb;
CREATE USER IF NOT EXISTS 'admin'@'%' IDENTIFIED BY 'admin';
ALTER USER 'admin'@'%' IDENTIFIED BY 'admin';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'admin'@'%';
GRANT 'arqueologia_admin' TO 'admin'@'%';
SET DEFAULT ROLE 'arqueologia_admin' TO 'admin'@'%';
INSERT INTO usuario_app(login,nome,perfil,senha_hash,ativo) VALUES ('admin','Administrador do acervo','ADMIN','pbkdf2_sha256$600000$0mh3xo7dFlDZ7kRdtWPSJw==$sIepiAOAhR6IIMAkS5n7m4MDVjHN6c3kib6Kwhjr92g=',TRUE) ON DUPLICATE KEY UPDATE nome=VALUES(nome),perfil=VALUES(perfil),senha_hash=VALUES(senha_hash),ativo=TRUE;

CREATE USER IF NOT EXISTS 'catalogador'@'%' IDENTIFIED BY 'catalogador';
ALTER USER 'catalogador'@'%' IDENTIFIED BY 'catalogador';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'catalogador'@'%';
GRANT 'arqueologia_catalogador' TO 'catalogador'@'%';
SET DEFAULT ROLE 'arqueologia_catalogador' TO 'catalogador'@'%';
INSERT INTO usuario_app(login,nome,perfil,senha_hash,ativo) VALUES ('catalogador','Catalogador do acervo','CATALOGADOR','pbkdf2_sha256$600000$9kSrD/37CSI1LuwMTPJyJg==$4IZUAMl9nzIEMDDM5GOsRdabyO0FNxkkEpJzzb+4Z0w=',TRUE) ON DUPLICATE KEY UPDATE nome=VALUES(nome),perfil=VALUES(perfil),senha_hash=VALUES(senha_hash),ativo=TRUE;

CREATE USER IF NOT EXISTS 'consulta'@'%' IDENTIFIED BY 'consulta';
ALTER USER 'consulta'@'%' IDENTIFIED BY 'consulta';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'consulta'@'%';
GRANT 'arqueologia_consulta' TO 'consulta'@'%';
SET DEFAULT ROLE 'arqueologia_consulta' TO 'consulta'@'%';
INSERT INTO usuario_app(login,nome,perfil,senha_hash,ativo) VALUES ('consulta','Consulta publica','CONSULTA','pbkdf2_sha256$600000$ZzIFwdVnZ515/MLIWImLmQ==$M4gPAu+j7uGpFRhWssA4WrQeFmGxPdpZVlOnfCZ5t9Q=',TRUE) ON DUPLICATE KEY UPDATE nome=VALUES(nome),perfil=VALUES(perfil),senha_hash=VALUES(senha_hash),ativo=TRUE;
