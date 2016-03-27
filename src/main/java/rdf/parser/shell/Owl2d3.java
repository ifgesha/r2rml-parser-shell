package rdf.parser.shell;

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 */
public class Owl2d3 {

    private static final Logger log = LoggerFactory.getLogger(Owl2d3.class);

    // Каждому классу и свойству даётся уникальный номер.
    // Тут храним хеш элементов онтологии.  <имя_элемента_в_онтологии, номер_в_элемента_в_выходном вайле>
    Map<String, Integer> hm;
    {
        hm = new HashMap<String, Integer>();
    }


    Integer nid = 0;


    public Model ReadModelFromFile(String modelFile, String ns ) {
        Model m;

        InputStream isMap = FileManager.get().open(modelFile);
        m=ModelFactory.createDefaultModel();
        try{
            m.read(isMap, ns);
        }
        catch( Exception e){
            String err = "Error reading file " + modelFile + e.toString()+"\n";
            log.error(err);
            throw new RuntimeException(err);
        }


        return m;

    }


    public String MakeD3Json (String NamedModel, Model model) {

         log.info("MakeD3Json");

        String result = "";
        String outClasses = "";
        String outSubClassLink = "";
        String outProperty = "";
        String outPropertyLink = "";
        String outList = "";
        nid = 0;


        // Создать онтологию на основе модели
        OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_MEM );
        OntModel m =  ModelFactory.createOntologyModel(spec,  model);

/*
        ExtendedIterator _ns = m.listNameSpaces();
        while (_ns.hasNext()) {
            String NameSpace = (String) _ns.next();
            System.out.println("ns: "+NameSpace);
        }
        ns: http://www.w3.org/1999/02/22-rdf-syntax-ns#
        ns: http://www.w3.org/2002/07/owl#
        ns: http://www.w3.org/2000/01/rdf-schema#
*/

        // Итератор классов модели
        ExtendedIterator classes = m.listClasses();

        // Сформировать классы
        while (classes.hasNext()) {
            OntClass Class = (OntClass) classes.next();
            if(Class.getLocalName() != null) {
                nid++;
                String ns = Class.getNameSpace();
                String className = Class.getLocalName();
                String c_id = NamedModel + "_id" + nid;
                String fullName = ns + className;

                // Добавим класс
                if(!hm.containsKey(fullName)){
                    hm.put(fullName, nid);
                    outClasses += "'"+c_id+"':{'id':'"+c_id+"', 'name':'"+className+ "', 'ont':'"+NamedModel+"', 'n_type':'class', 'ns':'"+ns+"' },\n";
                }
                log.info("Class: " + className);
            }
        }


        // Получить связи класс - подкласс
        classes = m.listClasses();
        while (classes.hasNext()) {
            OntClass Class = (OntClass) classes.next();
            if(Class.getLocalName() != null) {

                String ns = Class.getNameSpace();
                String className = Class.getLocalName();
                String fullNameClass = ns + className;

                // Перебор подклассов
                OntClass sClass = m.getOntClass(fullNameClass);
                if (Class.hasSubClass() ) for (Iterator i = sClass.listSubClasses(); i.hasNext(); ) {
                    OntClass sc = (OntClass) i.next();

                    try {
                        nid++;
                        String fullNameSubClass = ns + sc.getLocalName();
                        String p_id = NamedModel + "_id" + nid;
                        String c_id = NamedModel + "_id" + hm.get(fullNameClass).toString();
                        String s_id = NamedModel + "_id" + hm.get(fullNameSubClass).toString();

                        // Создать свойство "подкласс"
                        outProperty += "'" + p_id + "':{'id': '" + p_id + "', 'name': 'Subclass of', 'ont': '" + NamedModel + "', 'n_type':'subclass', 'ns':'"+ns+"'  },\n";
                        hm.put("subclass", nid);

                        // Создать связи  класс--свойство--субкласс
                        outSubClassLink += "{ 'source':'" + s_id + "', 'target': '" + p_id + "', 'l_type':'subclass'},\n";
                        outSubClassLink += "{ 'source':'" + p_id + "', 'target': '" + c_id + "', 'l_type':'subclass'},\n";
                        log.info("Subclass link: " + sc.getLocalName() + " " + "\n");

                    } catch( Exception e) {
                        log.error(" Error create Subclass Link for  " + className);
                    }
                }
            }
        }



