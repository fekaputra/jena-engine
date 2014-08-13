package org.cdlflex.jena.main;

import org.cdlflex.jena.engine.XlsxImportEngine;

/**
 * Example on how to import data from excel to an ontology
 * 
 * @author Ekaputra
 * 
 */
public class XlsxImportMain {

    private final XlsxImportEngine importer;

    public XlsxImportMain() {
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
        XlsxImportMain owl = new XlsxImportMain();

        String rampup = "data/rampup/ramp";
        String esem = "data/esem/esem";
        String ske = "data/ske/ske";

        owl.importModel("data/rampup/rampup-datamodel.xlsx", rampup);
        owl.importXlsxData("data/rampup/rampup.xlsx", rampup);
        owl.writeToFile(rampup, rampup + ".owl");

        owl.importModel("data/esem/ESEM_Mapper.xlsx", esem);
        owl.importXlsxData("data/esem/ESEM_Instances.xlsx", esem);
        owl.writeToFile(esem, esem + ".owl");

        owl.importModel("data/ske/ske-mapper.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_CK.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_DM_Fixed.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_FW.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_GM.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_GM2.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_JFM.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_MA.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_MA2.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_MK.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_MR.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_MR2.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_VF.xlsx", ske);
        owl.importXlsxData("data/ske/Data Extraction_GM_PBR.xlsx", ske);
        owl.writeToFile(ske, ske + ".owl");

    }
}
