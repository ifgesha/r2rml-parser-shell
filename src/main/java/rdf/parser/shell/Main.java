package rdf.parser.shell;

import org.apache.commons.lang.StringUtils;
import gr.seab.r2rml.beans.MainParser;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class Main {


    private static Properties properties = new Properties();
    private static Log log = new Log();
    private static MainForm form = new MainForm();


    private static String sDirSeparator = System.getProperty("file.separator");
    private static File currentDir = new File("."); // определяем текущий каталог

    public static String ParserPath = "";
    public static String propertiesFile = "r2rml.properties";





    public static void main(String[] args) {
        log.setOutTextarea(form.textPane1);

        // определяем полный путь парсеру и файлу свойств
        try {
            ParserPath = currentDir.getCanonicalPath() + sDirSeparator;// + ParserPath + sDirSeparator;
            propertiesFile = ParserPath + propertiesFile;
        } catch (IOException e) {
            e.printStackTrace();
        }

        LoadProperty(propertiesFile);


    }


    public static void CreateMapFile(){

        Database db = new Database();
        db.setProperties(properties);
        db.setLog(log);

        MapGenerator mg = new MapGenerator();
        mg.setDb(db);
        mg.setLog(log);
        mg.setProperties(properties);

        String tripletMap =  mg.makeR2RML();


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
        owlGen.setProperties(properties);

        String owl =  owlGen.createOWL(true);

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


    public static void readTDB() {
        System.out.println("CreateOWL");
        StoreTDB  tdb = new StoreTDB();
        tdb.setProperties(properties);
        tdb.setLog(log);


        // Получить список именованых моделей
        List<String> modelName = tdb.getNamedModelList();
        for(Iterator<String> i = modelName.iterator(); i.hasNext(); ) {
            String mName = i.next();
            tdb.getOwlClasses(mName);
        }
        //tdb.getOwlClasses(""); // Список классов модели DefaultModel
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
        updateItems(form);
    }

    // Обновить значения в полях формы
    public static void updateItems (MainForm form){


        form.nsOWL.setText( properties.getProperty("gen.ns.owl"));
        form.nsMap.setText( properties.getProperty("gen.ns.map"));
        form.nsMapEX.setText( properties.getProperty("gen.ns.map.ex"));
        form.generatorFileType.setSelectedItem(properties.getProperty("gen.file.type"));

        form.mappingFile.setText( properties.getProperty("mapping.file"));
        form.mappingFileType.setSelectedItem(properties.getProperty("mapping.file.type"));
        form.defaultNamespace.setText( properties.getProperty("default.namespace"));
        form.defaultLog.setText(properties.getProperty("default.log"));
        if(properties.getProperty("default.verbose").equals("true")){
            form.defaultVerbose.setSelected(true);
        }
        if(properties.getProperty("default.incremental").equals("true")){
            form.defaultIncremental.setSelected(true);
        }

        form.inputModel.setText( properties.getProperty("input.model"));
        form.inputModelType.setSelectedItem(properties.getProperty("input.model.type"));

        form.dbDriver.setText( properties.getProperty("db.driver"));
        form.dbUrl.setText( properties.getProperty("db.url"));
        form.dbLogin.setText( properties.getProperty("db.login"));
        form.dbPassword.setText( properties.getProperty("db.password"));

        if(properties.getProperty("jena.storeOutputModelUsingTdb").equals("true")){
            form.jenaStoreOutputModelUsingTdb.setSelected(true);
        }
        if(properties.getProperty("jena.cleanTdbOnStartup").equals("true")){
            form.jenaCleanTdbOnStartup.setSelected(true);
        }
        form.jenaTdbDirectory.setText( properties.getProperty("jena.tdb.directory"));

        form.jenaDestinationFileName.setText( properties.getProperty("jena.destinationFileName"));
        form.jenaDestinationFileSyntax.setSelectedItem(properties.getProperty("jena.destinationFileSyntax"));

    }




    // Сохранить изменения в файле .propertys
    public static void saveProperty(String propertiesFile) {
        //String propertiesFile = path + "r2rml.properties";
        try {

            properties.setProperty("gen.file.type",          form.generatorFileType.getSelectedItem().toString());
            properties.setProperty("gen.ns.owl",             form.nsOWL.getText());
            properties.setProperty("gen.ns.map",             form.nsMap.getText());
            properties.setProperty("gen.ns.map.ex",          form.nsMapEX.getText());

            properties.setProperty("mapping.file",          form.mappingFile.getText());
            properties.setProperty("mapping.file.type",     form.mappingFileType.getSelectedItem().toString());
            properties.setProperty("default.namespace",     form.defaultNamespace.getText());
            properties.setProperty("default.log",           form.defaultLog.getText());
            properties.setProperty("default.verbose",       Boolean.toString(form.defaultVerbose.isSelected()));
            properties.setProperty("default.incremental",   Boolean.toString(form.defaultIncremental.isSelected()));
            properties.setProperty("input.model",           form.inputModel.getText());
            properties.setProperty("input.model.type",      form.inputModelType.getSelectedItem().toString());
            properties.setProperty("db.driver",             form.dbDriver.getText());
            properties.setProperty("db.url",                form.dbUrl.getText());
            properties.setProperty("db.login",              form.dbLogin.getText());
            properties.setProperty("db.password",           form.dbPassword.getText());
            properties.setProperty("jena.storeOutputModelUsingTdb", Boolean.toString(form.jenaStoreOutputModelUsingTdb.isSelected()));
            properties.setProperty("jena.cleanTdbOnStartup",        Boolean.toString(form.jenaCleanTdbOnStartup.isSelected()));
            properties.setProperty("jena.tdb.directory",         form.jenaTdbDirectory.getText());
            properties.setProperty("jena.destinationFileName",      form.jenaDestinationFileName.getText());
            properties.setProperty("jena.destinationFileSyntax",    form.jenaDestinationFileSyntax.getSelectedItem().toString());

            FileOutputStream  out = new FileOutputStream(propertiesFile);
            properties.store(out, null);
            out.close();
        }
        catch (Exception e ) {
            e.printStackTrace();
        }

    }


    public static void ParceDB(){
        try {
            MainParser.runParcer(null);
        }catch (RuntimeException e){
            log.error(e.toString());
            //e.printStackTrace();
        }
    }



    // запустить в консоле
    private static void runProcess(String command) throws Exception {
        Process pro = Runtime.getRuntime().exec(command);
        printLines(command + " stdout:", pro.getInputStream());
        printLines(command + " stderr:", pro.getErrorStream());
        pro.waitFor();
        System.out.println(command + " exitValue() " + pro.exitValue());
    }


    // Вывод
    private static void printLines(String name, InputStream ins) throws Exception {
        String line = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(ins));
        while ((line = in.readLine()) != null) {
            System.out.println(name + " " + line);
        }
    }



}
