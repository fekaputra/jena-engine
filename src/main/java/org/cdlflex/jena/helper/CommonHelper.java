package org.cdlflex.jena.helper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXParser;
import org.jbibtex.ParseException;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.FileManager;

public class CommonHelper {

    public static final String MAPPING_SHEET = "Ontology Mapping";
    public static final String CONCEPT_SHEET = "Concepts";
    public static String DEFAULT_NS = "http://www.cdl.ifs.tuwien.ac.at/xlsx2onto?#";

    public static Dataset readFile(String URL) {
        return TDBFactory.createDataset(URL);
    }

    public static String readOwlFile(Dataset dataset, String owlFile) {
        String defURI = "";
        datasetWait(dataset);
        dataset.begin(ReadWrite.WRITE);

        Model model = dataset.getDefaultModel();
        if (owlFile != null) {
            model.read(owlFile);
        }
        defURI = model.getNsPrefixURI("");

        dataset.commit();
        dataset.end();

        return defURI;
    }

    public static OntModel readOrCreateFile(String URL) {
        OntModel model = ModelFactory.createOntologyModel();
        try {
            InputStream in = FileManager.get().open(URL);
            if (in != null)
                model.read(in, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (model.getNsPrefixURI("") == null) {
                model.setNsPrefix("", DEFAULT_NS.replace("?", ""));
            }
        }
        return model;
    }

    public static OntModel create() {
        OntModel model = ModelFactory.createOntologyModel();
        return model;
    }

    public static void writeFile(OntModel model, String URL) {

        try {
            FileOutputStream fileOut = new FileOutputStream(URL);
            model.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write default model to an output file
     * 
     * @param dataset
     * @param URL
     */
    public static void writeFile(Dataset dataset, String URL) {
        datasetWait(dataset);
        dataset.begin(ReadWrite.READ);

        Model model = dataset.getDefaultModel();
        try {
            FileOutputStream fileOut = new FileOutputStream(URL);
            model.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            model.close();
            dataset.end();
        }
    }

    public static BibTeXDatabase parseBibTeX(String bibTexString) throws IOException, ParseException {
        Reader reader = new StringReader(bibTexString);
        BibTeXParser parser = new BibTeXParser();
        return parser.parse(reader);
    }

    private static void datasetWait(Dataset dataset) {
        while (dataset.isInTransaction()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
