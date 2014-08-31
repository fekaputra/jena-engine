package org.cdlflex.jena.main;

import java.util.Map;

import org.cdlflex.jena.versions.VersionEngine;
import org.cdlflex.jena.versions.VersionHelper;
import org.cdlflex.jena.versions.VersionImport;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class VersioningMain {

    public static void main(String[] args) {
        String user = "fajar";
        String document1 = "doc1";
        String document2 = "doc2";
        String ske = "data/ske/ske";

        // instantiate
        VersionImport importer = new VersionImport();
        VersionEngine engine = new VersionEngine("data/version", null, true);

        // import data
        Map<String, String> prefixes = engine.getDefaultPrefixes();
        Model m1 = importer.importXlsx("data/ske/ske-mapper.xlsx", "data/ske/Data Extraction_CK.xlsx", prefixes);
        Model m2 =
            importer.importXlsx("data/ske/ske-mapper.xlsx", "data/ske/Data Extraction_DM_Fixed.xlsx", prefixes);
        Model m3 = importer.importXlsx("data/ske/ske-mapper.xlsx", "data/ske/Data Extraction_FW.xlsx", prefixes);
        Model m4 = importer.importXlsx("data/ske/ske-mapper.xlsx", "data/ske/Data Extraction_GM_PBR.xlsx", prefixes);

        // To check: VersionHelper.writeFile(m1, "data/version/version.owl");

        // put the imported data according to username and document-id (generated from CSt App)
        engine.insertData(user, document1, m1);
        engine.insertData(user, document1, m2);

        String namedGraphKey1 = VersionHelper.generateKey(engine.getDefaultPrefixes().get(""), user, document1);
        engine.query(namedGraphKey1, ske + "1.rq", null);
        engine.query(namedGraphKey1, ske + "2.rq", null);
        engine.query(namedGraphKey1, ske + "3.rq", null);
        engine.query(namedGraphKey1, ske + "4.rq", null);
        engine.query(namedGraphKey1, ske + "5.rq", null);
        engine.query(namedGraphKey1, ske + "6.rq", null);
        engine.query(namedGraphKey1, ske + "7.rq", null);
        engine.query(namedGraphKey1, ske + "8.rq", null);

        engine.insertData(user, document2, m3);
        engine.insertData(user, document2, m4);

        String namedGraphKey2 = VersionHelper.generateKey(engine.getDefaultPrefixes().get(""), user, document2);
        ResultSet rs1 = engine.query(namedGraphKey2, ske + "1.rq", null);
        ResultSet rs2 = engine.query(namedGraphKey2, ske + "2.rq", null);
        ResultSet rs3 = engine.query(namedGraphKey2, ske + "3.rq", null);
        ResultSet rs4 = engine.query(namedGraphKey2, ske + "4.rq", null);
        ResultSet rs5 = engine.query(namedGraphKey2, ske + "5.rq", null);
        ResultSet rs6 = engine.query(namedGraphKey2, ske + "6.rq", null);
        ResultSet rs7 = engine.query(namedGraphKey2, ske + "7.rq", null);
        ResultSet rs8 = engine.query(namedGraphKey2, ske + "8.rq", null);

        // To check the main graph
        engine.writeToFile("data/version/engine.owl", "");
    }
}
