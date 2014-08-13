package org.cdlflex.jena.engine;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.cdlflex.jena.helper.CommonHelper;
import org.cdlflex.jena.helper.QueryHelper;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;

/**
 * QueryEngine Designed to work with a custom inference engine & queries in
 * files
 * 
 */
public class QueryEngine {
    private OutputStream outputStream;
    private OntModel model;

    private final Dataset dataset;
    private final String defaultURI;
    private final String ruleFile;
    private final String outputFileFolder = "data/result/";

    /**
     * 
     * @param datasetLocation TDB dataset location.
     * @param owlFile owlfile name.
     * @param ruleFile custom rule file.
     * @param isUsingFile whether the output should go to file or printed out.
     */
    public QueryEngine(String datasetLocation, String owlFile, String ruleFile, boolean isUsingFile) {
        dataset = CommonHelper.readFile(datasetLocation);
        defaultURI = CommonHelper.readOwlFile(dataset, owlFile);
        this.ruleFile = ruleFile;
        setOutput(isUsingFile);
    }

    /**
     * Query executor.
     * 
     * @param fileName query file name
     * @param params parameters to replace params in the query. E.g., if you
     *        have a pair of string "pou" & "T_Pump", it will replace all ?pou
     *        with string "T_Pump"
     * @param debug if this is true, will print SPARQL query in the terminal
     */
    public void QueryExec(String fileName, Map<String, String> params, boolean debug) {
        dataset.begin(ReadWrite.READ);

        initRules(ruleFile);
        ParameterizedSparqlString qString = new ParameterizedSparqlString();
        qString.setNsPrefixes(model.getNsPrefixMap());
        formulateQuery(qString, fileName);
        if (params != null && !params.isEmpty())
            setParam(qString, params);
        doQueryExec(fileName, qString, debug);

        model.close();
        dataset.end();
    }

    /**
     * Export model to a file.
     * 
     * @param filename
     */
    public void exportModel(String filename) {
        dataset.begin(ReadWrite.READ);
        initRules(ruleFile);

        try (FileWriter writer = new FileWriter(filename)) {
            model.writeAll(writer, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.close();
        dataset.end();
    }

    /**
     * DEMO purpose: show OntResource
     * 
     * @param URI
     */
    public void showOntResource(String URI) {
        dataset.begin(ReadWrite.READ);
        initRules(ruleFile);

        OntResource resource = model.getOntResource(defaultURI + URI);
        if (resource == null) {
            try {
                outputStream.write(("there is no resource: " + URI).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        } else if (resource.isIndividual()) {
            showIndividual(resource.asIndividual());
        } else if (resource.isClass()) {
            showClass(resource.asClass());
        }

        model.close();
        dataset.end();
    }

    /**
     * If resource is an individual (i.e., instance of class), show list of its
     * properties
     * 
     * @param instance
     */
    private void showIndividual(Individual instance) {
        StmtIterator stmtIter = instance.listProperties();
        try {
            outputStream.write(("Instance Name: " + instance.getLocalName() + "\n").getBytes());
            while (stmtIter.hasNext()) {
                outputStream.write((stmtIter.next().toString() + "\n").getBytes());
            }
            outputStream.write(("\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * If resource is a class, show list of its properties
     * 
     * @param cls
     */
    private void showClass(OntClass cls) {
        StmtIterator stmtIter = cls.listProperties();
        try {
            outputStream.write(("Class Name: " + cls.getLocalName() + "\n").getBytes());
            while (stmtIter.hasNext()) {
                outputStream.write((stmtIter.next().toString() + "\n").getBytes());
            }
            outputStream.write(("\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showListOfProperty(String URI, String propURI) {
        dataset.begin(ReadWrite.READ);
        initRules(ruleFile);

        Individual ind = model.getIndividual(defaultURI + URI);
        OntProperty prop = model.getOntProperty(defaultURI + propURI);
        try {
            outputStream.write(("Instance Name: " + ind.getLocalName() + "\n").getBytes());
            outputStream.write(("Property Name: " + prop.getLocalName() + "\n").getBytes());
            Iterator<RDFNode> iter = ind.listPropertyValues(prop);
            while (iter.hasNext()) {
                RDFNode node = iter.next();
                outputStream.write(("Props: " + node.toString() + "\n").getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        model.close();
        dataset.end();
    }

    /**
     * Set output file; whether you want it to be saved into a file or printed
     * out.
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
                outputStream = new FileOutputStream(outputFileFolder + sdf.format(resultdate) + ".txt");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Read query from a file, omit comments (and printed it out), and put it in
     * a ParameterizedSparqlString.
     * 
     * @param qString
     * @param fileName
     */
    private void formulateQuery(ParameterizedSparqlString qString, String fileName) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String brLine;
            while ((brLine = br.readLine()) != null) {
                if (brLine.startsWith("#")) {
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

    /**
     * The real execution of the query. Put the result directly to the outstream
     * that is defined in the setOutput() method.
     * 
     * @param fileName
     * @param qString
     * @param debug
     */
    private void doQueryExec(String fileName, ParameterizedSparqlString qString, boolean debug) {
        if (debug)
            System.out.println(qString.toString());

        long execTime = System.currentTimeMillis();
        QueryExecution qe = QueryExecutionFactory.create(qString.asQuery(), model);
        ResultSet rs = qe.execSelect();
        // ResultSetFormatter.out(outputStream, rs);
        ResultSetFormatter.outputAsJSON(outputStream, rs);
        execTime = System.currentTimeMillis() - execTime;
        try {
            outputStream.write(("execution time: " + Long.toString(execTime) + " ms \n\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        qe.close();
    }

    /**
     * It is possible to put parameter to the query using this function. The
     * parameters should be structured as a Map.
     * 
     * @param qString
     * @param params
     */
    private void setParam(ParameterizedSparqlString qString, Map<String, String> params) {
        Iterator<String> paramIter = params.keySet().iterator();
        while (paramIter.hasNext()) {
            String key = paramIter.next();
            String value = params.get(key);
            qString.setParam(key, ResourceFactory.createPlainLiteral(value));
        }
    }

    /**
     * 
     * If there is rule file associated with the ontology, this function execute
     * the rule file (and maybe built-in reasoner) to calculate the resulted
     * facts.
     * 
     * @param ruleFile
     */
    private void initRules(String ruleFile) {
        Model m = dataset.getDefaultModel();
        if (ruleFile != null) {
            Reasoner reasoner = new GenericRuleReasoner(QueryHelper.readRules(defaultURI, ruleFile));
            reasoner = reasoner.bindSchema(m);
            OntModelSpec ontModelSpec = OntModelSpec.OWL_DL_MEM;
            ontModelSpec.setReasoner(reasoner);
            model = ModelFactory.createOntologyModel(ontModelSpec, m);
        } else {
            OntModelSpec ontModelSpec = OntModelSpec.OWL_DL_MEM;
            model = ModelFactory.createOntologyModel(ontModelSpec, m);
        }
    }
}
