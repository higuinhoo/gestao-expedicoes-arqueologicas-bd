package br.edu.ucb.arqueologia;
import br.edu.ucb.arqueologia.db.*;import br.edu.ucb.arqueologia.ui.*;import br.edu.ucb.arqueologia.service.Errors;
import javax.swing.*;import java.awt.Font;
public final class App {
    private App(){}
    public static void main(String[] args){
        SwingUtilities.invokeLater(()->{
            try{
                for(var laf:UIManager.getInstalledLookAndFeels())if(laf.getName().equals("Nimbus")){UIManager.setLookAndFeel(laf.getClassName());break;}
                UIManager.put("defaultFont",new Font("SansSerif",Font.PLAIN,14));
                new LoginFrame(new Database(Config.load())).setVisible(true);
            }catch(Exception ex){JOptionPane.showMessageDialog(null,Errors.friendly(ex),"Inicialização",JOptionPane.ERROR_MESSAGE);}
        });
    }
}
