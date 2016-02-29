package rdf.parser.shell;


import javax.swing.*;

public class Log {

    private JTextPane textarea;

    public void info(String s) {
        System.out.println("info: "+s);
        if(textarea != null )  textarea.setText(textarea.getText() + "\ninfo: "+s);
    }

    public void error(String s) {
        System.out.println("error: "+s);
        if(textarea != null )  textarea.setText(textarea.getText() + "\nerror: "+s);
    }


    public void errorTextarea(String s) {
        textarea.setText("error: "+s);
    }


    public void setOutTextarea(JTextPane textarea) {
        this.textarea = textarea;
    }

}


