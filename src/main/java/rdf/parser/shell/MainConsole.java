package rdf.parser.shell;

import gr.seab.r2rml.beans.MainParser;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Properties;

public class MainConsole {


    private static Properties properties = new Properties();
    private static Log log = new Log();



    private static String sDirSeparator = System.getProperty("file.separator");
    private static File currentDir = new File("."); // определяем текущий каталог

    public static String ParserPath = "";
    public static String propertiesFile = "r2rml.properties";





    public static void main(String[] args) {

        // определяем полный путь парсеру и файлу свойств
        try {
            ParserPath = currentDir.getCanonicalPath() + sDirSeparator;// + ParserPath + sDirSeparator;
            propertiesFile = ParserPath + propertiesFile;
        } catch (IOException e) {
            e.printStackTrace();
        }

        LoadProperty(propertiesFile);


        if (args.length > 0) {
            if(args[0] == "createOWL"){  }
            if(args[0] == "createMapFile"){  }
        }else{
            System.err.println("Need Argument");
        }




    }


    public static void CreateMapFile(){

        Database db = new Database();
        db.setProperties(properties);
        db.setLog(log);

        MapGenerator mg = new MapGenerator();
        mg.setDb(db);
        mg.setLog(log);

        String tripletMap =  mg.makeR2RML(properties.getProperty("mapping.file.type"));


        if(tripletMap != null) {

            // Записать в файл
            String file =  ParserPath + properties.getProperty("mapping.file");
            log.info("Write triplet to file " + file);

            try {
                PrintWriter writer = new PrintWriter(file, "UTF-8");
                writer.println(tripletMap);
                writer.close();
            } catch (IOException ex) {
                log.error("Error write map file (" + file + ")." + ex.toString());
            }
        }
    }


    public static void CreateOWL() {
        System.out.println("CreateOWL");

        Database db = new Database();
        db.setProperties(properties);
        db.setLog(log);

        OWLgenerator owlGen = new OWLgenerator();
        owlGen.setDb(db);
        owlGen.setLog(log);
        //String owl =  owlGen.createOWL(properties.getProperty("mapping.file.type"));
        String owl =  owlGen.createOWL("RDF/XML-ABBREV", false);

        // Записать в файл
        String file =  ParserPath + "Ontology.rdf";
        log.info("Write owl to map file " + file);
        try {
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            writer.println(owl);
            writer.close();
        } catch (IOException ex) {
            log.error("Error write map file (" + file + ")." + ex.toString());
        }
    }





    // Вычитать properties для парсера из файла
    public static void LoadProperty(String propertiesFile) {
       // String propertiesFile = path + "r2rml.properties";
        try {
            if (StringUtils.isNotEmpty(propertiesFile)) {
                properties.load(new FileInputStream(propertiesFile));
                log.info("Loaded properties from " + propertiesFile);
            }
        } catch (FileNotFoundException e) {
            log.error("Properties file not found (" + propertiesFile + ").");
            //System.exit(1);
        } catch (IOException e) {
            log.error("Error reading properties file (" + propertiesFile + ").");
            //System.exit(1);
        }
        //properties.list(System.out);
        //updateItems(form);
    }












}
