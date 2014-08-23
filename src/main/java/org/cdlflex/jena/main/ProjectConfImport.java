package org.cdlflex.jena.main;

import org.cdlflex.jena.engine.QueryEngine;
import org.cdlflex.jena.engine.XlsxImportEngine;

public class ProjectConfImport {

    private final XlsxImportEngine importer;

    public ProjectConfImport() {
        importer = new XlsxImportEngine();
    }

    public void importModel(String excelSource, String owlTarget) {
        if (excelSource != null && owlTarget != null) {
            importer.importXlsxModelToOwl(excelSource, owlTarget);
        }
    }

    public void importXlsxData(String excelSource, String owlTarget) {
        if (excelSource != null && owlTarget != null) {
            importer.importXlsxDataToOwl(excelSource, owlTarget);
        }
    }

    public void writeToFile(String datasetURI, String target) {
        if (datasetURI != null && target != null) {
            importer.writeToFile(datasetURI, target);
        }
    }

    public static void main(String[] args) {

        String project_conf = "data/project-conf/project_conf";

        // ProjectConfImport owl = new ProjectConfImport();
        // owl.importModel(project_conf + "_model.xlsx", project_conf);
        // owl.importXlsxData(project_conf + "_data.xlsx", project_conf);
        // owl.writeToFile(project_conf, project_conf + ".owl");

        QueryEngine rampupTester = new QueryEngine(project_conf, project_conf + ".owl", null, false);

        rampupTester.QueryExec(project_conf + "0.rq", null, true);
        rampupTester.QueryExec(project_conf + "1.rq", null, true);
        rampupTester.QueryExec(project_conf + "2.rq", null, true);

    }

}