        // Получить ObjectProperties
        String[] outP = prepareProperty(m.listObjectProperties(), NamedModel, "oprop");
        outProperty += outP[0];
        outPropertyLink += outP[1];
        outList += outP[2];

        // Получить InverseFunctionalProperties
        outP = prepareProperty(m.listInverseFunctionalProperties(), NamedModel, "iprop");
        outProperty += outP[0];
        outPropertyLink += outP[1];
        outList += outP[2];

        // Получить DatatypeProperties (получаем последними)
        outP = prepareProperty(m.listDatatypeProperties(), NamedModel, "dprop");
        outProperty += outP[0];
        outPropertyLink += outP[1];
        outList += outP[2];


        // Подготовка перед выводом
        // Проще тут всё заменить чем городить \"\"\"
        outClasses = outClasses.replaceAll("'","\"");

        if(outProperty.length() > 0 )
        outProperty = outProperty.replaceAll("'","\"").substring(0, outProperty.length() - 2);

        outSubClassLink = outSubClassLink.replaceAll("'","\"");

        if(outPropertyLink.length() > 0 )
        outPropertyLink = outPropertyLink.replaceAll("'","\"").substring(0, outPropertyLink.length() - 2);

        if(outList.length() > 0 )
        outList = outList.replaceAll("'","\"").substring(0, outList.length() - 2);


        // Готовим вывод
        result +=  "{  \"nodes\": \n{";
        result +=  outClasses;
        result +=  outProperty;
        result +=  "}\n,\"links\": \n[";
        result +=  outSubClassLink;
        result +=  outPropertyLink;
        //result +=  "]}";
        result +=  "]\n,\"list\": \n[";
        result +=  outList;
        result +=  "]}";
        return result;
    }




    public String[] prepareProperty (ExtendedIterator prop, String NamedModel, String n_type){
        String outProp = "";
        String outLink = "";
        String outList = "";

        while (prop.hasNext()) {
            OntProperty p = (OntProperty) prop.next();
            if (p.getLocalName() != null && p.getDomain() != null ) {
                String ns = p.getNameSpace();
                String propLocalName = p.getLocalName();

                // Перебрать все домены (у свойства их может быть несколько)
                ExtendedIterator d = p.listDomain();

                while (d.hasNext()) {
                    OntResource domain  = (OntResource) d.next();

                    nid++;
                    String DomainName = domain.getLocalName();
                    String classRange = "";
                    String p_id = NamedModel + "_id" + nid;
                    String d_id = NamedModel + "_id" + hm.get(domain.getNameSpace()+domain.getLocalName());

                    // Свойство
                   // if(!hm.containsKey(domain.getNameSpace()+propLocalName)) {
                        outProp += "'" + p_id + "':{'id': '" + p_id + "', 'name': '" + propLocalName + "', 'ont': '" + NamedModel + "', 'n_type': '" + n_type + "', 'ns':'"+ns+"' },\n";
                   //     hm.put(domain.getNameSpace()+propLocalName, nid);

                        // Линки
                        if (n_type.equals("oprop") && p.getRange() != null) {
                            classRange = p.getRange().getLocalName();
                            String r_id = NamedModel + "_id" + hm.get(p.getRange().getNameSpace() + p.getRange().getLocalName());
                            //String r_id = ns + classRange;
                            outLink += "{ 'source':'" + p_id + "', 'target': '" + r_id + "', 'l_type':'" + n_type + "'},\n";
                            outLink += "{ 'source':'" + d_id + "', 'target': '" + p_id + "', 'l_type':'" + n_type + "'},\n";
                        } else {
                            outLink += "{ 'source':'" + p_id + "', 'target': '" + d_id + "', 'l_type':'" + n_type + "'},\n";
                        }


                        outList += "{ 'name':'" + p.getLocalName() + "', 'domain': '" + DomainName + "', 'range': '" + classRange + "', 'n_type':'" + n_type + "'},\n";


                        log.info("Class: " + DomainName + " Prop: " + p.getLocalName());
                  //  }
                }

            }


        }
        return new String[] {outProp, outLink, outList};
    }






}
