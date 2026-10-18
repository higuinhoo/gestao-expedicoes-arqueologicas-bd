package br.edu.ucb.arqueologia.db;
import br.edu.ucb.arqueologia.model.Session;
import java.sql.*;import java.util.*;
public final class Database {
    private final Config config;
    public Database(Config config){this.config=config;}
    public Connection connect(String user,char[] password) throws SQLException {
        Properties p=new Properties();p.setProperty("user",user);p.setProperty("password",new String(password));
        p.setProperty("useServerPrepStmts","true");p.setProperty("connectTimeout","5000");p.setProperty("socketTimeout","15000");
        Connection c=DriverManager.getConnection(config.url(),p);
        try{c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);return c;}
        catch(SQLException ex){c.close();throw ex;}
    }
    public Connection open(Session session)throws SQLException {
        char[] secret=session.password();
        Connection c;
        try{c=connect(session.login(),secret);}finally{Arrays.fill(secret,'\0');}
        try{execute(c,"SET @app_actor = ?",session.actor());return c;}
        catch(SQLException ex){c.close();throw ex;}
    }
    public static PreparedStatement prepare(Connection c,String sql,Object... args)throws SQLException {
        PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
        try{
            for(int i=0;i<args.length;i++) {
                Object value=args[i];
                if(value instanceof java.time.LocalDate d)p.setDate(i+1,java.sql.Date.valueOf(d));
                else p.setObject(i+1,value);
            }
            return p;
        }catch(SQLException ex){p.close();throw ex;}
    }
    public static List<Map<String,Object>> query(Connection c,String sql,Object... args)throws SQLException {
        try(PreparedStatement p=prepare(c,sql,args);ResultSet r=p.executeQuery()) {
            List<Map<String,Object>> rows=new ArrayList<>();ResultSetMetaData meta=r.getMetaData();
            while(r.next()){
                Map<String,Object> row=new LinkedHashMap<>();
                for(int i=1;i<=meta.getColumnCount();i++)row.put(meta.getColumnLabel(i),r.getObject(i));
                rows.add(row);
            }
            return rows;
        }
    }
    public static int execute(Connection c,String sql,Object... args)throws SQLException {
        try(PreparedStatement p=prepare(c,sql,args)){return p.executeUpdate();}
    }
    public static int insert(Connection c,String sql,Object... args)throws SQLException {
        try(PreparedStatement p=prepare(c,sql,args)){
            p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){
                if(!r.next())throw new SQLException("A inclusão não retornou identificador.");return r.getInt(1);
            }
        }
    }
    public static Object scalar(Connection c,String sql,Object... args)throws SQLException {
        List<Map<String,Object>> rows=query(c,sql,args);
        return rows.isEmpty()?null:rows.get(0).values().iterator().next();
    }
}
