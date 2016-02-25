package rdf.parser.shell;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDBFactory;
import gr.seab.r2rml.entities.MappingDocument;


import java.io.StringWriter;
import java.sql.*;
import java.util.*;



public class OWLgenerator {

    private Database db;
    private static Log log;


    // Create an empty ontology model
    OntModel m = ModelFactory.createOntologyModel();
    String baseURI = "http://www.damp1r.ru";
    String sep = "/";
    String ns = baseURI + "#";
    String nsDB = "";
    String nsTable = "";


    public String createOWL(String outFormat, boolean writeTDB){



        if(m.getDocumentManager() != null) {

            try{
                // Имя текущей базы
                ResultSet result =  db.query("SELECT DATABASE()");
                result.next();
                String dbName = result.getString(1);

                // СТАРЫЙ Вариант NS
                nsDB = ns + dbName + sep ;

                // НОВЫЙ Вариант  NS
                baseURI = baseURI + sep + dbName;
                ns = baseURI + "#";
                nsDB = ns ;


                // Выбор information_schema там хранятся все данные по базам, таблицам и т.д.
                db.query("USE `information_schema`");

                // Получить список таблиц
                result = db.query("SELECT TABLE_NAME FROM `TABLES` WHERE  TABLE_SCHEMA = '"+dbName+"' #and TABLE_NAME in ('customer','borrower')" );
                while(result.next()) {
                    String tableName = result.getString(1);
                    log.info("Tables "+dbName +"." + tableName);

                    nsTable = nsDB +tableName + sep;

                    ArrayList<String> ExistsResurs = new ArrayList<String>();



                    // 7.	Если Первичный ключ таблицы состоит из двух Внешних ключей к двум другим таблицам,
                    // а сама таблица дополнительных атрибутов не имеет, то такая таблица не преобразовывается в класс,
                    // а сами внешние ключи преобразуются в Объектное свойство и Объектное обратно свойство (inverseOf).
                    // Каждое свойство характеризуется доменом и диапазоном в соответствии с правилами их формирования
                    // для внешних ключей.

                    String q = "select \n" +
                            "(select count(COLUMN_NAME) from `COLUMNS`  WHERE TABLE_SCHEMA  = '"+dbName+"' and TABLE_NAME='"+tableName+"' ) as c_col,\n" +
                            "(select count(COLUMN_NAME) from `KEY_COLUMN_USAGE`  WHERE TABLE_SCHEMA  = '"+dbName+"' and TABLE_NAME='"+tableName+"' AND CONSTRAINT_NAME='PRIMARY') as c_pk, " +
                            "(select count(COLUMN_NAME) from `KEY_COLUMN_USAGE`  WHERE TABLE_SCHEMA  = '"+dbName+"' and TABLE_NAME='"+tableName+"' AND referenced_table_name IS NOT NULL ) as c_fk ";

                    ResultSet resultColumns = db.query(q);
                    resultColumns.next();
                    if(resultColumns.getInt(1) == 2 && resultColumns.getInt(2) == 2 && resultColumns.getInt(3) == 2){


                        q = "SELECT COLUMN_NAME, REFERENCED_Table_NAME,  REFERENCED_COLUMN_NAME " +
                                "FROM  information_schema.KEY_COLUMN_USAGE " +
                                "WHERE  TABLE_SCHEMA = '"+dbName+"' and referenced_table_name IS NOT NULL AND TABLE_NAME='"+tableName+"' ";
                        resultColumns = db.query(q);

                        // Ролучить данные по таблицам входящим во внешние ключи
                        resultColumns.next();
                        String ReferencedTableName1 = resultColumns.getString(2);
                        String ReferencedColumnName1 = resultColumns.getString(3);

                        resultColumns.next();
                        String ReferencedTableName2 = resultColumns.getString(2);
                        String ReferencedColumnName2 = resultColumns.getString(3);

                        // Создадим класс таблици если его нет ********* для первой т.
                        OntClass t_class;
                        if(m.getOntClass(nsDB +ReferencedTableName1) != null){
                            t_class= m.getOntClass(nsDB +ReferencedTableName1);
                        }else{
                            t_class = m.createClass(nsDB +ReferencedTableName1);
                        }
                        // Создать  Object Property mapping
                        ObjectProperty op = m.createObjectProperty(nsTable +ReferencedColumnName1);
                        op.addDomain(t_class);
                        op.addRange(ResourceFactory.createResource(nsDB + ReferencedTableName2));
                        ExistsResurs.add(ReferencedColumnName1);
                        // Добавить ограничение ““If foreign key is a primary key or part of a primary key"
                        t_class.addSuperClass(m.createCardinalityRestriction(null, op, 1));


                        // Создадим класс таблици если его нет ********* для второй т.
                        //if(m.getOntClass(nsDB +ReferencedTableName2) != null){
                        //    t_class= m.getOntClass(nsDB +ReferencedTableName2);
                        //}else{
                            t_class = m.createClass(nsDB +ReferencedTableName2); // Дублирования можно не боятся
                        //}
                        // Создать  Object Property mapping
                        op = m.createObjectProperty(nsTable +ReferencedColumnName2);
                        op.addDomain(t_class);
                        op.addRange(ResourceFactory.createResource(nsDB + ReferencedTableName1));
                        op.addInverseOf(m.getOntProperty(nsTable +ReferencedColumnName1));
                        ExistsResurs.add(ReferencedColumnName2);
                        // Добавить ограничение ““If foreign key is a primary key or part of a primary key"
                        t_class.addSuperClass(m.createCardinalityRestriction(null, op, 1));



                        continue; // Далее не идём
                    }





                    // Создаём  класс из таблицы
                    OntClass t_class = m.createClass(nsDB +tableName);

                    // Primary key to Inverse Functional Property mapping -------------------------------------------------
                    q = "SELECT COLUMN_NAME "+
                            "FROM information_schema.KEY_COLUMN_USAGE "+
                            "WHERE TABLE_SCHEMA = '"+dbName+"' and TABLE_NAME='"+tableName+"' " +
                            "AND CONSTRAINT_NAME='PRIMARY' ";

                    resultColumns = db.query(q);

                    ArrayList<String> PKeyPart = new ArrayList<String>();
                    while(resultColumns.next()) {
                        PKeyPart.add(resultColumns.getString(1));

                        // Создать Inverse Functional Property из певичного ключа
                        InverseFunctionalProperty ifp = m.createInverseFunctionalProperty(nsTable + resultColumns.getString(1));

                        ifp.addDomain(t_class);

                        ExistsResurs.add(PKeyPart.get(0));

                        // Добавить ограничение (Фактически в терминах БД говорим NOT NULL )
                        t_class.addSuperClass(m.createMinCardinalityRestriction(null, ifp, 1));
                    }

                    /*

                    // Создаём Inverse Functional Property Только если в колонке уникальные значения
                    // Если PrimaryKey  составной, создавать  Inverse Functional Property НЕ нужно
                    if(PKeyPart.size() == 1) {

                        // Создать Inverse Functional Property из певичного ключа
                        InverseFunctionalProperty ifp = m.createInverseFunctionalProperty(nsTable + PKeyPart.get(0));

                        ExistsResurs.add(PKeyPart.get(0));

                        // Добавить ограничение (Фактически в терминах БД говорим NOT NULL )
                        t_class.addSuperClass(m.createMinCardinalityRestriction(null, ifp, 1));
                    }
                    */


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
                        //ObjectProperty op = m.createObjectProperty(nsTable +FKeyColumnName);
                        ObjectProperty op = m.createObjectProperty(nsDB +ReferencedTableName + sep + tableName );
                        op.addDomain(t_class);
                        op.addRange(ResourceFactory.createResource(nsDB + ReferencedTableName));

                        //ExistsResurs.add(FKeyColumnName);

                        // Добавить ограничение ““If foreign key is a primary key or part of a primary key"
                        // if(PKeyPart.contains(FKeyColumnName)){
                        if(PKeyPart.contains(nsDB +ReferencedTableName + sep + tableName )){
                            t_class.addSuperClass(m.createCardinalityRestriction(null, op, 1));
                        }

                        // Сделать тукущий класс подкласом
                        t_class.addSuperClass(m.createClass(nsDB +ReferencedTableName));

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



                }


                db.query("USE `"+dbName+"`");


            }catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
                //return sqlEx.toString();
            }
        }

