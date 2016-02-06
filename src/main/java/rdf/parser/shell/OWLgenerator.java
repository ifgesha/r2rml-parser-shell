package rdf.parser.shell;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


import java.io.StringWriter;
import java.sql.*;


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
    OntModel m = ModelFactory.createOntologyModel();
    String baseURI = "http://www.damp1r.ru";
    String ns = baseURI + "#";

    String ns_xds = "http://www.w3.org/2001/XMLSchema#";

    public void createOWL(){



        OntDocumentManager dm = m.getDocumentManager();

        if(db.openConnection() != null) {

            try{
                // Имя текущей базы
                ResultSet result =  db.query("SELECT DATABASE()");
                result.next();
                String dbName = result.getString(1);


                // Выбор information_schema там хранятся все данные по базам, таблицам и т.д.
                db.query("USE `information_schema`");

                // Получить список таблиц
                result = db.query("SELECT TABLE_NAME FROM `TABLES` WHERE  TABLE_SCHEMA = '"+dbName+"' #limit 3" );
                while(result.next()) {
                    String tableName = result.getString(1);
                    log.info("Tables "+dbName +"." + tableName);

                    // TODo возможно нужно будет как-то классифицировать таблици и создавать классы не для всех
                    // Создаём  класс из таблицы
                    OntClass t_class = m.createClass(ns+tableName);

                    //Создать свойства из колонок таблицы --------------
                    String  q = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, "+
                                    "IF(COLUMN_TYPE LIKE '%unsigned', 'YES', 'NO') as IS_UNSIGNED "+
                                    "FROM information_schema.COLUMNS "+
                                    "WHERE TABLE_SCHEMA = '"+dbName+"' and TABLE_NAME='"+tableName+"'";
                    ResultSet resultColumns = db.query(q);
                    while(resultColumns.next()){
                        String columnName = resultColumns.getString(1);
                        String columnType = resultColumns.getString(2);

                        // Создать свойства из колонок
                        DatatypeProperty dp = m.createDatatypeProperty(ns + columnName);
                        dp.addDomain(t_class);
                        // http://sanjeewamalalgoda.blogspot.ru/2011/03/mapping-data-between-sql-typw-and-xsd.html
                        dp.addRange(ResourceFactory.createResource(findDataTypeFromSql(columnType).getURI()));
                    }

                    // Primary key to Inverse Functional Property mapping --------------
                    // ToDo Не понятно что делать с составными первичными ключами ????
                    // Составной ключ выглядит как 2 и более строк результата запроса
                    //
                    q = "SELECT COLUMN_NAME "+
                                    "FROM information_schema.KEY_COLUMN_USAGE "+
                                    "WHERE TABLE_SCHEMA = '"+dbName+"' and TABLE_NAME='"+tableName+"' AND CONSTRAINT_NAME='PRIMARY'";
                    resultColumns = db.query(q);
                    while(resultColumns.next()){
                        String PKeyColumnName = resultColumns.getString(1);

                        // Создать Inverse Functional Property из певичного ключа
                        InverseFunctionalProperty ifp = m.createInverseFunctionalProperty(ns + PKeyColumnName);

                        // Добавить ограничение
                        // ToDo Не понятно что делать если тип поля ключа НЕ int
                        t_class.addSuperClass( m.createMinCardinalityRestriction(null, ifp, 1 ));
                    }


                    // Внешние ключи --------------
                    // !!!!! такие ключи есть только в таблицах InnoDB
                    q = "SELECT COLUMN_NAME, constraint_name, referenced_table_name, REFERENCED_COLUMN_NAME  " +
                            "FROM  information_schema.KEY_COLUMN_USAGE " +
                            "WHERE  TABLE_SCHEMA = '"+dbName+"' and referenced_table_name IS NOT NULL AND TABLE_NAME='"+tableName+"'";
                    resultColumns = db.query(q);
                    while(resultColumns.next()){
                        String ChildColumnName = resultColumns.getString(1);
                        String FKeyName = resultColumns.getString(2);
                        String ParentTableName = resultColumns.getString(3);
                        String ParentColumnName = resultColumns.getString(4);


                        // Создать  Object Property mapping
                        ObjectProperty op = m.createObjectProperty(ns + ChildColumnName);
                        op.addDomain(t_class);
                        op.addRange(ResourceFactory.createResource(ns + ParentColumnName));

                        // ToDo сделать проверки "If foreign key is a primary key or part of a primary key" и доавлять ограничения


                    }



                }


            }catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
                //return sqlEx.toString();
            }
        }


        //m.write (System.out, "RDF/XML", ns);

        StringWriter out = new StringWriter();
        m.write (out, "RDF/XML-ABBREV", ns);
        String result = out.toString();
        log.info("\n\n----------- OWL -----------\n\n"+result);



    }





    public BaseDatatype findDataTypeFromSql(String sqlDataType) {
        sqlDataType = sqlDataType.toLowerCase();
        if (sqlDataType.equals("character")
                || sqlDataType.equals("text")
                || sqlDataType.equals("longtext")
                || sqlDataType.equals("tinytext")
                || sqlDataType.equals("mediumtext")
                || sqlDataType.contains("varchar")
                || sqlDataType.contains("char")
                || sqlDataType.contains("varbit")
                || sqlDataType.contains("cidr")
                || sqlDataType.contains("inet")
                || sqlDataType.contains("macaddr")) {
            return XSDDatatype.XSDstring;
            //if the sql field type is a string, it is ok to omit the xsd datatype from the result
            //return null;
        } else if (sqlDataType.equals("binary")
                || sqlDataType.equals("bytea")) {
            return XSDDatatype.XSDbase64Binary;
        } else if (sqlDataType.contains("numeric")
                || sqlDataType.contains("decimal")) {
            return XSDDatatype.XSDdecimal;
        } else if (sqlDataType.equals("smallint")
                || sqlDataType.equals("integer")
                || sqlDataType.equals("bigint")
                || sqlDataType.equals("int")
                || sqlDataType.equals("int2")
                || sqlDataType.equals("int4")
                || sqlDataType.equals("int8")
                || sqlDataType.equals("serial")
                || sqlDataType.equals("serial4")
                || sqlDataType.equals("bigserial")) {
            return XSDDatatype.XSDinteger;
        } else if (sqlDataType.equals("float")
                || sqlDataType.equals("float4")
                || sqlDataType.equals("float8")
                || sqlDataType.equals("real")
                || sqlDataType.equals("double precision")
                || sqlDataType.equals("number")) {
            return XSDDatatype.XSDdouble;
        } else if (sqlDataType.equals("boolean")
                || sqlDataType.equals("bool")) {
            return XSDDatatype.XSDboolean;
        } else if (sqlDataType.equals("datetime")) {
            return XSDDatatype.XSDdateTime;
        } else if (sqlDataType.equals("date")) {
            return XSDDatatype.XSDdate;
        } else if (sqlDataType.equals("time")
                || sqlDataType.equals("timetz")) {
            return XSDDatatype.XSDtime;
        } else if (sqlDataType.equals("timestamp")
                || sqlDataType.equals("timestamptz")) {
            return XSDDatatype.XSDdateTime;
        } else {
            String err = "Found unknown SQL sqlDataType " + sqlDataType;
            log.error(err);
            throw new RuntimeException(err);
            //System.exit(1);
        }
        //return null;
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
