package br.edu.ucb.arqueologia;
import br.edu.ucb.arqueologia.db.*;import br.edu.ucb.arqueologia.ui.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;import static org.junit.jupiter.api.Assertions.*;
import javax.swing.*;import java.awt.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.Callable;import java.util.concurrent.atomic.AtomicReference;
@EnabledIfEnvironmentVariable(named="ARQ_TEST_ADMIN_PASSWORD",matches=".+")
@EnabledIfEnvironmentVariable(named="DISPLAY",matches=".+")
class SwingSmokeTest {
    static final Path EVIDENCE=Path.of("../docs/evidencias/etapa2");
    static <T>T edt(Callable<T> callable)throws Exception{AtomicReference<T> result=new AtomicReference<>();AtomicReference<Exception> error=new AtomicReference<>();SwingUtilities.invokeAndWait(()->{try{result.set(callable.call());}catch(Exception ex){error.set(ex);}});if(error.get()!=null)throw error.get();return result.get();}
    static Component find(Component c,String name){if(name.equals(c.getName()))return c;if(c instanceof Container parent)for(Component child:parent.getComponents()){Component hit=find(child,name);if(hit!=null)return hit;}return null;}
    static <T extends Component>T named(Component root,String name,Class<T> type){Component c=find(root,name);assertNotNull(c,name);return type.cast(c);}
    static void until(Callable<Boolean> condition)throws Exception{long end=System.nanoTime()+15_000_000_000L;while(System.nanoTime()<end){if(edt(condition))return;Thread.sleep(40);}fail("A interface não concluiu a operação a tempo.");}
    static <T extends Window>T window(Class<T> type){for(Window w:Window.getWindows())if(type.isInstance(w)&&w.isShowing())return type.cast(w);return null;}
    static void click(Component root,String name)throws Exception{edt(()->{named(root,name,JButton.class).doClick();return null;});}
    static void ready(JFrame f)throws Exception{until(()->!Boolean.TRUE.equals(f.getRootPane().getClientProperty("busy")));}
    static void screenshot(JFrame f,String filename)throws Exception{Files.createDirectories(EVIDENCE);Rectangle bounds=edt(f::getBounds);javax.imageio.ImageIO.write(new Robot().createScreenCapture(bounds),"png",EVIDENCE.resolve(filename).toFile());}
    static void tab(MainFrame main,String title)throws Exception{edt(()->{JTabbedPane tabs=named(main,"main.tabs",JTabbedPane.class);for(int i=0;i<tabs.getTabCount();i++)if(tabs.getTitleAt(i).equals(title)){tabs.setSelectedIndex(i);return null;}throw new AssertionError(title);});ready(main);}
    static Component selected(MainFrame main)throws Exception{return edt(()->named(main,"main.tabs",JTabbedPane.class).getSelectedComponent());}
    static JOptionPane option(){for(Window w:Window.getWindows())if(w instanceof JDialog && w.isShowing()){JOptionPane p=optionIn(w);if(p!=null)return p;}return null;}
    static JOptionPane optionIn(Component c){if(c instanceof JOptionPane p)return p;if(c instanceof Container parent)for(Component child:parent.getComponents()){var hit=optionIn(child);if(hit!=null)return hit;}return null;}
    static void confirmYes()throws Exception{until(()->option()!=null);edt(()->{option().setValue(JOptionPane.YES_OPTION);return null;});}
    static void fill(Component root,String field,String text)throws Exception{edt(()->{named(root,field,JTextField.class).setText(text);return null;});}
    @AfterEach void dispose()throws Exception{edt(()->{for(Window w:Window.getWindows())w.dispose();return null;});}
    @Test void loginCrudQueriesAndConstraintErrorThroughSwing()throws Exception{
        edt(()->{for(var laf:UIManager.getInstalledLookAndFeels())if(laf.getName().equals("Nimbus")){UIManager.setLookAndFeel(laf.getClassName());break;}new LoginFrame(new Database(Config.load())).setVisible(true);return null;});
        LoginFrame login=edt(()->window(LoginFrame.class));fill(login,"login.user","admin");
        edt(()->{named(login,"login.password",JPasswordField.class).setText(System.getenv("ARQ_TEST_ADMIN_PASSWORD"));return null;});click(login,"login.submit");until(()->window(MainFrame.class)!=null);
        MainFrame main=edt(()->window(MainFrame.class));ready(main);screenshot(main,"tela-principal.png");
        // Um CRUD completo pela interface: os quatro CRUDs são exercitados também nos testes de integração.
        tab(main,"Locais de armazenamento");String code="GUI-"+UUID.randomUUID().toString().substring(0,6);
        click(main,"new.LOCAIS");until(()->window(RecordDialog.class)!=null);RecordDialog dialog=edt(()->window(RecordDialog.class));fill(dialog,"field.codigo",code);fill(dialog,"field.capacidade","100");click(dialog,"record.save");until(()->window(RecordDialog.class)==null);ready(main);
        fill(main,"search.LOCAIS",code);click(main,"refresh.LOCAIS");ready(main);assertEquals(1,edt(()->named(main,"table.LOCAIS",JTable.class).getRowCount()));
        edt(()->{named(main,"table.LOCAIS",JTable.class).setRowSelectionInterval(0,0);return null;});click(main,"edit.LOCAIS");until(()->window(RecordDialog.class)!=null);dialog=edt(()->window(RecordDialog.class));String updated=code+"U";fill(dialog,"field.codigo",updated);click(dialog,"record.save");until(()->window(RecordDialog.class)==null);ready(main);
        fill(main,"search.LOCAIS",updated);click(main,"refresh.LOCAIS");ready(main);screenshot(main,"tela-crud-local.png");edt(()->{named(main,"table.LOCAIS",JTable.class).setRowSelectionInterval(0,0);return null;});
        // Confirmação modal roda no EDT em um loop aninhado; o teste continua em paralelo.
        SwingUtilities.invokeLater(()->named(main,"delete.LOCAIS",JButton.class).doClick());confirmYes();ready(main);
        click(main,"refresh.LOCAIS");ready(main);assertEquals(0,edt(()->named(main,"table.LOCAIS",JTable.class).getRowCount()));
        tab(main,"Consulta do acervo");click(main,"query.run.acervo");ready(main);screenshot(main,"tela-consulta.png");fill(main,"query.material","Cerâmica' OR 1=1 #");click(main,"query.run.acervo");ready(main);
        Component query=selected(main);assertEquals(0,edt(()->firstTable(query).getRowCount()));screenshot(main,"tela-injecao.png");
        tab(main,"Custódia atual");click(selected(main),"report.refresh");ready(main);assertTrue(edt(()->firstTable(selectedUnchecked(main)).getRowCount())>=100);screenshot(main,"tela-relatorio.png");
        tab(main,"Campanhas");until(()->named(main,"table.CAMPANHAS",JTable.class).getRowCount()>0);edt(()->{named(main,"table.CAMPANHAS",JTable.class).setRowSelectionInterval(0,0);return null;});
        // Busca uma campanha original com unidades, cuja exclusão deve ser negada pela FK.
        fill(main,"search.CAMPANHAS","Campanha de prospecção");click(main,"refresh.CAMPANHAS");ready(main);edt(()->{named(main,"table.CAMPANHAS",JTable.class).setRowSelectionInterval(0,0);return null;});
        SwingUtilities.invokeLater(()->named(main,"delete.CAMPANHAS",JButton.class).doClick());confirmYes();until(()->option()!=null && String.valueOf(option().getMessage()).contains("não pode ser removido"));screenshot(main,"tela-restricao.png");edt(()->{option().setValue(JOptionPane.OK_OPTION);return null;});
    }
    @Test void readerProfileHasOnlyPublicViews()throws Exception{
        edt(()->{new LoginFrame(new Database(Config.load())).setVisible(true);return null;});
        LoginFrame login=edt(()->window(LoginFrame.class));fill(login,"login.user","consulta");
        edt(()->{named(login,"login.password",JPasswordField.class).setText(System.getenv("ARQ_TEST_CONSULTA_PASSWORD"));return null;});click(login,"login.submit");until(()->window(MainFrame.class)!=null);
        MainFrame main=edt(()->window(MainFrame.class));ready(main);
        assertEquals(3,edt(()->named(main,"main.tabs",JTabbedPane.class).getTabCount()).intValue());
        tab(main,"Custódia atual");click(selected(main),"report.refresh");ready(main);assertTrue(edt(()->firstTable(selectedUnchecked(main)).getRowCount())>=100);screenshot(main,"tela-perfil-consulta.png");
    }
    static Component selectedUnchecked(MainFrame main){return named(main,"main.tabs",JTabbedPane.class).getSelectedComponent();}
    static JTable firstTable(Component c){if(c instanceof JTable t)return t;if(c instanceof Container p)for(Component child:p.getComponents()){JTable t=firstTable(child);if(t!=null)return t;}return null;}
}
