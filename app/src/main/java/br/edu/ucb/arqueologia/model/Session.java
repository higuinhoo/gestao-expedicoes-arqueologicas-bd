package br.edu.ucb.arqueologia.model;
import java.util.Arrays;
/** Credencial individual mantida em memória somente até o logout. */
public final class Session implements AutoCloseable {
    private final int id; private final String login,name; private final Role role; private final char[] password;
    public Session(int id,String login,String name,Role role,char[] password) {
        this.id=id;this.login=login;this.name=name;this.role=role;this.password=password.clone();
    }
    public int id(){return id;} public String login(){return login;} public String name(){return name;}
    public Role role(){return role;} public char[] password(){return password.clone();}
    public String actor(){return id+":"+login;}
    @Override public void close(){Arrays.fill(password,'\0');}
    @Override public String toString(){return name+" ("+role+")";}
}
