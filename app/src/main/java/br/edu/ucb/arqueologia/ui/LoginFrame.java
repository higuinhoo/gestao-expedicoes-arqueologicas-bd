package br.edu.ucb.arqueologia.ui;
import br.edu.ucb.arqueologia.db.Database;import br.edu.ucb.arqueologia.service.AuthService;
import javax.swing.*;import java.awt.*;import java.util.Arrays;
public final class LoginFrame extends JFrame {
    public LoginFrame(Database db){
        super("Acesso — Acervo arqueológico");setName("login.frame");setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel panel=UI.padded(new GridBagLayout());GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(10,10,10,10);g.fill=GridBagConstraints.HORIZONTAL;
        JLabel title=new JLabel("Acervo Arqueológico");title.setFont(title.getFont().deriveFont(Font.BOLD,24f));g.gridx=0;g.gridy=0;g.gridwidth=2;panel.add(title,g);
        g.gridy=1;panel.add(new JLabel("Entre com sua conta individual do projeto."),g);g.gridwidth=1;
        JTextField user=new JTextField(24);user.setName("login.user");JPasswordField password=new JPasswordField(24);password.setName("login.password");
        g.gridy=2;g.gridx=0;panel.add(new JLabel("Usuário"),g);g.gridx=1;panel.add(user,g);g.gridy=3;g.gridx=0;panel.add(new JLabel("Senha"),g);g.gridx=1;panel.add(password,g);
        JButton login=UI.button("Entrar","login.submit",()->{String username=user.getText().trim();char[] secret=password.getPassword();password.setText("");
            UI.work(this,()->{try{return new AuthService(db).login(username,secret);}finally{Arrays.fill(secret,'\0');}},session->{setDefaultCloseOperation(DISPOSE_ON_CLOSE);dispose();new MainFrame(db,session,()->new LoginFrame(db).setVisible(true)).setVisible(true);});
        });
        g.gridy=4;g.gridx=1;panel.add(login,g);getRootPane().setDefaultButton(login);add(panel);pack();setLocationRelativeTo(null);
    }
}
