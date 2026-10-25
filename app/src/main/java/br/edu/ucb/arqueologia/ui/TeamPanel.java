package br.edu.ucb.arqueologia.ui;
import br.edu.ucb.arqueologia.model.Session;import br.edu.ucb.arqueologia.service.TeamService;
import javax.swing.*;import java.awt.*;import java.time.LocalDate;import java.util.*;
public final class TeamPanel extends JPanel {
    private final JTable table=UI.table();
    private java.util.List<Map<String,Object>> data=java.util.List.of();
    public TeamPanel(Session session,TeamService service){
        super(new BorderLayout(10,10));setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        JComboBox<UI.Choice> campaigns=new JComboBox<>(),people=new JComboBox<>();JTextField function=new JTextField(12),entry=new JTextField(LocalDate.now().toString(),10),hours=new JTextField("40",4);
        JPanel form=new JPanel();form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));
        JPanel selection=new JPanel(new FlowLayout(FlowLayout.LEFT));campaigns.setPreferredSize(new Dimension(230,28));people.setPreferredSize(new Dimension(300,28));
        selection.add(new JLabel("Campanha"));selection.add(campaigns);selection.add(new JLabel("Pesquisador ativo"));selection.add(people);form.add(selection);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));actions.add(new JLabel("Função"));actions.add(function);actions.add(new JLabel("Entrada"));actions.add(entry);actions.add(new JLabel("Horas"));actions.add(hours);form.add(actions);
        Runnable reload=()->UI.work(this,()->service.list(session),rows->{data=rows;UI.rows(table,rows);});
        actions.add(UI.button("Carregar","team.load",()->UI.work(this,()->service.lookups(session),refs->{
            campaigns.removeAllItems();people.removeAllItems();for(var r:refs.get("campaign")){var it=r.values().iterator();campaigns.addItem(new UI.Choice(it.next(),String.valueOf(it.next())));}for(var r:refs.get("researcher")){var it=r.values().iterator();people.addItem(new UI.Choice(it.next(),String.valueOf(it.next())));}reload.run();
        })));
        actions.add(UI.button("Adicionar","team.add",()->{
            try{Integer c=UI.selectedId(campaigns),p=UI.selectedId(people);LocalDate d=LocalDate.parse(entry.getText());int h=Integer.parseInt(hours.getText());if(c==null||p==null)throw new IllegalArgumentException();String f=function.getText().trim();UI.work(this,()->{service.add(session,c,p,f,d,null,h);return true;},ok->reload.run());}
            catch(RuntimeException ex){UI.info(this,"Carregue as opções e confira entrada, função e horas.");}
        }));
        actions.add(UI.button("Remover participação","team.delete",()->{
            int selected=table.getSelectedRow();if(selected<0||data.isEmpty()){UI.info(this,"Selecione uma participação.");return;}var r=data.get(table.convertRowIndexToModel(selected));
            if(JOptionPane.showConfirmDialog(this,"Remover a participação selecionada?","Confirmar",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)UI.work(this,()->{service.remove(session,((Number)r.get("id_campanha")).intValue(),((Number)r.get("id_pesquisador")).intValue());return true;},ok->reload.run());
        }));
        add(form,BorderLayout.NORTH);add(new JScrollPane(table),BorderLayout.CENTER);
    }
}
