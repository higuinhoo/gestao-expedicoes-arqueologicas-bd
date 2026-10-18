package br.edu.ucb.arqueologia.tools;
import br.edu.ucb.arqueologia.db.*;import br.edu.ucb.arqueologia.service.Passwords;
import java.io.Console;import java.sql.*;import java.util.*;
/** Alternativa Java para quem usa MySQL instalado, sem Docker/Python.
 * Crie as três contas MySQL e atribua as roles pelo Workbench antes deste cadastro. */
public final class RegisterAccounts {
    public static void main(String[] args)throws Exception{
        Console console=System.console();if(console==null)throw new IllegalStateException("Execute em um terminal, para ler senhas sem exibi-las.");
        Database db=new Database(Config.load());String maintenance=console.readLine("Usuário de manutenção MySQL: ");char[] rootSecret=console.readPassword("Senha de manutenção: ");
        try(Connection c=db.connect(maintenance,rootSecret)){
            for(String login:List.of("admin","catalogador","consulta")){
                char[] secret=console.readPassword("Senha da conta MySQL %s (já criada): ",login);
                try{
                    if(secret.length<12)throw new IllegalArgumentException("Use pelo menos doze caracteres.");
                    try(Connection probe=db.connect(login,secret)){Database.scalar(probe,"SELECT 1");}
                    Database.execute(c,"INSERT INTO usuario_app(login,nome,perfil,senha_hash,ativo) VALUES (?,?,?,?,TRUE) ON DUPLICATE KEY UPDATE nome=VALUES(nome),perfil=VALUES(perfil),senha_hash=VALUES(senha_hash),ativo=TRUE",login,login,login.equals("admin")?"ADMIN":login.toUpperCase(Locale.ROOT),Passwords.hash(secret));
                    System.out.println("Conta registrada com hash e sal: "+login);
                }finally{Arrays.fill(secret,'\0');}
            }
        }finally{Arrays.fill(rootSecret,'\0');}
    }
}
