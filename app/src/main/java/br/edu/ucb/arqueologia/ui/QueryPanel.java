package br.edu.ucb.arqueologia.ui;
import br.edu.ucb.arqueologia.model.Session;import br.edu.ucb.arqueologia.service.ReportService;
import javax.swing.*;import java.awt.*;import java.time.LocalDate;
public final class QueryPanel extends JPanel {
    private final JTable table=UI.table();
    public QueryPanel(Session session,ReportService service,boolean analyses){
        super(new BorderLayout(10,10));setBorder(BorderFactory.createEmptyBorder(14,14,14,14));setName(analyses?"query.analises":"query.acervo");
        JComboBox<UI.Choice> id=new JComboBox<>();id.addItem(new UI.Choice(null,"Todas"));id.setPreferredSize(new Dimension(180,28));JTextField filter=new JTextField(14),start=new JTextField("2025-01-01",10),end=new JTextField("2027-12-31",10);
        id.setName(analyses?"query.lab":"query.campaign");filter.setName(analyses?"query.status":"query.material");
        JPanel form=new JPanel(new FlowLayout(FlowLayout.LEFT));form.add(new JLabel(analyses?"Laboratório":"Campanha"));form.add(id);
        form.add(new JLabel(analyses?"Situação (opcional)":"Material (opcional)"));form.add(filter);form.add(new JLabel("De"));form.add(start);form.add(new JLabel("até"));form.add(end);
        form.add(UI.button("Carregar filtros",analyses?"query.filters.analises":"query.filters.acervo",()->UI.work(this,()->service.filters(session,analyses),rows->{id.removeAllItems();id.addItem(new UI.Choice(null,"Todas"));for(var r:rows){var it=r.values().iterator();id.addItem(new UI.Choice(it.next(),String.valueOf(it.next())));}})));
        form.add(UI.button("Consultar",analyses?"query.run.analises":"query.run.acervo",()->{
            try{Integer selected=UI.selectedId(id);String value=filter.getText().trim();LocalDate a=LocalDate.parse(start.getText().trim()),b=LocalDate.parse(end.getText().trim());
                UI.work(this,()->analyses?service.analises(session,selected,value,a,b):service.acervo(session,selected,value,a,b),rows->UI.rows(table,rows));
            }catch(RuntimeException ex){UI.info(this,"Confira as datas no formato AAAA-MM-DD.");}
        }));
        add(form,BorderLayout.NORTH);add(new JScrollPane(table),BorderLayout.CENTER);
        add(new JLabel(analyses?"Totais por laboratório; cada custo é contado uma vez por análise.":"Totais por campanha, material e estado de conservação."),BorderLayout.SOUTH);
    }
}
