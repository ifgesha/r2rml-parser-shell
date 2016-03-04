package rdf.parser.shell;


import com.hp.hpl.jena.rdf.model.*;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.*;


public class MapGenerator {

    private Database db;
    private static Log log;
    private static Properties properties;



    private final String rrNs = "http://www.w3.org/ns/r2rml#";
    private final String foafNs = "http://xmlns.com/foaf/0.1/";
    private final String xsdNs = "http://www.w3.org/2001/XMLSchema#";
    private final String rdfsNs = "http://www.w3.org/2000/01/rdf-schema#";



    Model m = ModelFactory.createDefaultModel();



    public String makeR2RML(){


        String ns = properties.getProperty("gen.ns.map");
        String exNs = properties.getProperty("gen.ns.map.ex");



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
        Property propParentTriplesMap = m.createProperty(rrNs +"parentTriplesMap");
        Property propJoinCondition = m.createProperty(rrNs +"joinCondition");
        Property propChild = m.createProperty(rrNs +"child");
        Property propParent = m.createProperty(rrNs +"parent");




        try{
                // Имя текущей базы
                ResultSet result =  db.query("SELECT DATABASE()");
                result.next();
                String dbName = result.getString(1);

                // Выбор information_schema там хранятся все данные по базам, таблицам и т.д.
                db.query("USE `information_schema`");

                // Получить список таблиц
                result = db.query("SELECT TABLE_NAME FROM `TABLES` WHERE  TABLE_SCHEMA = '"+dbName+"' # limit 1" );
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
                        pk += column.toLowerCase()+"={"+column.toUpperCase()+"}";
                    }

                    // Нужно делать шаблон в разных регистрах иначе если в любом месте строки встретится то что есть в {} будет не корректный парсинг
                    String rrTemplate = ns+tableName;
                    rrTemplate = rrTemplate.toLowerCase()+"/"+pk;


                    // Добавим узел SubjectMap в TriplesMap
                    res.addProperty(propSubjectMap,
                            m.createResource()
                                    .addProperty(propGraph, m.createResource( ns+"graph/"+tableName ))
                                    .addProperty(propClass, m.createResource( ns+"/"+tableName ))
                                    .addProperty(propTemplate, rrTemplate)
                    );


                    // Внешние ключи --------------------------------------------------------------------------------------
                    // !!!!! такие ключи есть только в таблицах InnoDB
                    q = "SELECT COLUMN_NAME, REFERENCED_Table_NAME,  REFERENCED_COLUMN_NAME " +
                            "FROM  information_schema.KEY_COLUMN_USAGE " +
                            "WHERE  TABLE_SCHEMA = '"+dbName+"' and referenced_table_name IS NOT NULL AND TABLE_NAME='"+tableName+"' ";


                    resultColumns = db.query(q);
                    while(resultColumns.next()){

                        String FKeyColumnName = resultColumns.getString(1);
                        String ReferencedTableName = resultColumns.getString(2);
                        String ReferencedColumnName = resultColumns.getString(3);

                        // Добавим узел PredicateObjectMap в TriplesMap
                        res.addProperty(propPredicateObjectMap,
                                m.createResource()
                                        .addProperty(propPredicate, m.createResource(exNs + "/" + tableName +"#ref-" + FKeyColumnName))
                                        .addProperty(propObjectMap,
                                                m.createResource()
                                                        .addProperty(propParentTriplesMap,  m.createResource("#TriplesMap_"+ReferencedTableName))
                                                        .addProperty(propJoinCondition,
                                                                m.createResource()
                                                                        .addProperty(propChild, FKeyColumnName)
                                                                        .addProperty(propParent, ReferencedColumnName)

                                                        )
                                        )
                        );

                    }




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
                                        .addProperty(propPredicate, m.createResource(exNs + "/" + tableName + "#"+columnName))
                                        .addProperty(propObjectMap,
                                                m.createResource()
                                                        .addProperty(propColumn, columnName)
                                        )
                        );


                    }


                }


                db.query("USE `"+dbName+"`");


            }catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
                //return sqlEx.toString();
            }


        StringWriter out = new StringWriter();


        try {
            m.write(out, properties.getProperty("gen.file.type"), ns);
            log.info("Result size =  " + out.toString().length());

        }catch (Exception e){
            log.error("Error " + e.toString());
        }



        return out.toString();

    }







    public void setDb(Database db) {
        this.db = db;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

}
