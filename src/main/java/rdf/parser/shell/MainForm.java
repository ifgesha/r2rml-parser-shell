package rdf.parser.shell;

import sun.applet.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Created by Женя on 05.01.2016.
 */
public class MainForm extends JFrame{
    private JPanel rootPanel;
    public JComboBox mappingFileType;
    public JTextField defaultNamespace;
    public JCheckBox defaultVerbose;
    public JTextField mappingFile;
    public JCheckBox defaultIncremental;
    public JTextField defaultLog;
    private JTabbedPane tabbedPane1;
    private JPanel General;
    private JPanel InputModel;
    public JTextField inputModel;
    public JComboBox inputModelType;
    private JPanel DatabaseConnectivity;
    public JPasswordField dbPassword;
    public JTextField dbUrl;
    public JTextField dbLogin;
    public JTextField dbDriver;
    private JPanel Jena1;
    private JPanel Jena2;
    public JCheckBox jenaStoreOutputModelUsingTdb;
    public JCheckBox jenaCleanTdbOnStartup;
    public JTextField jenaTdbDirectory;
    public JComboBox jenaDestinationFileSyntax;
    public JTextField jenaDestinationFileName;
    private JButton saveProperty;
    private JButton DBTestConnectButton;
    public JTextPane textPane1;
    public JScrollPane LogPanel;
    private JButton parseDBButton;


    public MainForm(){

        super("R2RML ParserShell");
        setLocationRelativeTo(null);
        setContentPane(rootPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // чтобы процесс завершался после закрытия окна
        pack(); // автоматически настраиваем размер окна под содержимое
        setVisible(true); // отображаем окно

        setSize(750,600); // Размер окна

        // По центру экрана
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - getHeight()) / 2);
        setLocation(x, y);




        saveProperty.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                System.out.println("click saveProperty");
               Main.saveProperty(Main.propertiesFile);
            }
        });




        DBTestConnectButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                System.out.println("click DBTestConnectButton");
                Main.CreateMapFile();
            }
        });

        parseDBButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                System.out.println("click ParseDB");
                Main.ParceDB();
            }
        });

    }





}

