package rdf.parser.shell;

import gr.seab.r2rml.beans.MainParser;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Properties;
import java.util.UUID;

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
                owlGen.setProperties(properties);
                //res =  owlGen.createOWL(args[1], false);
                res =  owlGen.createOWL(false);
            }


            if(args[0].equals("createMapFile")){
                file =  ParserPath + "result/MappingFile";
                MapGenerator mg = new MapGenerator();
                mg.setDb(db);
                mg.setLog(log);
                mg.setProperties(properties);
                res =  mg.makeR2RML();
            }

            if(args[0].equals("ont2d3")){
                try {
                    file =  ParserPath + "result/"+args[1];
                    String ontFile =  ParserPath + "upload/ont2d3.owl";
                    Owl2d3  o2d3 = new Owl2d3();

                    String uniqueID = "ont_"+ UUID.randomUUID().toString().replaceAll("-","_");

                    res = o2d3.MakeD3Json( uniqueID,  o2d3.ReadModelFromFile(ontFile, ""));
                }catch (RuntimeException e){
                    log.error(e.toString());
                }
            }


            // Записать в файл
            if(res != "") {
                Util.StringToFile(file, res);
            }else{
                log.info("Result is NULL !!!");
            }


            if(args[0].equals("RunParser")){
                try {
                    String prop[] = {"fixProperty"};
                    MainParser.runParcer(prop);
                }catch (RuntimeException e){
                    log.error(e.toString());
                }
            }

            if(args[0].equals("FormatConverter")){
                try {
                    //String prop[] = {"fixProperty"};
                    JenaFormatConvertor conv = new JenaFormatConvertor();
                    conv.setLog(log);
                    conv.run(ParserPath, args[1], args[2], args[3]);
                }catch (RuntimeException e){
                    log.error(e.toString());
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
                properties = fixProperty(properties);
            }
        } catch (FileNotFoundException e) {
            log.error("Properties file not found (" + propertiesFile + ").");
            //System.exit(1);
        } catch (IOException e) {
            log.error("Error reading properties file (" + propertiesFile + ").");
            //System.exit(1);
        }
    }


    // Установить критичные настройки
    public static Properties fixProperty(Properties properties) {

            properties.setProperty("mapping.file",          "result/MappingFile");
            //properties.setProperty("mapping.file.type",     form.mappingFileType.getSelectedItem().toString());
            //properties.setProperty("default.namespace",     form.defaultNamespace.getText());
            properties.setProperty("default.log",           "result/status.rdf");
            //properties.setProperty("default.verbose",       Boolean.toString(form.defaultVerbose.isSelected()));
            //properties.setProperty("default.incremental",   Boolean.toString(form.defaultIncremental.isSelected()));
            properties.setProperty("input.model",           "dspace/edm-empty.rdf");
            //properties.setProperty("input.model.type",      form.inputModelType.getSelectedItem().toString());
            //properties.setProperty("db.driver",             form.dbDriver.getText());
            //properties.setProperty("db.url",                form.dbUrl.getText());
            //properties.setProperty("db.login",              form.dbLogin.getText());
            //properties.setProperty("db.password",           form.dbPassword.getText());
            //properties.setProperty("jena.storeOutputModelUsingTdb", Boolean.toString(form.jenaStoreOutputModelUsingTdb.isSelected()));
            //properties.setProperty("jena.cleanTdbOnStartup",        Boolean.toString(form.jenaCleanTdbOnStartup.isSelected()));
            properties.setProperty("jena.tdb.directory",         "result/tdb");
            properties.setProperty("jena.destinationFileName",      "result/parser_result.rdf");
            //properties.setProperty("jena.destinationFileSyntax",    form.jenaDestinationFileSyntax.getSelectedItem().toString());

        return properties;

    }




}
