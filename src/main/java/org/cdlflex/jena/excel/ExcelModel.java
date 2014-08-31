package org.cdlflex.jena.excel;

import java.util.HashMap;

public class ExcelModel extends HashMap<Integer, ExcelModelEntry> {

    private static final long serialVersionUID = 1410627376193697451L;

    public static enum ExcelModelComponent {
        EXCEL_SHEET_NAME, EXCEL_CONCEPT_NAME, EXCEL_COLUMN_NAME, EXCEL_ONTO_PROPERTY, EXCEL_ONTO_KEY, EXCEL_ONTO_DATATYPE, EXCEL_COLUMN_NUMBER, EXCEL_ONTO_CARDINALITY, EXCEL_ONTO_ALLOWED_VALUES
    }

    String sheetName;
    String ontoConcept;
    Integer primaryKey;

    public ExcelModel() {
        super();
        sheetName = null;
        ontoConcept = null;
        primaryKey = null;
    }

    public ExcelModel(String name, String concept, Integer key) {
        super();
        sheetName = name;
        ontoConcept = concept;
        primaryKey = key;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public String getOntoConcept() {
        return ontoConcept;
    }

    public void setOntoConcept(String ontoConcept) {
        this.ontoConcept = ontoConcept;
    }

    public Integer getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(Integer primaryKey) {
        this.primaryKey = primaryKey;
    }

}
