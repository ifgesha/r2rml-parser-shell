package rdf.parser.shell;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import gr.seab.r2rml.entities.MappingDocument;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


public class StoreTDB {

    private static Log log;
    private static Properties properties;

    private String jenaTdbDirectory = "tdb_ont4rdb";


    public List<String> getNamedModelList () {
        Iterator<String> modelName;
        List<String> myList = new CopyOnWriteArrayList<String>();

        // Получить модель из TDB
        Dataset dataset = TDBFactory.createDataset(jenaTdbDirectory);
        dataset.begin(ReadWrite.READ);
            modelName = dataset.listNames();     // Список именованых моделей
        dataset.end();

        while (modelName.hasNext()) {
            String mName =  modelName.next();
            myList.add(mName);
            System.out.println("TDB Model : " + mName);
        }

        return myList;
    }



    public void getOwlClasses (String NamedModel) {
        Model model;

        String outClasses = "";
        String outSubClassLink = "";
        String outProperty = "";
        String outPropertyLink = "";
        Integer cid = 0;
        Integer pid = 0;


        // Получить модель из TDB
        Dataset dataset = TDBFactory.createDataset(jenaTdbDirectory);
        dataset.begin(ReadWrite.READ);

             // Get model inside the transaction
            if(NamedModel != null && !NamedModel.equals("")){
                System.out.println("\nClasses of TDB Model : " + NamedModel);
                model = dataset.getNamedModel(NamedModel); // Получить именованцю модель
            }else{
                System.out.println("\nClasses of TDB Model : DefaultModel" );
                model = dataset.getDefaultModel();          // Получить модель по умолчанию
                NamedModel = "def";
            }

        dataset.end();


        // Создать онтологию на основе модели
        OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_MEM );
        OntModel m =  ModelFactory.createOntologyModel(spec,  model);

        //writeNamedModelTDB(m, "model2", false);
        //writeNamedModelTDB(m, "model3", false);
        //writeNamedModelTDB(m, "model4", false);



        // Итератор классов модели
        ExtendedIterator classes = m.listClasses();

        // Перебор классов
        while (classes.hasNext()) {
            OntClass essaClasse = (OntClass) classes.next();

            if(essaClasse.getLocalName() != null){

                cid ++;
                String ns = essaClasse.getNameSpace();
                String vClasse = essaClasse.getLocalName();
                System.out.println("Classe: " + vClasse);
                outClasses += "'"+vClasse+"':{'id': '"+NamedModel+"_c"+cid+"', 'name': '"+vClasse+"', 'ont': '"+NamedModel+"', 'n_type': 'class' },\n";

                // Перебор подклассов
                OntClass cla = m.getOntClass(ns + vClasse);
                if (essaClasse.hasSubClass() ) {
                    for (Iterator i = cla.listSubClasses(); i.hasNext(); ) {
                        OntClass subC = (OntClass) i.next();
                        System.out.print("         sub: " + subC.getLocalName() + " " + "\n");
                        pid++;
                        String subName = "subclass_"+ subC.getLocalName();

                        outClasses += "'"+subName+"':{'id': '"+NamedModel+"_subclass_"+pid+"', 'name': 'Subclass of', 'ont': '"+NamedModel+"', 'n_type':'subclass' },\n";

                        outSubClassLink += "{ 'source':'" + subC.getLocalName() + "', 'target': '" + subName + "', 'l_type':'subclass'},\n";
                        outSubClassLink += "{ 'source':'" + subName + "', 'target': '" + vClasse + "', 'l_type':'subclass'},\n";
                    }
                }

            }
        }


        // Получить ObjectProperties
        String[] outP = prepareProperty(m.listObjectProperties(), NamedModel, "oprop");
        outProperty += outP[0];
        outPropertyLink += outP[1];


        // Получить DatatypeProperties
        outP = prepareProperty(m.listDatatypeProperties(), NamedModel, "dprop");
        outProperty += outP[0];
        outPropertyLink += outP[1];


        // Подготовка перед выводом
        // Проще тут всё заменить чем городить \"\"\"
        outClasses = outClasses.replaceAll("'","\"");
        outProperty = outProperty.replaceAll("'","\"").substring(0, outProperty.length() - 2);
        outSubClassLink = outSubClassLink.replaceAll("'","\"");
        outPropertyLink = outPropertyLink.replaceAll("'","\"").substring(0, outPropertyLink.length() - 2);




        System.out.println( "{  \"nodes\": {");

        System.out.println( outClasses);
        System.out.println( outProperty);

        System.out.println( "},\"links\": [");

        System.out.println( outSubClassLink);
        System.out.println( outPropertyLink);

        System.out.println("]}");


    }


    public String[] prepareProperty (ExtendedIterator prop, String idPref, String n_type){
        String outProp = "";
        String outLink = "";
        Integer pid = 0;

        while (prop.hasNext()) {
            OntProperty p = (OntProperty) prop.next();

            if(p.getLocalName() != null) {
                pid++;
                String ns = p.getNameSpace();
                String classDonain = p.getDomain().getLocalName();
                String pName =  classDonain + "_"+ p.getLocalName();
                String pNameLocal =  p.getLocalName();

                System.out.println("Class: " + classDonain + " Prop: " + pName);

                outProp += "'"+pName+"':{'id': '"+idPref+"_"+n_type+"_"+pid+"', 'name': '"+pNameLocal+"', 'ont': '"+idPref+"', 'n_type': '"+n_type+"' },\n";

                // Линки
                if(n_type.equals("oprop")){
                    String classRange = p.getRange().getLocalName();
                    outLink += "{ 'source':'" + pName + "', 'target': '" + classRange + "', 'l_type':'oprop'},\n";
                    outLink += "{ 'source':'" + classDonain + "', 'target': '" + pName + "', 'l_type':'oprop'},\n";
                }else{
                    outLink += "{ 'source':'" + pName + "', 'target': '" + classDonain + "', 'l_type':'dprop'},\n";
                }



            }


        }
        return new String[] {outProp, outLink };
    }





    public void writeNamedModelTDB (Model resultModel, String namedModel, Boolean clearModel){

        //Model resultModel = m;
        MappingDocument mappingDocument = new MappingDocument();

        Dataset dataset = TDBFactory.createDataset(jenaTdbDirectory);

        // Очистить содержимое
        if(clearModel) {
            log.info("Clear model to database.");
            dataset.begin(ReadWrite.WRITE);
            //Model dm = dataset.getDefaultModel();
            Model dm = dataset.getNamedModel(namedModel);
            dm.removeAll();
            dataset.commit();
            dataset.end();
        }

        log.info("Storing model to database. Model has " + resultModel.size() + " statements.");
        Calendar c0 = Calendar.getInstance();
        long t0 = c0.getTimeInMillis();

        //Sync start
        dataset = TDBFactory.createDataset(jenaTdbDirectory);
        dataset.begin(ReadWrite.WRITE);

        Model existingDbModel = dataset.getNamedModel(namedModel);
        log.info("Existing model has " + existingDbModel.size() + " statements.");

        List<Statement> statementsToRemove = new ArrayList<Statement>();
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


    public void setLog(Log log) {
        this.log = log;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }


}
