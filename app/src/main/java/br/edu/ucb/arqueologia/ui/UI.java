package br.edu.ucb.arqueologia.ui;
import br.edu.ucb.arqueologia.service.Errors;
import javax.swing.*;import javax.swing.table.DefaultTableModel;import java.awt.*;import java.awt.event.*;import java.util.*;import java.util.List;import java.util.concurrent.Callable;import java.util.function.Consumer;
public final class UI {
    private UI(){}
    public record Choice(Object id,String label){@Override public String toString(){return label;}}
    public static JButton button(String label,String name,Runnable action){JButton b=new JButton(label);b.setName(name);b.addActionListener(e->action.run());return b;}
    public static JPanel padded(LayoutManager layout){JPanel p=new JPanel(layout);p.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));return p;}
    public static JComboBox<Choice> choices(List<Map<String,Object>> rows,boolean optional){
        JComboBox<Choice> box=new JComboBox<>();if(optional)box.addItem(new Choice(null,"Não informado"));
        for(var row:rows){var it=row.values().iterator();Object id=it.next();String label=it.hasNext()?String.valueOf(it.next()):String.valueOf(id);box.addItem(new Choice(id,label));}return box;
    }
    public static Integer selectedId(JComboBox<Choice> box){Choice c=(Choice)box.getSelectedItem();return c==null || c.id()==null?null:((Number)c.id()).intValue();}
    public static void select(JComboBox<Choice> box,Object value){for(int i=0;i<box.getItemCount();i++)if(Objects.equals(String.valueOf(box.getItemAt(i).id()),String.valueOf(value))){box.setSelectedIndex(i);return;}}
    public static JTable table(){JTable t=new JTable(new DefaultTableModel());t.setAutoCreateRowSorter(true);t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);t.setRowHeight(28);t.setFillsViewportHeight(true);return t;}
    public static void rows(JTable table,List<Map<String,Object>> rows){rows(table,rows,false);}
    public static void rows(JTable table,List<Map<String,Object>> rows,boolean catalog){
        String[] keys=rows.isEmpty()?new String[]{"Nenhum resultado"}:rows.get(0).keySet().stream().filter(k->!catalog || (!k.startsWith("id_")&&!Set.of("descricao","objetivo").contains(k))).toArray(String[]::new);
        String[] columns=Arrays.stream(keys).map(UI::label).toArray(String[]::new);
        Object[][] data=rows.stream().map(r->Arrays.stream(keys).map(r::get).toArray()).toArray(Object[][]::new);
        DefaultTableModel model=new DefaultTableModel(data,columns){@Override public boolean isCellEditable(int r,int c){return false;}};
        table.setModel(model);
        for(int i=0;i<columns.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(Math.min(330,Math.max(140,columns[i].length()*10)));
    }
    private static String label(String key){
        return switch(key){
            case "tipo_especializacao"->"Especialização";case "registro_profissional"->"Registro profissional";
            case "credencial"->"Autorização / formação";case "detalhe"->"Especialidade";
            case "codigo_catalogo"->"Catálogo";case "codigo"->"Código";case "instituicao"->"Instituição";case "sitio"->"Sítio";
            case "situacao"->"Situação";case "estado_conservacao"->"Conservação";case "orcamento"->"Orçamento";
            case "data_inicio"->"Início";case "data_fim"->"Fim";case "data_descoberta"->"Descoberta";case "periodo_estimado"->"Período estimado";
            case "temperatura_c"->"Temperatura (°C)";case "umidade_percentual"->"Umidade (%)";
            case "profundidade_media_cm"->"Profundidade média (cm)";case "massa_consumida_g"->"Massa consumida (g)";
            case "custo_total"->"Custo total";case "analises"->"Análises";case "local_armazenamento"->"Local";
            default->key.isEmpty()?key:Character.toUpperCase(key.charAt(0))+key.substring(1).replace('_',' ');
        };
    }
    public static void info(Component c,String text){JOptionPane.showMessageDialog(c,text,"Arqueologia",JOptionPane.INFORMATION_MESSAGE);}
    public static <T> void work(Component owner,Callable<T> job,Consumer<T> success){
        JRootPane root=SwingUtilities.getRootPane(owner);
        if(root!=null && Boolean.TRUE.equals(root.getClientProperty("busy")))return;
        if(root!=null){
            root.putClientProperty("busy",true);JPanel glass=new JPanel();glass.setOpaque(false);glass.addMouseListener(new MouseAdapter(){});glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));root.setGlassPane(glass);glass.setVisible(true);
        }
        new SwingWorker<T,Void>(){
            @Override protected T doInBackground()throws Exception{return job.call();}
            @Override protected void done(){
                if(root!=null){root.getGlassPane().setVisible(false);root.putClientProperty("busy",false);}
                try{success.accept(get());}
                catch(Exception ex){JOptionPane.showMessageDialog(owner,Errors.friendly(ex),"Operação não concluída",JOptionPane.WARNING_MESSAGE);}
            }
        }.execute();
    }
    public static String text(Object value){return value==null?"":value.toString();}
}
