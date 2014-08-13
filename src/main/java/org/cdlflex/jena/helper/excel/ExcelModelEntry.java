package org.cdlflex.jena.helper.excel;

import java.util.ArrayList;
import java.util.List;

public class ExcelModelEntry {
	private String excelColumnName;
	private String ontoProperty;
	private String key;
	private String datatype;
	private Integer	columnNumber;
	private Integer cardinality;
	private List<String> allowedValues;
	private List<String> additionalProperties;
	
	public ExcelModelEntry() {
		excelColumnName = null;
		ontoProperty = null;
		key = null;
		datatype = null;
		columnNumber = null;
		cardinality = null;
		allowedValues = new ArrayList<String>();
		additionalProperties = new ArrayList<String>();
	}
	
	public ExcelModelEntry(String colName, String ontoProp, String key, String datatype, 
			int colNum, int cardinality, List<String> allowedValues, List<String> additionalProperties) {
		excelColumnName = colName;
		ontoProperty = ontoProp;
		this.key = key;
		this.datatype = datatype;
		columnNumber = colNum;
		this.cardinality = cardinality;
		this.allowedValues = allowedValues;
		this.additionalProperties = additionalProperties;
	}
	
	public String getExcelColumnName() {
		return excelColumnName;
	}
	public void setExcelColumnName(String excelColumnName) {
		this.excelColumnName = excelColumnName;
	}
	public String getOntoProperty() {
		return ontoProperty;
	}
	public void setOntoProperty(String ontoProperty) {
		this.ontoProperty = ontoProperty;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getDatatype() {
		return datatype;
	}
	public void setDatatype(String datatype) {
		this.datatype = datatype;
	}
	public int getColumnNumber() {
		return columnNumber;
	}
	public void setColumnNumber(int columnNumber) {
		this.columnNumber = columnNumber;
	}
	public int getCardinality() {
		return cardinality;
	}
	public void setCardinality(int cardinality) {
		this.cardinality = cardinality;
	}
	public List<String> getAllowedValues() {
		return allowedValues;
	}
	public void setAllowedValues(List<String> allowedValues) {
		this.allowedValues = allowedValues;
	}

	public List<String> getAdditionalProperties() {
		return additionalProperties;
	}

	public void setAdditionalProperties(List<String> additionalProperties) {
		this.additionalProperties = additionalProperties;
	}
	
}
