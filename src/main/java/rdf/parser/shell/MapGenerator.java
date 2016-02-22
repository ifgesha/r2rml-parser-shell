package rdf.parser.shell;


import com.hp.hpl.jena.rdf.model.*;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.*;


public class MapGenerator {

    private Database db;
    private static Log log;


    private final String ns = "http://example.com/";
    private final String exNs = "http://example.com/";

    private final String rrNs = "http://www.w3.org/ns/r2rml#";
    private final String foafNs = "http://xmlns.com/foaf/0.1/";
    private final String xsdNs = "http://www.w3.org/2001/XMLSchema#";
    private final String rdfsNs = "http://www.w3.org/2000/01/rdf-schema#";








    Model m = ModelFactory.createDefaultModel();





    public String makeR2RML(String outFormat){


        /*
            Для тестов - Вычитывает R2RML файл в одном формате а затем конвертирует в другой

                Model mapModel;
                String baseNs = ns;

                InputStream isMap = FileManager.get().open(path);
                mapModel = ModelFactory.createDefaultModel();
                try {
                    mapModel.read(isMap, baseNs, "TURTLE");
                } catch (Exception e) {
                    String err = "Error reading mapping file " + e.toString();
                    log.error(err);
                    throw new RuntimeException(err);
                    //System.exit(1);
                }

                StringWriter out1 = new StringWriter();
                mapModel.write (out1, outFormat, ns);

                log.info(out1.toString());

        */

        // Префиксы
        m.setNsPrefix("rr", rrNs);
        m.setNsPrefix("foaf", foafNs);
        m.setNsPrefix("xsd", xsdNs);
        m.setNsPrefix("rdfs", rdfsNs);
        m.setNsPrefix("ex", exNs);

        // Подготовим свойства
        Property propTableName = m.createProperty(rrNs +"tableName");
        Property propLogicalTable = m.createProperty(rrNs +"logicalTable");
        Property propSubjectMap = m.createProperty(rrNs +"subjectMap");
        Property propGraph = m.createProperty(rrNs +"graph");
        Property propClass = m.createProperty(rrNs +"class");
        Property propTemplate = m.createProperty(rrNs +"template");
        Property propPredicateObjectMap = m.createProperty(rrNs +"predicateObjectMap");
        Property propObjectMap = m.createProperty(rrNs +"objectMap");
        Property propColumn = m.createProperty(rrNs +"column");
        Property propPredicate = m.createProperty(rrNs +"predicate");



        try{
                // Имя текущей базы
                ResultSet result =  db.query("SELECT DATABASE()");
                result.next();
                String dbName = result.getString(1);

                // Выбор information_schema там хранятся все данные по базам, таблицам и т.д.
                db.query("USE `information_schema`");

                // Получить список таблиц
                result = db.query("SELECT TABLE_NAME FROM `TABLES` WHERE  TABLE_SCHEMA = '"+dbName+"' #limit 1" );
                while(result.next()) {
                    String tableName = result.getString(1);
                    log.info("Tables "+dbName +"." + tableName);


                    ArrayList<String> ExistsResurs = new ArrayList<String>();

                    // Создаём TriplesMap
                    Resource res = m.createResource("#TriplesMap_"+tableName ,m.createResource(rrNs +"TriplesMap"));


                    // Добавим узел LogicalTable в TriplesMap
                    res.addProperty(propLogicalTable,
                            m.createResource().addProperty(propTableName, tableName)
                    );


                    // Primary key  -------------------------------------------------
                    String q = "SELECT COLUMN_NAME "+
                            "FROM information_schema.KEY_COLUMN_USAGE "+
                            "WHERE TABLE_SCHEMA = '"+dbName+"' and TABLE_NAME='"+tableName+"' " +
                            "AND CONSTRAINT_NAME='PRIMARY' ";

                    ResultSet resultColumns = db.query(q);

                    String pk = "";
                    while(resultColumns.next()) {
                        String column = resultColumns.getString(1);
                        if(pk != "" ){ pk = pk+"/"; } // Разделим несколько полей в составном ключе
                        pk += "{"+column+"}";
                    }

                    // Добавим узел SubjectMap в TriplesMap
                    res.addProperty(propSubjectMap,
                            m.createResource()
                                    //.addProperty(propGraph, "<"+ns+"graph/"+tableName+">")
                                    //.addProperty(propClass, "<"+ns+"ontology/"+tableName+">")
                                    .addProperty(propGraph, m.createResource( ns+"graph/"+tableName ))
                                    .addProperty(propClass, m.createResource( ns+"ontology/"+tableName ))
                                    .addProperty(propTemplate, ns+tableName+"/"+pk)
                    );



                    // Колонки таблици в PredicateObjectMap --------------------------------------------
                    q = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, "+
                            "IF(COLUMN_TYPE LIKE '%unsigned', 'YES', 'NO') as IS_UNSIGNED "+
                            "FROM information_schema.COLUMNS "+
                            "WHERE TABLE_SCHEMA = '"+dbName+"' and TABLE_NAME='"+tableName+"'";
                    resultColumns = db.query(q);
                    while(resultColumns.next()) {
                        String columnName = resultColumns.getString(1);

                        // Добавим узел PredicateObjectMap в TriplesMap
                        res.addProperty(propPredicateObjectMap,
                                m.createResource()
                                        .addProperty(propPredicate,  m.createResource(exNs + columnName))
                                        .addProperty(propObjectMap,
                                                m.createResource()
                                                        .addProperty(propColumn, columnName)
                                        )
                        );


                    }





/*



                    // Primary key to Inverse Functional Property mapping -------------------------------------------------
                    String q = "SELECT COLUMN_NAME "+
                            "FROM information_schema.KEY_COLUMN_USAGE "+
                            "WHERE TABLE_SCHEMA = '"+dbName+"' and TABLE_NAME='"+tableName+"' " +
                            "AND CONSTRAINT_NAME='PRIMARY' ";

                    ResultSet resultColumns = db.query(q);

                    ArrayList<String> PKeyPart = new ArrayList<String>();
                    while(resultColumns.next()) {
                        PKeyPart.add(resultColumns.getString(1));
                    }

                    // Создаём Inverse Functional Property Только если в колонке уникальные значения
                    // Если PrimaryKey  составной, создавать  Inverse Functional Property НЕ нужно
                    if(PKeyPart.size() == 1) {

                        // Создать Inverse Functional Property из певичного ключа
                        InverseFunctionalProperty ifp = m.createInverseFunctionalProperty(nsTable + PKeyPart.get(0));

                        ExistsResurs.add(PKeyPart.get(0));

                        // Добавить ограничение (Фактически в терминах БД говорим NOT NULL )
                        t_class.addSuperClass(m.createMinCardinalityRestriction(null, ifp, 1));
                    }


                    // Внешние ключи --------------------------------------------------------------------------------------
                    // !!!!! такие ключи есть только в таблицах InnoDB
                    q = "SELECT COLUMN_NAME, REFERENCED_Table_NAME,  REFERENCED_COLUMN_NAME " +
                            "FROM  information_schema.KEY_COLUMN_USAGE " +
                            "WHERE  TABLE_SCHEMA = '"+dbName+"' and referenced_table_name IS NOT NULL AND TABLE_NAME='"+tableName+"' ";

                    //log.info("q " + q);

                    resultColumns = db.query(q);
                    while(resultColumns.next()){

                        String FKeyColumnName = resultColumns.getString(1);
                        String ReferencedTableName = resultColumns.getString(2);
                        String ReferencedColumnName = resultColumns.getString(3);

                        // Создать  Object Property mapping
                        ObjectProperty op = m.createObjectProperty(nsTable +FKeyColumnName);
                        op.addDomain(t_class);
                        op.addRange(ResourceFactory.createResource(nsDB + ReferencedTableName));

                        ExistsResurs.add(FKeyColumnName);

                        // Добавить ограничение ““If foreign key is a primary key or part of a primary key"
                        if(PKeyPart.contains(FKeyColumnName)){
                            t_class.addSuperClass(m.createCardinalityRestriction(null, op, 1));
                        }
                    }


                    //Создать свойства из колонок таблицы -----------------------------------------------------------------
                    q = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, "+
                            "IF(COLUMN_TYPE LIKE '%unsigned', 'YES', 'NO') as IS_UNSIGNED "+
                            "FROM information_schema.COLUMNS "+
                            "WHERE TABLE_SCHEMA = '"+dbName+"' and TABLE_NAME='"+tableName+"'";
                    resultColumns = db.query(q);
                    while(resultColumns.next()) {
                        String columnName = resultColumns.getString(1);
                        String columnType = resultColumns.getString(2);

                        // ToDo Не знаю нужно это условие или нет
                        if (!ExistsResurs.contains(columnName)){
                            // Создать свойства из колонок
                            DatatypeProperty dp = m.createDatatypeProperty(nsTable + columnName);
                            dp.addDomain(t_class);
                            // http://sanjeewamalalgoda.blogspot.ru/2011/03/mapping-data-between-sql-typw-and-xsd.html
                            dp.addRange(ResourceFactory.createResource(owlDLDataTypeFromSql(columnType).getURI()));
                        }
                    }

    */

                }


                db.query("USE `"+dbName+"`");


            }catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
                //return sqlEx.toString();
            }


