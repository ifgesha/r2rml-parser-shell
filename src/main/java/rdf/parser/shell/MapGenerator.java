package rdf.parser.shell;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.*;


public class MapGenerator {

    private Database db;
    private static Log log;


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
