package rdf.parser.shell;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;

/**
 * Created by Женя on 05.03.2016.
 */
public class JenaFormatConvertor {


    private static Log log;
    private static Properties properties;


    Model m = ModelFactory.createDefaultModel();


    public static void run(String path, String from, String to, String ns ) {

        Model m;
        //String ns = "";

        InputStream isMap = FileManager.get().open(path+"conv/original.txt");
        m=ModelFactory.createDefaultModel();
        try{
            m.read(isMap, ns, from);
        }
        catch(Exception e){
            String err = "Error reading mapping file "+path+"conv/original.txt " + e.toString();
            log.error(err);
            throw new RuntimeException(err);
            //System.exit(1);
        }


        StringWriter out = new StringWriter();
        m.write (out, to, ns);

        System.out.println(out.toString());

        //log.info(out.toString());
    }




    public void setLog(Log log) {
        this.log = log;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }


}