        StringWriter out = new StringWriter();
        m.write (out, outFormat, ns);

        return out.toString();

    }


















    public String getShema(){

        String out = "";
        int t = 0;

        if(db.openConnection() != null) {
            try {
                ResultSet rsTable = db.query("SHOW TABLES"); // Получить список таблиц
                while (rsTable.next()) {
                    String tName = rsTable.getString(1);

                    log.info("Process table " + tName);

                    // Получить строение таблици
                    ResultSet rsTableCrata = db.query(" SHOW CREATE TABLE `" + tName + "`");
                    while (rsTableCrata.next()) {
                        t++;
                        String tCreate = rsTableCrata.getString(2);
                        //log.info(" SHOW CREATE TABLE " + tCreate);

                        out += getTriplet(t, tName, tCreate);

                    }
                }

            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
                return sqlEx.toString();
            }

        }

        out = getHeadMap() + out;
        return out;
    }













    private static String getTriplet(int t, String tName, String tCreate){
        String resultTriplet = "";
        String resTriplet = "";
        String pkey = "";

        Pattern pItem = Pattern.compile("  `(.+)` .+");
        Pattern pK = Pattern.compile("PRIMARY KEY \\(`(.+)`\\)");


        String[] lines = tCreate.split("\\r?\\n");
        for (String str: lines) {
            Matcher m = pItem.matcher(str);
            if( m.find()){
                resTriplet +="\n";
                resTriplet +="	rr:predicateObjectMap [\n";
                resTriplet +="		rr:predicate ex:"+m.group(1)+";\n";
                resTriplet +="		rr:objectMap [ rr:column \""+m.group(1)+"\" ];\n";
                resTriplet +="	];\n";
            }

            Matcher k = pK.matcher(str);
            if( k.find()) {
                pkey = k.group(1);
            }

        }

        resultTriplet +="\n";
        resultTriplet +="<#TriplesMap"+t+">\n";
        resultTriplet +="	a rr:TriplesMap;\n";
        resultTriplet +="	rr:logicalTable [ rr:tableName  \"\\\""+tName+"\\\"\" ];\n";
        resultTriplet +="	rr:subjectMap [\n";
        resultTriplet +="		rr:template \"http://data.example.com/"+tName+"/{\\\""+pkey+"\\\"}\";\n";
        resultTriplet +="		rr:class <http://example.com/ontology/"+tName+">;\n";
        resultTriplet +="		rr:graph <http://example.com/graph/"+tName+"> ;\n";
        resultTriplet +="	];\n";
        resultTriplet += resTriplet;
        resultTriplet +="	.\n\n";

        return resultTriplet;
    }


    private static String getHeadMap () {
        String head = "@prefix rr: <http://www.w3.org/ns/r2rml#> .\n" +
                "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n" +
                "@prefix ex: <http://example.com/> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "@base <http://example.com/base/> .\n";

        return head;
    }



    public void setDb(Database db) {
        this.db = db;
    }

    public void setLog(Log log) {
        this.log = log;
    }
}
