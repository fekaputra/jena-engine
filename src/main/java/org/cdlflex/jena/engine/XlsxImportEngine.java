package org.cdlflex.jena.engine;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.cdlflex.jena.helper.CommonHelper;
import org.cdlflex.jena.helper.excel.ExcelHelper;
import org.cdlflex.jena.helper.excel.ExcelModel;
import org.cdlflex.jena.helper.excel.ExcelModelEntry;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.ParseException;
import org.jbibtex.Value;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Hello world!
 * 
 */
public class XlsxImportEngine {
    Map<String, ExcelModel> modelMapper = new HashMap<String, ExcelModel>();
    Dataset dataset;
    XSSFWorkbook workbook;

    public XlsxImportEngine() {
        dataset = null;
        workbook = null;
    }

    public void importXlsxModelToOwl(String excelSource, String owlTarget) {
        init(excelSource, owlTarget);
        importModel();
        System.out.println("Successfully importing '" + excelSource + "' Model into '" + owlTarget);
    }

    public void importXlsxDataToOwl(String excelSource, String owlTarget) {
        init(excelSource, owlTarget);
        importData();
        System.out.println("Successfully importing '" + excelSource + "' Model into '" + owlTarget);
    }

    public void writeToFile(String datasetURI, String owlTarget) {
        Dataset temp = CommonHelper.readFile(datasetURI);
        CommonHelper.writeFile(temp, owlTarget);
    }

    private void init(String excelSource, String owlTarget) {
        dataset = CommonHelper.readFile(owlTarget);
        workbook = ExcelHelper.readFile(excelSource);
        storeModelMap(workbook);
    }

    private Resource createModelClass(Model model, String className, String superClassName) {

        Resource owlThing = model.createResource(model.getNsPrefixURI("owl") + "Class");
        Resource cls = model.createResource(model.getNsPrefixURI("") + className, owlThing);
        if (superClassName != null) {
            Resource superCls = model.createResource(model.getNsPrefixURI("") + superClassName);
            cls.addProperty(model.createProperty(model.getNsPrefixURI("rdfs") + "subClassOf"), superCls);
        }

        return cls;
    }

    private Property createDatatypeProperty(Model model, Resource domain, Resource range, String propertyName) {
        Property property = model.createProperty(model.getNsPrefixURI(""), propertyName);

        Property propertyDomain = model.createProperty(model.getNsPrefixURI("rdfs") + "domain");
        Property propertyRange = model.createProperty(model.getNsPrefixURI("rdfs") + "range");
        Property rdfType = model.createProperty(model.getNsPrefixURI("rdf") + "type");

        Resource propType = model.createResource(model.getNsPrefixURI("owl") + "DatatypeProperty");

        model.add(model.createStatement(property, propertyDomain, domain));
        model.add(model.createStatement(property, rdfType, propType));
        if (range != null)
            model.add(model.createStatement(property, propertyRange, range));

        return property;
    }

    private Property createObjectProperty(Model model, Resource domain, Resource range, String propertyName) {
        Property property = model.createProperty(model.getNsPrefixURI(""), propertyName);

        Property propertyDomain = model.createProperty(model.getNsPrefixURI("rdfs") + "domain");
        Property propertyRange = model.createProperty(model.getNsPrefixURI("rdfs") + "range");
        Property rdfType = model.createProperty(model.getNsPrefixURI("rdf") + "type");

        Resource propType = model.createResource(model.getNsPrefixURI("owl") + "ObjectProperty");

        model.add(model.createStatement(property, propertyDomain, domain));
        model.add(model.createStatement(property, rdfType, propType));
        if (range != null)
            model.add(model.createStatement(property, propertyRange, range));

        return property;
    }

    private void setFunctionalProperty(Model model, Property property) {
        Property rdfType = model.createProperty(model.getNsPrefixURI("rdf") + "type");
        Resource functional = model.createResource(model.getNsPrefixURI("owl") + "FunctionalProperty");
        model.add(model.createStatement(property, rdfType, functional));
    }

