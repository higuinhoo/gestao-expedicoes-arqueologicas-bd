package br.edu.ucb.arqueologia.ui;
import br.edu.ucb.arqueologia.model.Session;import br.edu.ucb.arqueologia.service.CustodyService;
import javax.swing.*;import java.awt.*;import java.time.LocalDate;import java.util.*;
public final class TransferPanel extends JPanel {
    private final Map<String,JComboBox<UI.Choice>> boxes=new HashMap<>();
    public TransferPanel(Session session,CustodyService service){
        super(new BorderLayout());setName("transfer.panel");JPanel form=UI.padded(new GridBagLayout());GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(7,7,7,7);g.fill=GridBagConstraints.HORIZONTAL;
        String[][] labels={{"artifact","Artefato"},{"institution","Instituição de destino"},{"location","Local de destino (opcional)"},{"responsible","Responsável"}};int row=0;
        for(var l:labels){JComboBox<UI.Choice> box=new JComboBox<>();box.setName("transfer."+l[0]);boxes.put(l[0],box);g.gridy=row++;g.gridx=0;form.add(new JLabel(l[1]),g);g.gridx=1;g.weightx=1;form.add(box,g);}
        JTextField date=new JTextField(LocalDate.now().toString(),25),document=new JTextField(25),reason=new JTextField(25);
        JComboBox<String> status=new JComboBox<>(new String[]{"ARMAZENADO","EXPOSTO","EM_ANALISE","EM_CONSERVACAO","CATALOGADO"});
        Object[][] rest={{"Data (AAAA-MM-DD)",date},{"Documento",document},{"Motivo",reason},{"Situação após transferência",status}};
        for(var field:rest){g.gridy=row++;g.gridx=0;g.weightx=0;form.add(new JLabel((String)field[0]),g);g.gridx=1;g.weightx=1;form.add((Component)field[1],g);}
        date.setName("transfer.date");document.setName("transfer.document");reason.setName("transfer.reason");status.setName("transfer.status");
        JCheckBox failure=new JCheckBox("Demonstrar falha intermediária e ROLLBACK");failure.setName("transfer.fail");g.gridy=row++;g.gridx=0;g.gridwidth=2;form.add(failure,g);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(UI.button("Carregar opções","transfer.load",()->UI.work(this,()->service.lookups(session),data->{
            for(var l:labels){var combo=boxes.get(l[0]);combo.removeAllItems();if(l[0].equals("location"))combo.addItem(new UI.Choice(null,"Não informado"));
                for(var r:data.get(l[0])){var it=r.values().iterator();combo.addItem(new UI.Choice(it.next(),String.valueOf(it.next())));}}
        })));
        buttons.add(UI.button("Transferir custódia","transfer.submit",()->{
            try{Integer art=UI.selectedId(boxes.get("artifact")),institution=UI.selectedId(boxes.get("institution")),responsible=UI.selectedId(boxes.get("responsible"));
                if(art==null||institution==null||responsible==null)throw new IllegalArgumentException();
                var t=new CustodyService.Transfer(art,institution,UI.selectedId(boxes.get("location")),responsible,LocalDate.parse(date.getText().trim()),document.getText().trim(),reason.getText().trim(),(String)status.getSelectedItem());boolean fail=failure.isSelected();
                UI.work(this,()->{service.transfer(session,t,fail);return true;},ok->UI.info(this,"Transferência confirmada. Custódia e artefato foram atualizados na mesma transação."));
            }catch(RuntimeException ex){UI.info(this,"Carregue as opções e confira os campos e a data.");}
        }));g.gridy=row;g.gridx=0;g.gridwidth=2;form.add(buttons,g);
        add(new JScrollPane(form),BorderLayout.NORTH);JLabel help=new JLabel("Uma custódia vigente por artefato. O modelo permite um início de custódia por dia.");help.setBorder(BorderFactory.createEmptyBorder(14,20,14,14));add(help,BorderLayout.SOUTH);
    }
}
