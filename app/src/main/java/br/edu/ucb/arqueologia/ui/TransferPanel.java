package br.edu.ucb.arqueologia.ui;
import br.edu.ucb.arqueologia.model.Session;import br.edu.ucb.arqueologia.service.CustodyService;
import javax.swing.*;import java.awt.*;import java.time.LocalDate;import java.util.*;
public final class TransferPanel extends JPanel {
    private final Map<String,JComboBox<UI.Choice>> boxes=new HashMap<>();
    private Runnable loadOptions;
    private java.util.List<Map<String,Object>> locations=java.util.List.of();
    private boolean loading;
    public TransferPanel(Session session,CustodyService service){
        super(new BorderLayout());setName("transfer.panel");JPanel form=UI.padded(new GridBagLayout());GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(7,7,7,7);g.fill=GridBagConstraints.HORIZONTAL;
        String[][] labels={{"artifact","Artefato"},{"institution","Instituição de destino"},{"location","Local de destino (opcional)"},{"responsible","Responsável"}};int row=0;
        for(var l:labels){JComboBox<UI.Choice> box=new JComboBox<>();box.setName("transfer."+l[0]);box.setEnabled(false);box.setMaximumRowCount(12);box.setPreferredSize(new Dimension(400,28));box.setToolTipText("Selecione um registro já cadastrado na lista.");boxes.put(l[0],box);g.gridy=row++;g.gridx=0;g.weightx=0;form.add(new JLabel(l[1]),g);g.gridx=1;g.weightx=1;form.add(box,g);}
        boxes.get("institution").addActionListener(e->{if(!loading)filterLocations();});
        JTextField date=new JTextField(LocalDate.now().toString(),25),document=new JTextField(25),reason=new JTextField(25);
        JComboBox<String> status=new JComboBox<>(new String[]{"ARMAZENADO","EXPOSTO","EM_ANALISE","EM_CONSERVACAO","CATALOGADO"});
        Object[][] rest={{"Data (AAAA-MM-DD)",date},{"Documento",document},{"Motivo",reason},{"Situação após transferência",status}};
        for(var field:rest){g.gridy=row++;g.gridx=0;g.weightx=0;form.add(new JLabel((String)field[0]),g);g.gridx=1;g.weightx=1;form.add((Component)field[1],g);}
        date.setName("transfer.date");document.setName("transfer.document");reason.setName("transfer.reason");status.setName("transfer.status");
        JCheckBox failure=new JCheckBox("Demonstrar falha intermediária e ROLLBACK");failure.setName("transfer.fail");g.gridy=row++;g.gridx=0;g.gridwidth=2;form.add(failure,g);
        JLabel optionsStatus=new JLabel("As opções serão carregadas ao abrir esta aba.");optionsStatus.setName("transfer.help");
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submit=UI.button("Transferir custódia","transfer.submit",()->{
            try{Integer art=UI.selectedId(boxes.get("artifact")),institution=UI.selectedId(boxes.get("institution")),responsible=UI.selectedId(boxes.get("responsible"));
                if(art==null||institution==null||responsible==null)throw new IllegalArgumentException("Selecione o artefato, a instituição de destino e o responsável.");
                if(document.getText().isBlank()||document.getText().trim().length()>40)throw new IllegalArgumentException("Informe o documento de movimentação, com até 40 caracteres.");
                if(reason.getText().isBlank()||reason.getText().trim().length()>200)throw new IllegalArgumentException("Informe o motivo da transferência, com até 200 caracteres.");
                var t=new CustodyService.Transfer(art,institution,UI.selectedId(boxes.get("location")),responsible,LocalDate.parse(date.getText().trim()),document.getText().trim(),reason.getText().trim(),(String)status.getSelectedItem());boolean fail=failure.isSelected();
                UI.work(this,()->{service.transfer(session,t,fail);return true;},ok->UI.info(this,"Transferência confirmada. Custódia e artefato foram atualizados na mesma transação."));
            }catch(java.time.format.DateTimeParseException ex){UI.info(this,"Informe uma data válida no formato AAAA-MM-DD, por exemplo 2026-10-25.");date.requestFocusInWindow();}
            catch(IllegalArgumentException ex){UI.info(this,ex.getMessage());}
        });
        submit.setEnabled(false);
        loadOptions=()->UI.work(this,()->service.lookups(session),data->{
            loading=true;
            try{
                for(var l:labels){
                    if(l[0].equals("location"))continue;
                    var combo=boxes.get(l[0]);Integer selected=UI.selectedId(combo);combo.removeAllItems();
                    for(var r:data.get(l[0])){var it=r.values().iterator();combo.addItem(new UI.Choice(it.next(),String.valueOf(it.next())));}
                    if(selected!=null)UI.select(combo,selected);combo.setEnabled(combo.getItemCount()>0);
                }
                locations=data.get("location");filterLocations();
                boolean ready=java.util.List.of("artifact","institution","responsible").stream().allMatch(key->boxes.get(key).getItemCount()>0);
                submit.setEnabled(ready);optionsStatus.setText(ready?"Opções carregadas. Os locais exibidos pertencem à instituição de destino.":"Faltam registros cadastrados. Cadastre-os e clique em Atualizar opções.");
            }finally{loading=false;}
        });
        buttons.add(UI.button("Atualizar opções","transfer.load",this::reload));buttons.add(submit);
        g.gridy=row;g.gridx=0;g.gridwidth=2;form.add(buttons,g);
        JPanel content=new JPanel(new BorderLayout());content.add(form,BorderLayout.NORTH);add(new JScrollPane(content),BorderLayout.CENTER);
        JPanel guidance=UI.padded(new GridLayout(0,1,0,5));guidance.add(new JLabel("Selecione os registros nas listas; digite a data, o documento e o motivo."));guidance.add(optionsStatus);
        guidance.add(new JLabel("Uma custódia vigente por artefato. A data deve ser posterior ao início da custódia atual."));add(guidance,BorderLayout.SOUTH);
    }
    public void reload(){loadOptions.run();}
    private void filterLocations(){
        var box=boxes.get("location");Integer selected=UI.selectedId(box),institution=UI.selectedId(boxes.get("institution"));
        box.removeAllItems();box.addItem(new UI.Choice(null,"Não informado"));
        for(var location:locations)if(institution!=null && institution.equals(((Number)location.get("id_instituicao")).intValue()))box.addItem(new UI.Choice(location.get("id_local"),String.valueOf(location.get("local"))));
        if(selected!=null)UI.select(box,selected);box.setEnabled(institution!=null);
    }
}
