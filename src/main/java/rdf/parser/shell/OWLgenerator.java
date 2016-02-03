package rdf.parser.shell;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Женя on 01.02.2016.
 *
 *  There are five steps of our approach:
 *  (1) classification of tables,
 *  (2) mapping tables,
 *  (3) mapping columns,
 *  (4) mapping relationships,
    (5) mapping constraints.
    Next these steps will be illustrated by example.
 *
 */
public class OWLgenerator {

    private Database db;
    private static Log log;

    // Константы описывающие типы таблиц
    private static final String typeOfTableBase = "base table";
    private static final String typeOfTableDependent = "dependent table";
    private static final String typeOfTableComposite = "composite table";



    // Create an empty ontology model
    OntModel my_model = ModelFactory.createOntologyModel();
    String baseURI = "http://www.damp1r.ru";
    String ns = baseURI + "#";




    public void test(){
        Connection connection = db.openConnection();
        if(db.openConnection() != null) {

            try{
                // Имя текущей базы
                ResultSet result =  db.query("SELECT DATABASE()");
                result.next();
                String dbName = result.getString(1);


                // Выбор information_schema там хранятся все данные по базам, таблицам и т.д.
                db.query("USE `information_schema`");

                // Получить список таблиц
                result = db.query("SELECT TABLE_NAME FROM `TABLES` WHERE  TABLE_SCHEMA = '"+dbName+"'");
                while(result.next()) {
                    String tableName = result.getString(1);
                    log.info("Tables "+dbName +"." + tableName);

                    // TODo возможно нужно будет как-то классифицировать таблици и создавать классы не для всех
                    // Создаём  класс из таблицы
                    OntClass t_class = my_model.createClass(ns+tableName);


                    // Получить колонки таблицы
                    String q =
                            "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, "+
                            "IF(COLUMN_TYPE LIKE '%unsigned', 'YES', 'NO') as IS_UNSIGNED "+
                            "from information_schema.COLUMNS "+
                            "where TABLE_NAME='"+tableName+"'";
                    ResultSet resultColumns = db.query(q);

                    while(resultColumns.next()){
                        String columnName = resultColumns.getString(1);
                        String columnType = resultColumns.getString(2);;


                        DatatypeProperty dtp = my_model.createDatatypeProperty(ns+ columnName);
                        dtp.addDomain(t_class);

                        // ToDo нужно сделать преобразование типов данных mysql в типа xsd.
                        //Пока просто так вставляю но скорее всего будет не валидно
                        // http://sherdim.ru/pts/semantic_web/REC-owl-guide-20040210_ru.html
                        // http://sanjeewamalalgoda.blogspot.ru/2011/03/mapping-data-between-sql-typw-and-xsd.html
                        dtp.addRange(ResourceFactory.createResource("&xds;"+columnType));
                    }
                }


            }catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
                //return sqlEx.toString();
            }
        }

        my_model.write (System.out, "RDF/XML-ABBREV", ns);
        //my_model.write (System.out, "RDF/XML", ns);
    }

/*
    public Map<Integer, String> getAllJdbcTypeNames() {
        Map<Integer, String> result = new HashMap<Integer, String>();
        for (Field field : Types.class.getFields()) {
            try {
                result.put((Integer)field.get(null), field.getName());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
*/


    public String getShema(){

        String out = "";
        int t = 0;

        if(db.openConnection() != null) try {
           // java.sql.ResultSet rs = db.query(selectQuery.getQuery());

             db.query("USE `information_schema`");

            ResultSet rsTable = db.query("SHOW TABLES"); // Получить список таблиц
            while (rsTable.next()) {
                String tName = rsTable.getString(1);

                log.info("Process table " + tName);

                // Получить строение таблици
                ResultSet rsTableCrata = db.query(" SHOW CREATE TABLE `" + tName + "`");
                while (rsTableCrata.next()) {
                    t++;
                    String tCreate = rsTableCrata.getString(2);
                    // log.info(" SHOW CREATE TABLE " + tCreate);

                    // Прежде нужно классифицировать тип таблицы
                    String tClass = classificationOfTables(tCreate);
                    if (tClass.equals(typeOfTableBase)) {
                        mappingTableBase(tName, tCreate);
                    } else
                    if (tClass.equals(typeOfTableDependent)) {
                        // ToDo Обработка таблиц данного типа
                    } else
                    if (tClass.equals(typeOfTableComposite)) {
                        // ToDo Обработка таблиц данного типа
                    }





                }
            }

        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
            return sqlEx.toString();
        }

        //out = getHeadMap() + out;
        return out;
    }


    private static String classificationOfTables(String tCreate){
        // ToDo Сделать классификатор типов таблиц
        return typeOfTableBase;
    }


    private static String mappingTableBase(String tName, String tCreate){
        String result = "<owl:Class rdf:ID=\""+tName+"\"/>";



        return result;
    }




    public void setDb(Database db) {
        this.db = db;
    }

    public void setLog(Log log) {
        this.log = log;
    }








}
