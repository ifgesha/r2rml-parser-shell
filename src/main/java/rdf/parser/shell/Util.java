package rdf.parser.shell;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by Женя on 22.03.2016.
 */
public class Util {

    private static final Logger log = LoggerFactory.getLogger(Util.class);



    // Записать в файл
    public static Boolean StringToFile(String FileName, String data) {

        log.info("Write file " + FileName);
        try {
            PrintWriter writer = new PrintWriter(FileName, "UTF-8");
            writer.println(data);
            writer.close();
        } catch (IOException ex) {
            log.error("Error write  file (" + FileName + ")." + ex.toString());
            return false;
        }
        return true;
    }


}
