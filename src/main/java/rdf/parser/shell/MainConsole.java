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

            String res = "";
            String file = "";

            Database db = new Database();
            db.setProperties(properties);
            db.setLog(log);


            if(args[0].equals("createOWL")){
                file =  ParserPath + "result/Ontology.rdf";

                OWLgenerator owlGen = new OWLgenerator();
                owlGen.setDb(db);
                owlGen.setLog(log);
                res =  owlGen.createOWL(args[1], false);
            }


            if(args[0].equals("createMapFile")){
                //file = ParserPath + properties.getProperty("mapping.file");
                file =  ParserPath + "result/MappingFile";
                MapGenerator mg = new MapGenerator();
                mg.setDb(db);
                mg.setLog(log);
                res =  mg.makeR2RML(args[1]);
            }


            if(res != "") {
                // Записать в файл
                log.info("Write to file " + file);
                try {
                    PrintWriter writer = new PrintWriter(file, "UTF-8");
                    writer.println(res);
                    writer.close();
                } catch (IOException ex) {
                    log.error("Error write map file (" + file + ")." + ex.toString());
                }
            }




        }else{
            System.err.println("Need Argument");
        }




    }




    // Вычитать properties для парсера из файла
    public static void LoadProperty(String propertiesFile) {
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
    }






}