      // log.info("\n\n----------- OWL -----------\n\n"+ont);



        // Префиксы
        m.setNsPrefix("", ns);

        // Заголовки онтологии
        m.createOntology( baseURI );
        Ontology ont = m.getOntology( baseURI );
        ont.addComment("Auto generate оntology from RDB Shema","en");
        ont.addVersionInfo("v 1.00");
        //ont.addImport( m.createResource( "http://example.com/import1" ) );
        //ont.addImport( m.createResource( "http://example.com/import2" ) );

        //-----------------------------

        if(writeTDB){ WriteTDB ();}

        StringWriter out = new StringWriter();
        m.write (out, outFormat, ns);

        return out.toString();

    }



    public void WriteTDB (){

        String jenaTdbDirectory = "tdb_ont4rdb";
        Model resultModel = m;
        MappingDocument mappingDocument = new MappingDocument();

        // Очистить содержимое
        log.info("Clear model to database.");
        Dataset dataset = TDBFactory.createDataset(jenaTdbDirectory);
        dataset.begin(ReadWrite.WRITE);
        Model dm = dataset.getDefaultModel();
        dm.removeAll();
        dataset.commit();
        dataset.end();


        log.info("Storing model to database. Model has " + resultModel.size() + " statements.");
        Calendar c0 = Calendar.getInstance();
        long t0 = c0.getTimeInMillis();

        //Sync start
        dataset = TDBFactory.createDataset(jenaTdbDirectory);
        dataset.begin(ReadWrite.WRITE);

        Model existingDbModel = dataset.getDefaultModel();
        log.info("Existing model has " + existingDbModel.size() + " statements.");

        List<com.hp.hpl.jena.rdf.model.Statement> statementsToRemove = new ArrayList<com.hp.hpl.jena.rdf.model.Statement>();
        List<com.hp.hpl.jena.rdf.model.Statement> statementsToAdd = new ArrayList<com.hp.hpl.jena.rdf.model.Statement>();

        /*
        //first clear the ones from the old model
        StmtIterator stmtExistingIter = existingDbModel.listStatements();
        while (stmtExistingIter.hasNext()) {
            com.hp.hpl.jena.rdf.model.Statement stmt = stmtExistingIter.nextStatement();
            if (!resultModel.contains(stmt)) {
                statementsToRemove.add(stmt);
            }
        }
        stmtExistingIter.close();
        log.info("Will remove " + statementsToRemove.size() + " statements.");
        */

        //then add the new ones
        Model differenceModel = resultModel.difference(existingDbModel);
        StmtIterator stmtDiffIter = differenceModel.listStatements();
        while (stmtDiffIter.hasNext()) {
            com.hp.hpl.jena.rdf.model.Statement stmt = stmtDiffIter.nextStatement();
            statementsToAdd.add(stmt);
        }
        stmtDiffIter.close();
        differenceModel.close();
        log.info("Will add " + statementsToAdd.size() + " statements.");

        existingDbModel.remove(statementsToRemove);
        existingDbModel.add(statementsToAdd);
        dataset.commit();
        dataset.end();

        //Sync end
        Calendar c1 = Calendar.getInstance();
        long t1 = c1.getTimeInMillis();
        log.info("Updating model in database took " + (t1 - t0) + " milliseconds.");
        mappingDocument.getTimestamps().add(Calendar.getInstance().getTimeInMillis()); //3 Wrote clean model to tdb.

    }







    public BaseDatatype owlDLDataTypeFromSql(String sqlDataType) {
        sqlDataType = sqlDataType.toLowerCase();
        if (    sqlDataType.equals("char")
                || sqlDataType.equals("varchar")
                || sqlDataType.equals("binary")
                || sqlDataType.equals("varbinary")
                || sqlDataType.equals("tinyblob ")
                || sqlDataType.equals("tinytext")
                || sqlDataType.equals("blob ")
                || sqlDataType.equals("text")
                || sqlDataType.equals("mediumblob")
                || sqlDataType.equals("mediumtext")
                || sqlDataType.equals("longtext")
                || sqlDataType.equals("longblob")
                || sqlDataType.equals("enum")
                || sqlDataType.equals("set")
                ) {
            return XSDDatatype.XSDstring;

        } else if (sqlDataType.equals("bit")
                || sqlDataType.equals("boolean")) {
            return XSDDatatype.XSDboolean;

        } else if (sqlDataType.equals("tinyint")) {
            return XSDDatatype.XSDbyte;

        } else if (sqlDataType.equals("smallint")) {
            return XSDDatatype.XSDshort;

        } else if (sqlDataType.equals("mediumint")
                || sqlDataType.equals("int")) {
            return XSDDatatype.XSDint;

        } else if (sqlDataType.equals("bigint")) {
            return XSDDatatype.XSDlong;

        } else if (sqlDataType.equals("dec")
                || sqlDataType.equals("decimal")) {
            return XSDDatatype.XSDdecimal;

        } else if (sqlDataType.equals("double")
                || sqlDataType.equals("real")) {
            return XSDDatatype.XSDdouble;

        } else if (sqlDataType.equals("float")) {
            return XSDDatatype.XSDfloat;

        } else if (sqlDataType.equals("date")) {
            return XSDDatatype.XSDdate;

        } else if (sqlDataType.equals("datetime")
                || sqlDataType.equals("timestamp")) {
            return XSDDatatype.XSDdateTime;

        } else if (sqlDataType.equals("time")) {
            return XSDDatatype.XSDtime;

        } else if (sqlDataType.equals("year")) {
            return XSDDatatype.XSDgYear;

        } else {
            String err = "Found unknown mySQL sqlDataType " + sqlDataType;
            log.error(err);
            throw new RuntimeException(err);
        }
    }

    public void setDb(Database db) {
        this.db = db;
    }

    public void setLog(Log log) {
        this.log = log;
    }








}