    private void importModel() {
        dataset.begin(ReadWrite.WRITE);
        Model model = dataset.getDefaultModel();
        setPrefix(model);

        for (String sheetName : modelMapper.keySet()) {
            ExcelModel sheetModel = modelMapper.get(sheetName);
            Resource sheetClass = createModelClass(model, sheetModel.getOntoConcept(), null);
            for (Integer sheetColumnNumber : sheetModel.keySet()) {
                ExcelModelEntry modelEntry = sheetModel.get(sheetColumnNumber);
                Property property;
                Resource classRange;

                if (modelEntry.getKey().equalsIgnoreCase("fk")) {
                    classRange = createModelClass(model, modelEntry.getDatatype(), null);
                    property = createObjectProperty(model, sheetClass, classRange, modelEntry.getOntoProperty());
                    if (!modelEntry.getAdditionalProperties().isEmpty()) {
                        createAddPropResource(model, property, modelEntry.getAdditionalProperties());
                    }
                } else {
                    if (modelEntry.getDatatype().equalsIgnoreCase("int")) {
                        classRange = XSD.xint;
                    } else if (modelEntry.getDatatype().equalsIgnoreCase("Date")) {
                        classRange = XSD.date;
                    } else {
                        classRange = XSD.xstring;
                    }
                    property = createDatatypeProperty(model, sheetClass, classRange, modelEntry.getOntoProperty());
                    if (modelEntry.getKey().equalsIgnoreCase("pk")) {
                        setFunctionalProperty(model, property);
                    }
                }
            }
        }

        model.close();
        dataset.commit();
        dataset.end();
    }

    private void setPrefix(Model model) {
        model.setNsPrefix("", "http://cdlflex.org/ontology.owl#");
        model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
    }

    private void createAddPropResource(Model model, Resource ontClass, List<String> additionalProps) {
        Iterator<String> propsIterator = additionalProps.iterator();
        while (propsIterator.hasNext()) {
            String[] props = propsIterator.next().split("=");
            createDatatypeProperty(model, ontClass, XSD.xstring, props[0]);
        }
    }

