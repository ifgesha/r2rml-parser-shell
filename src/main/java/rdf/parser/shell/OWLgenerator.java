package rdf.parser.shell;

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





    public void test(){
        Connection connection = db.openConnection();
        if(db.openConnection() != null) {

            Map<Integer, String> jdbcMappings = getAllJdbcTypeNames();

            try{

                DatabaseMetaData databaseMetaData = connection.getMetaData();

                //Listing Tables
                // http://docs.oracle.com/javase/6/docs/api/java/sql/DatabaseMetaData.html#getTables%28java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String%5b%5d%29
                ResultSet result = databaseMetaData.getTables(null, null,null,null);
                while(result.next()) {
                    String tableName = result.getString(3); // TABLE_NAME - 3
                    log.info("Listing Tables " + tableName +" -- " + result.getString(9) + "---" + result.getString(10) );

                    // Listing Columns in a Table
                    ResultSet resultGetColumns = databaseMetaData.getColumns(null, null,  tableName, null);
                    while(resultGetColumns.next()){
                        String columnName = resultGetColumns.getString(4);
                        String columnType = jdbcMappings.get(resultGetColumns.getInt(5));
                        log.info("\tColumns in a Table type=" + columnType +"   " + columnName );
                    }



                }
/*
                select COLUMN_NAME,
                        COLUMN_TYPE,
                        IS_NULLABLE,
                IF(COLUMN_TYPE LIKE '%unsigned', 'YES', 'NO') as IS_UNSIGNED
                from information_schema.COLUMNS
                #where TABLE_NAME='record1'
*/

            }catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
                //return sqlEx.toString();
            }


        }

    }


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



    public String getShema(){

        String out = "";
        int t = 0;

        if(db.openConnection() != null) try {
           // java.sql.ResultSet rs = db.query(selectQuery.getQuery());

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

        out = getHeadMap() + out;
        return out;
    }


    private static String classificationOfTables(String tCreate){
        // ToDo Сделать классификатор типов таблиц
        return typeOfTableBase;
    }


    private static String mappingTableBase(String tName, String tCreate){
        String result = "<owl:Class rdf:ID=\""+tName+"\"/>";


        // Mapping Columns
/*

        try {
            ResultSetMetaData rsMeta = rs.getMetaData();
            if (verbose) log.info("Table name " + rsMeta.getTableName(1));
            for (int i = 1; i <= rsMeta.getColumnCount(); i++) {
                if (verbose) log.info("Column name is " + rsMeta.getColumnName(i));
                if (rsMeta.getColumnName(i).equals(field)) {
                    String sqlType = rsMeta.getColumnTypeName(i);
                    if (verbose) log.info("Column " + i + " with name " + rsMeta.getColumnName(i) + " is of type " + sqlType);
                    return util.findDataTypeFromSql(sqlType);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

*/


        return result;
    }

    private static String getOWL(int t, String tName, String tCreate){
        String resultTriplet = "";
        String res = "";
        String pkey = "";

        Pattern pItem = Pattern.compile("  `(.+)` .+");
        Pattern pK = Pattern.compile("PRIMARY KEY \\(`(.+)`\\)");


        String[] lines = tCreate.split("\\r?\\n");
        for (String str: lines) {
            Matcher m = pItem.matcher(str);
       /*     if( m.find()){
                res +="\n";
                res +="	rr:predicateObjectMap [\n";
                res +="		rr:predicate ex:"+m.group(1)+";\n";
                res +="		rr:objectMap [ rr:column \""+m.group(1)+"\" ];\n";
                res +="	];\n";
            }
*/
            Matcher k = pK.matcher(str);
            if( k.find()) {
                pkey = k.group(1);
            }

        }
/*
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
*/
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
