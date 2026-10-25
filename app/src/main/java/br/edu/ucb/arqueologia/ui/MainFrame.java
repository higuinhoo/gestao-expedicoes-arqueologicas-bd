package br.edu.ucb.arqueologia.ui;
import br.edu.ucb.arqueologia.db.Database;import br.edu.ucb.arqueologia.model.*;import br.edu.ucb.arqueologia.service.*;
import javax.swing.*;import java.awt.*;import java.awt.event.*;
public final class MainFrame extends JFrame {
    public MainFrame(Database db,Session session,Runnable logout){
        super("Acervo arqueológico — "+session.name());setName("main.frame");setDefaultCloseOperation(DISPOSE_ON_CLOSE);setSize(1360,850);setMinimumSize(new Dimension(1060,650));setLocationRelativeTo(null);
        JPanel header=UI.padded(new BorderLayout());JLabel title=new JLabel("Gestão de Expedições Arqueológicas");title.setFont(title.getFont().deriveFont(Font.BOLD,22f));header.add(title,BorderLayout.WEST);
        JPanel account=new JPanel(new FlowLayout(FlowLayout.RIGHT));account.add(new JLabel(session.toString()));account.add(UI.button("Sair","logout",()->{dispose();logout.run();}));header.add(account,BorderLayout.EAST);
        JTabbedPane tabs=new JTabbedPane();tabs.setName("main.tabs");CatalogService catalog=new CatalogService(db);ReportService reports=new ReportService(db);
        for(var spec:EntitySpec.values())if(spec.permits(session.role()))tabs.addTab(spec.title,new CrudPanel(session,catalog,spec));
        tabs.addTab("Consulta do acervo",new QueryPanel(session,reports,false));tabs.addTab("Consulta de análises",new QueryPanel(session,reports,true));
        tabs.addTab("Custódia atual",listing("Relatório consolidado alimentado pela visão do banco.",()->reports.consolidated(session)));
        if(session.role().writes()){tabs.addTab("Transferir custódia",new TransferPanel(session,new CustodyService(db)));tabs.addTab("Equipe de campanha",new TeamPanel(session,new TeamService(db)));}
        if(session.role()==Role.ADMIN)tabs.addTab("Auditoria",listing("Alterações de artefatos e custódias, com autor e valores anterior/posterior.",()->reports.audit(session)));
        tabs.addChangeListener(e->{
            if(tabs.getSelectedComponent() instanceof CrudPanel panel)panel.reload();
            else if(tabs.getSelectedComponent() instanceof TransferPanel panel)panel.reload();
        });
        add(header,BorderLayout.NORTH);add(tabs,BorderLayout.CENTER);addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){session.close();}});
        SwingUtilities.invokeLater(()->{if(tabs.getSelectedComponent() instanceof CrudPanel panel)panel.reload();});
    }
    private JPanel listing(String description,java.util.concurrent.Callable<java.util.List<java.util.Map<String,Object>>> query){
        JPanel panel=UI.padded(new BorderLayout(10,10));JTable table=UI.table();JPanel top=new JPanel(new BorderLayout());top.add(new JLabel(description),BorderLayout.CENTER);top.add(UI.button("Atualizar","report.refresh",()->UI.work(panel,query,rows->UI.rows(table,rows))),BorderLayout.EAST);panel.add(top,BorderLayout.NORTH);panel.add(new JScrollPane(table),BorderLayout.CENTER);return panel;
    }
}