    private void importData() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        for (XSSFSheet xssfSheet : workbook) {
            if (xssfSheet.getSheetName().equalsIgnoreCase(CommonHelper.MAPPING_SHEET))
                continue;
            processSheet(xssfSheet);
            System.out.println("At " + dateFormat.format(Calendar.getInstance().getTime()) + " done "
                    + xssfSheet.getSheetName());
        }
    }

    private void processSheet(XSSFSheet sheet) {

        ExcelModel map = modelMapper.get(sheet.getSheetName());
        if (map == null)
            return;

        while (dataset.isInTransaction()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        dataset.begin(ReadWrite.WRITE);
        Model model = dataset.getDefaultModel();

        for (Row row : sheet) {
            Cell primaryKeyCell = row.getCell(map.getPrimaryKey());
            if (primaryKeyCell == null)
                continue;

            Cell c = row.getCell(map.getPrimaryKey());
            c.setCellType(Cell.CELL_TYPE_STRING);

            if (c.getStringCellValue().isEmpty() || (row.getRowNum() == 0))
                continue;

            // TODO: to be separated into processRow()

            Resource concept = model.getResource(model.getNsPrefixURI("") + map.getOntoConcept());
            String PK = transformToKey(concept.getLocalName(), row.getCell(map.getPrimaryKey()).getStringCellValue());
            Resource instance = model.createResource(model.getNsPrefixURI("") + PK, concept);

            for (int i : map.keySet()) {
                ExcelModelEntry mapEntry = map.get(i);
                Cell cell = row.getCell(mapEntry.getColumnNumber());
                if (cell == null)
                    continue;
                List<Statement> listStmt = processCell(model, instance, mapEntry, cell);
                if (listStmt.isEmpty())
                    continue;
                model.add(listStmt);
            }
        }

        model.close();
        dataset.commit();
        dataset.end();
    }

    private List<Statement> processCell(Model model, Resource instance, ExcelModelEntry mapEntry, Cell cell) {
        List<Statement> listStatement = new ArrayList<Statement>();
        Property instanceProperty = model.getProperty(model.getNsPrefixURI("") + mapEntry.getOntoProperty());
        cell.setCellType(Cell.CELL_TYPE_STRING);

        String instancePropertyValues = cell.getStringCellValue().trim();
        RDFNode instancePropertyObject = null;

        if (mapEntry.getKey().equalsIgnoreCase("bib")) {
            instancePropertyObject = ResourceFactory.createPlainLiteral(instancePropertyValues);
            createBibTexModelEntry(model, instance, instancePropertyValues);
        } else {
            String[] values = instancePropertyValues.split(";");
            for (int i = 0; i < values.length; i++) {
                String instancePropertyValue = values[i];
                if (instancePropertyValue.equalsIgnoreCase("BLANK"))
                    continue;

                if (mapEntry.getKey().equalsIgnoreCase("fk")) {

                    Resource domain = instanceProperty.getPropertyResourceValue(model.createProperty(model
                            .getNsPrefixURI("rdfs") + "range"));
                    instancePropertyValue = transformToKey(domain.getLocalName(), instancePropertyValue);
                    instancePropertyObject = model.createResource(model.getNsPrefixURI("") + instancePropertyValue,
                            domain);

                    if (!mapEntry.getAdditionalProperties().isEmpty()) {
                        // TODO: HARDCODE
                        Property bokTopicID = createDatatypeProperty(model, domain, XSD.xstring, "bokTopicID");
                        listStatement.add(ResourceFactory.createStatement(instancePropertyObject.asResource(),
                                bokTopicID, ResourceFactory.createPlainLiteral(values[i].trim())));
                        Iterator<String> propsIterator = mapEntry.getAdditionalProperties().iterator();
                        while (propsIterator.hasNext()) {
                            String[] props = propsIterator.next().split("=");
                            Property prop = createDatatypeProperty(model, instancePropertyObject.asResource(),
                                    XSD.xstring, props[0]);
                            model.add(instancePropertyObject.asResource(), prop,
                                    ResourceFactory.createPlainLiteral(props[1].trim()));
                        }
                    }
                } else if (mapEntry.getDatatype().equalsIgnoreCase("Date")) {
                    instancePropertyObject = ResourceFactory.createTypedLiteral(instancePropertyValue.trim(),
                            XSDDatatype.XSDdate);
                } else if (mapEntry.getDatatype().equalsIgnoreCase("int")) {
                    instancePropertyObject = ResourceFactory.createTypedLiteral(instancePropertyValue.trim(),
                            XSDDatatype.XSDinteger);
                } else {
                    instancePropertyObject = ResourceFactory.createPlainLiteral(instancePropertyValue.trim());
                }

                listStatement.add(ResourceFactory.createStatement(instance, instanceProperty, instancePropertyObject));
            }
        }

        return listStatement;
    }

    private void createBibTexModelEntry(Model model, Resource instance, String bibTexString) {
        String bibtexPrefix = "bib_";

        BibTeXDatabase bdb;
        try {
            bdb = CommonHelper.parseBibTeX(bibTexString);
            Map<Key, BibTeXEntry> map = bdb.getEntries();
            Iterator<Key> iter = map.keySet().iterator();
            while (iter.hasNext()) {
                Key key = iter.next();

                BibTeXEntry bib = map.get(key);
                Map<Key, Value> m = bib.getFields();
                Iterator<Key> it = m.keySet().iterator();

                while (it.hasNext()) {

                    Key bibtexField = it.next();

                    Property property = createDatatypeProperty(model, instance.getPropertyResourceValue(model
                            .createProperty(model.getNsPrefixURI("rdf") + "type")), XSD.xstring, bibtexPrefix
                            + bibtexField);

                    Value v = m.get(bibtexField);
                    RDFNode node = ResourceFactory.createPlainLiteral(v.toUserString().trim());
                    model.add(instance, property, node);
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private void storeModelMap(XSSFWorkbook wbook) {
        XSSFSheet sheet = wbook.getSheet(CommonHelper.MAPPING_SHEET);
        if (sheet == null)
            return;

        for (Row row : sheet) {
            if (row.getRowNum() == 0)
                continue; // header

            String sheetName = row.getCell(0).getStringCellValue();
            String concept = row.getCell(1).getStringCellValue();

            ExcelModel map;
            if (modelMapper.containsKey(sheetName)) {
                map = modelMapper.get(sheetName);
            } else {
                map = new ExcelModel(sheetName, concept, null);
                modelMapper.put(sheetName, map);
            }

            String excelColumn = row.getCell(2).getStringCellValue();
            String ontoProperty = row.getCell(3).getStringCellValue();
            String key = row.getCell(4).getStringCellValue();
            String datatype = row.getCell(5).getStringCellValue();
            int num = (int) row.getCell(6).getNumericCellValue();
            if (key.equalsIgnoreCase("pk"))
                map.setPrimaryKey(num);
            int card = (int) row.getCell(7).getNumericCellValue();
            List<String> allowedVals = Arrays.asList(row.getCell(8).getStringCellValue().split(";"));
            List<String> addProps = Arrays.asList(row.getCell(9).getStringCellValue().split(";"));

            ExcelModelEntry entry = new ExcelModelEntry(excelColumn, ontoProperty, key, datatype, num, card,
                    allowedVals, addProps);
            map.put(num, entry);
        }
    }

    private String transformToKey(String cls_name, String entry) {
        entry = entry.trim();
        entry = entry.replace('(', '_');
        entry = entry.replace(')', '_');
        entry = entry.replace(' ', '_');
        entry = entry.replace(',', '_');
        entry = entry.replace(':', '_');
        entry = entry.replace('-', '_');
        entry = cls_name.concat("_").concat(entry);
        entry = entry.toUpperCase();
        return entry;
    }
}
