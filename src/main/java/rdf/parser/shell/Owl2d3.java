package rdf.parser.shell;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Iterator;

/**
 *
 */
public class Owl2d3 {

    private static final Logger log = LoggerFactory.getLogger(Owl2d3.class);


    public Model ReadModelFromFile(String modelFile, String ns ) {
        Model m;

        InputStream isMap = FileManager.get().open(modelFile);
        m=ModelFactory.createDefaultModel();
        try{
            m.read(isMap, ns);
        }
        catch( Exception e){
            String err = "Error reading mapping file " + modelFile + e.toString();
            log.error(err);
            throw new RuntimeException(err);
        }

         return m;

    }


    public String MakeD3Json (String NamedModel, Model model) {

        String result = "";

        String outClasses = "";
        String outSubClassLink = "";
        String outProperty = "";
        String outPropertyLink = "";
        Integer cid = 0;
        Integer pid = 0;

        // Создать онтологию на основе модели
        OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_MEM );
        OntModel m =  ModelFactory.createOntologyModel(spec,  model);



        // Итератор классов модели
        ExtendedIterator classes = m.listClasses();

        // Перебор классов
        while (classes.hasNext()) {
            OntClass essaClasse = (OntClass) classes.next();

            if(essaClasse.getLocalName() != null){

                cid ++;
                String ns = essaClasse.getNameSpace();
                String vClasse = essaClasse.getLocalName();
                log.info("Classe: " + vClasse);
                outClasses += "'"+vClasse+"':{'id': '"+NamedModel+"_c"+cid+"', 'name': '"+vClasse+"', 'ont': '"+NamedModel+"', 'n_type': 'class' },\n";

                // Перебор подклассов
                OntClass cla = m.getOntClass(ns + vClasse);
                if (essaClasse.hasSubClass() ) {
                    for (Iterator i = cla.listSubClasses(); i.hasNext(); ) {
                        OntClass subC = (OntClass) i.next();
                        log.info("         sub: " + subC.getLocalName() + " " + "\n");
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


        // Получить InverseFunctionalProperties
        outP = prepareProperty(m.listInverseFunctionalProperties(), NamedModel, "iprop");
        outProperty += outP[0];
        outPropertyLink += outP[1];


        // Подготовка перед выводом
        // Проще тут всё заменить чем городить \"\"\"
        outClasses = outClasses.replaceAll("'","\"");
        outProperty = outProperty.replaceAll("'","\"").substring(0, outProperty.length() - 2);
        outSubClassLink = outSubClassLink.replaceAll("'","\"");
        outPropertyLink = outPropertyLink.replaceAll("'","\"").substring(0, outPropertyLink.length() - 2);


        // Готовим вывод
        result +=  "{  \"nodes\": {";
        result +=  outClasses;
        result +=  outProperty;
        result +=  "},\"links\": [";
        result +=  outSubClassLink;
        result +=  outPropertyLink;
        result +=  "]}";

        return result;
    }


    public String[] prepareProperty (ExtendedIterator prop, String idPref, String n_type){
        String outProp = "";
        String outLink = "";
        Integer pid = 0;

        while (prop.hasNext()) {
            OntProperty p = (OntProperty) prop.next();

            if(p.getLocalName() != null && p.getDomain() != null) {
                pid++;
                String ns = p.getNameSpace();
                String classDonain = p.getDomain().getLocalName();
                String pName =  classDonain + "_"+ p.getLocalName();
                String pNameLocal =  p.getLocalName();

                log.info("Class: " + classDonain + " Prop: " + pName);

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






}
