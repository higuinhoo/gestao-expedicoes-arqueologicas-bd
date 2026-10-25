package br.edu.ucb.arqueologia.service;
import br.edu.ucb.arqueologia.db.Database;import br.edu.ucb.arqueologia.model.*;
import java.sql.*;import java.util.*;
public final class AuthService {
    private final Database db;
    public AuthService(Database db){this.db=db;}
    public Session login(String login,char[] password)throws SQLException {
        try(Connection c=db.connect(login,password)){
            var rows=Database.query(c,"SELECT id_usuario,login,nome,perfil,senha_hash FROM vw_minha_conta WHERE login=? AND ativo=TRUE",login);
            if(rows.size()!=1 || !Passwords.verify(password,(String)rows.get(0).get("senha_hash")))throw new BusinessException("Usuário ou senha inválidos.");
            var r=rows.get(0);return new Session(((Number)r.get("id_usuario")).intValue(),login,(String)r.get("nome"),Role.valueOf((String)r.get("perfil")),password);
        }
    }
}
