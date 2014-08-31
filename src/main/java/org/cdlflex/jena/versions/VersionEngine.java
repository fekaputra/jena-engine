package org.cdlflex.jena.versions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.cdlflex.jena.metadata.Prefix;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class VersionEngine {
    private OutputStream outputStream;
    private String ruleFile = null;
    private boolean debug = true;

    private final Dataset dataset;

    public VersionEngine(String dataset, String ruleFile, boolean reset) {
        this.dataset = TDBFactory.createDataset(dataset);
        this.ruleFile = ruleFile;
        this.setOutput(false);
        if (reset) {
            reset();
        }
    }

    public ResultSet query(String graph, String fileName, Map<String, String> params) {
        ParameterizedSparqlString query = new ParameterizedSparqlString();
        formulateQuery(query, fileName);
        if (params != null && !params.isEmpty())
            setParam(query, params);
        return query(graph, query.toString());
    }

    public ResultSet query(String graph, String query) {
        ResultSet rs = null;
        dataset.begin(ReadWrite.READ);

        if (dataset.containsNamedModel(graph)) {
            Model m = dataset.getNamedModel(graph);
            OntModel model = initOntModel(ruleFile, m);
            ParameterizedSparqlString qString = new ParameterizedSparqlString(query);
            qString.setNsPrefixes(dataset.getDefaultModel().getNsPrefixMap());

            rs = doQueryExec(qString, model);
        }
        dataset.end();

        return rs;
    }

    public void insertData(String user, String docID, Model model) {
        dataset.begin(ReadWrite.WRITE);
        String versionID = VersionHelper.generateKey(dataset.getDefaultModel().getNsPrefixURI(""), user, docID);

        Model temp;
        if (dataset.containsNamedModel(versionID)) {
            temp = dataset.getNamedModel(versionID);
            temp.add(model);
            temp.close();
        } else {
            dataset.addNamedModel(versionID, model);
            addMetadata(VersionHelper.transformToKey(user), VersionHelper.transformToKey(docID), versionID);
        }
        dataset.commit();
        dataset.end();
    }

    public OntModel initOntModel(String ruleFile, Model m) {
        OntModel model;
        if (ruleFile != null) {
            Reasoner reasoner = new GenericRuleReasoner(VersionHelper.readRules(m.getNsPrefixMap(), ruleFile));
            reasoner = reasoner.bindSchema(m);
            OntModelSpec ontModelSpec = OntModelSpec.OWL_DL_MEM;
            ontModelSpec.setReasoner(reasoner);
            model = ModelFactory.createOntologyModel(ontModelSpec, m);
        } else {
            OntModelSpec ontModelSpec = OntModelSpec.OWL_DL_MEM;
            model = ModelFactory.createOntologyModel(ontModelSpec, m);
        }
        return model;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Map<String, String> getDefaultPrefixes() {
        dataset.begin(ReadWrite.READ);
        Map<String, String> prefixes = dataset.getDefaultModel().getNsPrefixMap();
        dataset.end();
        return prefixes;
    }

    public void writeToFile(String URI, String graph) {
        dataset.begin(ReadWrite.READ);
        if (dataset.containsNamedModel(graph)) {
            VersionHelper.writeFile(dataset.getNamedModel(graph), URI);
        } else if (graph == "") {
            VersionHelper.writeFile(dataset.getDefaultModel(), URI);
        }
        dataset.end();
    }

    private void formulateQuery(ParameterizedSparqlString qString, String fileName) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String brLine;
            while ((brLine = br.readLine()) != null) {
                if (brLine.startsWith("#")) {
                    if (debug)
                        outputStream.write(brLine.concat("\n").getBytes());
                } else {
                    qString.append(brLine);
                    qString.append(System.getProperty("line.separator"));
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setParam(ParameterizedSparqlString qString, Map<String, String> params) {
        Iterator<String> paramIter = params.keySet().iterator();
        while (paramIter.hasNext()) {
            String key = paramIter.next();
            String value = params.get(key);
            qString.setParam(key, ResourceFactory.createPlainLiteral(value));
        }
    }

    private void reset() {
        dataset.begin(ReadWrite.WRITE);
        Model model = dataset.getDefaultModel();
        model.removeAll();
        model.setNsPrefix("", Prefix.SKE);
        model.setNsPrefix("foaf", FOAF.getURI());
        model.setNsPrefix("rdf", RDF.getURI());
        model.setNsPrefix("rdfs", RDFS.getURI());
        model.setNsPrefix("owl", OWL.getURI());
        model.commit();
        Iterator<String> iterator = dataset.listNames();
        while (iterator.hasNext()) {
            dataset.removeNamedModel(iterator.next());
        }
        dataset.commit();
        dataset.end();
    }

    private ResultSet doQueryExec(ParameterizedSparqlString qString, OntModel model) {
        ResultSet rs = null;
        if (debug)
            System.out.println(qString.toString());

        long execTime = System.currentTimeMillis();
        QueryExecution qe = QueryExecutionFactory.create(qString.asQuery(), model);
        ResultSet resultSet = qe.execSelect();
        execTime = System.currentTimeMillis() - execTime;
        rs = ResultSetFactory.copyResults(resultSet);

        try {
            outputStream.write(("execution time: " + Long.toString(execTime) + " ms \n\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        qe.close();
        return rs;
    }

    private void addMetadata(String user, String docID, String versionID) {

        Model defaultModel = dataset.getDefaultModel();
        Resource userRes = defaultModel.createResource(defaultModel.getNsPrefixURI("") + user, FOAF.Person);
        Resource docRes = defaultModel.createResource(defaultModel.getNsPrefixURI("") + docID, FOAF.Document);
        docRes.addProperty(FOAF.homepage, versionID);
        userRes.addProperty(FOAF.made, docRes);
        defaultModel.close();
    }

    /**
     * Set output file; whether you want it to be saved into a file or printed out.
     * 
     * @param isUsingFile
     */
    private void setOutput(boolean isUsingFile) {
        if (!isUsingFile) {
            outputStream = System.out;
        } else {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd:MM:yyyy_HH:mm");
                Date resultdate = new Date(System.currentTimeMillis());
                outputStream = new FileOutputStream("data/result" + sdf.format(resultdate) + ".txt");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

}
