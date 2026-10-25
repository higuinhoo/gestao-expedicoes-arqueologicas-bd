package br.edu.ucb.arqueologia;

import br.edu.ucb.arqueologia.db.*;
import br.edu.ucb.arqueologia.model.*;
import br.edu.ucb.arqueologia.service.*;
import br.edu.ucb.arqueologia.ui.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import javax.swing.*;
import java.sql.Connection;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.ucb.arqueologia.SwingSmokeTest.*;

@EnabledIfEnvironmentVariable(named="ARQ_TEST_ADMIN_PASSWORD",matches=".+")
@EnabledIfEnvironmentVariable(named="DISPLAY",matches=".+")
class TransferPanelTest {
    Database db;Session session;MainFrame frame;
    @BeforeEach void open()throws Exception{
        db=new Database(Config.load());session=new AuthService(db).login("admin",System.getenv("ARQ_TEST_ADMIN_PASSWORD").toCharArray());
        frame=edt(()->{MainFrame f=new MainFrame(db,session,()->{});f.setVisible(true);return f;});
        ready(frame);tab(frame,"Transferir custódia");
    }
    @AfterEach void close()throws Exception{edt(()->{for(var window:java.awt.Window.getWindows())window.dispose();return null;});if(session!=null)session.close();}
    @SuppressWarnings("unchecked") JComboBox<UI.Choice> box(String key){return named(frame,"transfer."+key,JComboBox.class);}
    void choose(String key,int id)throws Exception{edt(()->{UI.select(box(key),id);return null;});}
    @Test void openingTabLoadsAllRequiredSelectorsWithoutAnExtraClick()throws Exception{
        for(String key:List.of("artifact","institution","responsible")){
            assertTrue(edt(()->box(key).isEnabled()));assertTrue(edt(()->box(key).getItemCount()>0));assertNotNull(edt(()->UI.selectedId(box(key))));
        }
        assertTrue(edt(()->named(frame,"transfer.submit",JButton.class).isEnabled()));
    }
    @Test void changingInstitutionFiltersLocationsAndClearsIncompatibleSelection()throws Exception{
        var locations=new CustodyService(db).lookups(session).get("location");
        var first=locations.get(0);int institution=((Number)first.get("id_instituicao")).intValue(),local=((Number)first.get("id_local")).intValue();
        choose("institution",institution);choose("location",local);assertEquals(local,edt(()->UI.selectedId(box("location"))));
        var other=locations.stream().filter(r->((Number)r.get("id_instituicao")).intValue()!=institution).findFirst().orElseThrow();
        int destination=((Number)other.get("id_instituicao")).intValue();choose("institution",destination);
        assertNull(edt(()->UI.selectedId(box("location"))));
        for(int i=1;i<edt(()->box("location").getItemCount());i++){
            int index=i;int id=((Number)edt(()->box("location").getItemAt(index).id())).intValue();
            assertTrue(locations.stream().anyMatch(r->((Number)r.get("id_local")).intValue()==id && ((Number)r.get("id_instituicao")).intValue()==destination));
        }
    }
    @Test void refreshingAndReopeningKeepEnteredTextAndSelections()throws Exception{
        fill(frame,"transfer.document","SWING-PREENCHIMENTO");fill(frame,"transfer.reason","Teste de conservação e rastreabilidade");fill(frame,"transfer.date","2026-10-25");
        choose("institution",2);choose("artifact",2);click(frame,"transfer.load");ready(frame);
        tab(frame,"Campanhas");tab(frame,"Transferir custódia");
        assertEquals(2,edt(()->UI.selectedId(box("institution"))));assertEquals(2,edt(()->UI.selectedId(box("artifact"))));
        assertEquals("SWING-PREENCHIMENTO",edt(()->named(frame,"transfer.document",JTextField.class).getText()));
        assertEquals("Teste de conservação e rastreabilidade",edt(()->named(frame,"transfer.reason",JTextField.class).getText()));
        assertEquals("2026-10-25",edt(()->named(frame,"transfer.date",JTextField.class).getText()));
        screenshot(frame,"transferencia-preenchida.png");
    }
    @Test void transferThroughSwingRollsBackOnFailureAndCommitsOnSuccess()throws Exception{
        int id=new CatalogService(db).save(session,EntitySpec.ARTEFATOS,null,IntegrationTest.artifact("Transferência Swing"));
        String suffix=UUID.randomUUID().toString().substring(0,8);
        try(Connection c=db.open(session)){
            Database.execute(c,"INSERT INTO custodia_artefato (id_artefato,id_instituicao,id_responsavel,data_inicio,documento_movimentacao,motivo) VALUES (?,1,1,'2026-10-01',?,'Recebimento de teste')",id,"INI-SWING-"+suffix);
        }
        click(frame,"transfer.load");ready(frame);choose("artifact",id);choose("institution",2);
        fill(frame,"transfer.date","2026-10-25");fill(frame,"transfer.document","MOV-SWING-"+suffix);fill(frame,"transfer.reason","Teste de transação pela interface");
        edt(()->{named(frame,"transfer.fail",JCheckBox.class).setSelected(true);return null;});click(frame,"transfer.submit");
        until(()->option()!=null);assertTrue(edt(()->String.valueOf(option().getMessage())).contains("Falha simulada"));
        edt(()->{option().setValue(JOptionPane.OK_OPTION);return null;});ready(frame);
        try(Connection c=db.open(session)){
            assertEquals("CATALOGADO",Database.scalar(c,"SELECT situacao FROM artefato WHERE id_artefato=?",id));
            assertEquals(1,((Number)Database.scalar(c,"SELECT COUNT(*) FROM custodia_artefato WHERE id_artefato=?",id)).intValue());
            assertNull(Database.scalar(c,"SELECT data_fim FROM custodia_artefato WHERE id_artefato=?",id));
        }
        edt(()->{named(frame,"transfer.fail",JCheckBox.class).setSelected(false);return null;});click(frame,"transfer.submit");
        until(()->option()!=null);assertTrue(edt(()->String.valueOf(option().getMessage())).contains("Transferência confirmada"));
        edt(()->{option().setValue(JOptionPane.OK_OPTION);return null;});ready(frame);
        try(Connection c=db.open(session)){
            assertEquals("ARMAZENADO",Database.scalar(c,"SELECT situacao FROM artefato WHERE id_artefato=?",id));
            assertEquals(2,((Number)Database.scalar(c,"SELECT COUNT(*) FROM custodia_artefato WHERE id_artefato=?",id)).intValue());
            assertEquals(1,((Number)Database.scalar(c,"SELECT COUNT(*) FROM custodia_artefato WHERE id_artefato=? AND data_fim IS NULL",id)).intValue());
        }
    }
}
