package br.edu.ucb.arqueologia.ui;
import br.edu.ucb.arqueologia.model.*;import br.edu.ucb.arqueologia.service.CatalogService;
import javax.swing.*;import java.awt.*;import java.util.*;import java.util.List;
public final class RecordDialog extends JDialog {
    private final Map<String,JComponent> inputs=new LinkedHashMap<>();
    public RecordDialog(Window owner,Session session,CatalogService service,EntitySpec spec,Map<String,Object> current,Map<String,List<Map<String,Object>>> references,Runnable saved){
        super(owner,(current==null?"Novo — ":"Editar — ")+spec.title,ModalityType.APPLICATION_MODAL);setName("record.dialog");
        JPanel form=UI.padded(new GridBagLayout());GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(5,5,5,5);g.fill=GridBagConstraints.HORIZONTAL;
        int row=0;
        for(var field:spec.fields){
            JComponent input;Object value=current==null?null:current.get(field.key());
            if(field.lookup()!=null){var combo=UI.choices(references.get(field.key()),!field.required());UI.select(combo,value);input=combo;}
            else if(field.options()!=null){JComboBox<String> combo=new JComboBox<>(field.options().split(","));if(value!=null)combo.setSelectedItem(value.toString());input=combo;}
            else {JTextField text=new JTextField(UI.text(value),32);input=text;}
            input.setName("field."+field.key());inputs.put(field.key(),input);
            g.gridx=0;g.gridy=row;g.weightx=0;form.add(new JLabel(field.label()+(field.required()?" *":"")),g);
            g.gridx=1;g.weightx=1;form.add(input,g);row++;
        }
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(UI.button("Cancelar","record.cancel",this::dispose));
        buttons.add(UI.button("Salvar","record.save",()->{
            try{
                Map<String,Object> values=new LinkedHashMap<>();
                for(var f:spec.fields){JComponent input=inputs.get(f.key());Object raw=input instanceof JTextField t?t.getText():((JComboBox<?>)input).getSelectedItem();
                    if(raw instanceof UI.Choice choice)raw=choice.id();values.put(f.key(),f.parse(raw==null?"":raw.toString()));}
                Integer id=current==null?null:((Number)current.get(spec.pk)).intValue();
                UI.work(this,()->service.save(session,spec,id,values),result->{dispose();saved.run();});
            }catch(IllegalArgumentException ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Confira os campos",JOptionPane.WARNING_MESSAGE);}
        }));
        setLayout(new BorderLayout());add(new JScrollPane(form),BorderLayout.CENTER);add(buttons,BorderLayout.SOUTH);
        setSize(780,Math.min(720,190+row*46));setLocationRelativeTo(owner);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
}
