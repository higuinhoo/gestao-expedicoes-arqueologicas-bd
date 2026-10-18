package br.edu.ucb.arqueologia.tools;
import br.edu.ucb.arqueologia.db.*;import br.edu.ucb.arqueologia.model.Session;import br.edu.ucb.arqueologia.service.AuthService;
import java.sql.*;import java.util.concurrent.*;import java.time.Instant;
public final class ConcurrentExperiment {
    public static void main(String[] args)throws Exception{
        Database db=new Database(Config.load());String password=required("ARQ_TEST_ADMIN_PASSWORD");int artifact=args.length==0?1:Integer.parseInt(args[0]);
        try(Session session=new AuthService(db).login("admin",password.toCharArray());Connection first=db.open(session);Connection second=db.open(session)){
            System.out.println("Experimento observado em "+Instant.now());
            first.setAutoCommit(false);second.setAutoCommit(false);
            System.out.println("Sessão A: id="+Database.scalar(first,"SELECT CONNECTION_ID()")+", isolamento="+Database.scalar(first,"SELECT @@transaction_isolation"));
            System.out.println("Sessão B: id="+Database.scalar(second,"SELECT CONNECTION_ID()")+", isolamento="+Database.scalar(second,"SELECT @@transaction_isolation"));
            Database.query(first,"SELECT id_artefato FROM artefato WHERE id_artefato=? FOR UPDATE",artifact);
            System.out.println("A: BEGIN; SELECT id_artefato FROM artefato WHERE id_artefato="+artifact+" FOR UPDATE; lock adquirido.");
            ExecutorService executor=Executors.newSingleThreadExecutor();CountDownLatch ready=new CountDownLatch(1);
            try{
                Future<Long> blocked=executor.submit(()->{ready.countDown();long start=System.nanoTime();Database.query(second,"SELECT id_artefato FROM artefato WHERE id_artefato=? FOR UPDATE",artifact);long elapsed=System.nanoTime()-start;second.rollback();return elapsed;});
                if(!ready.await(2,TimeUnit.SECONDS))throw new IllegalStateException("Segunda sessão não iniciou.");
                try{blocked.get(900,TimeUnit.MILLISECONDS);throw new IllegalStateException("Não houve espera; confira o cenário.");}
                catch(TimeoutException expected){System.out.println("B: SELECT FOR UPDATE permanece pendente após 900 ms.");}
                try(Connection observer=db.connect(required("ARQ_MAINT_USER"),required("ARQ_MAINT_PASSWORD").toCharArray())){
                    var waits=Database.query(observer,"SELECT dl.OBJECT_NAME,dl.INDEX_NAME,dl.LOCK_TYPE,dl.LOCK_MODE,dl.LOCK_STATUS FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks dl ON dl.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID");
                    if(waits.isEmpty())throw new IllegalStateException("Não foi observado lock pendente no servidor.");for(var row:waits)System.out.println("Observação performance_schema: "+row);
                }
                first.commit();System.out.println("A: COMMIT, liberando o lock.");
                long elapsed=blocked.get(5,TimeUnit.SECONDS);System.out.printf(java.util.Locale.ROOT,"B: lock adquirido após %.3f ms; ROLLBACK. Nenhum dado foi alterado.%n",elapsed/1e6);
            }finally{first.rollback();second.rollback();executor.shutdownNow();}
        }
    }
    static String required(String key){String value=System.getenv(key);if(value==null)throw new IllegalArgumentException("Defina "+key+" para este experimento em base isolada.");return value;}
}
