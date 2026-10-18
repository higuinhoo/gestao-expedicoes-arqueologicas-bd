package br.edu.ucb.arqueologia.ui;
import br.edu.ucb.arqueologia.model.*;import br.edu.ucb.arqueologia.service.CatalogService;
import javax.swing.*;import java.awt.*;import java.util.*;import java.util.List;
public final class CrudPanel extends JPanel {
    private final Session session;private final CatalogService service;private final EntitySpec spec;
    private final JTable table=UI.table();private final JTextField search=new JTextField(24);private final JLabel status=new JLabel("Pesquise ou atualize para carregar até 500 registros.");
    private List<Map<String,Object>> data=List.of();
    public CrudPanel(Session session,CatalogService service,EntitySpec spec){
        super(new BorderLayout(10,10));this.session=session;this.service=service;this.spec=spec;setName("crud."+spec.name());setBorder(BorderFactory.createEmptyBorder(14,14,14,14));table.setName("table."+spec.name());search.setName("search."+spec.name());
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));actions.add(new JLabel("Buscar:"));actions.add(search);
        actions.add(UI.button("Pesquisar","refresh."+spec.name(),this::reload));
        actions.add(UI.button("Novo","new."+spec.name(),()->edit(null)));
        actions.add(UI.button("Editar","edit."+spec.name(),()->{var r=selected();if(r!=null)edit(r);}));
        actions.add(UI.button("Remover","delete."+spec.name(),()->{
            var r=selected();if(r!=null && JOptionPane.showConfirmDialog(this,"Remover o registro selecionado?","Confirmar remoção",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                UI.work(this,()->{service.delete(session,spec,((Number)r.get(spec.pk)).intValue());return true;},ok->reload());
        }));
        search.addActionListener(e->reload());add(actions,BorderLayout.NORTH);add(new JScrollPane(table),BorderLayout.CENTER);add(status,BorderLayout.SOUTH);
    }
    private Map<String,Object> selected(){int row=table.getSelectedRow();if(row<0 || data.isEmpty()){UI.info(this,"Selecione um registro na tabela.");return null;}return data.get(table.convertRowIndexToModel(row));}
    private void edit(Map<String,Object> current){UI.work(this,()->service.lookups(session,spec),refs->new RecordDialog(SwingUtilities.getWindowAncestor(this),session,service,spec,current,refs,this::reload).setVisible(true));}
    public void reload(){String filter=search.getText().trim();UI.work(this,()->service.list(session,spec,filter),rows->{data=rows;UI.rows(table,rows,true);status.setText(rows.size()+" registro(s). Se houver mais de 500, refine a busca.");});}
}
