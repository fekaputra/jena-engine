package org.cdlflex.jena.versions;

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
import org.cdlflex.jena.excel.ExcelModel;
import org.cdlflex.jena.excel.ExcelModelEntry;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.ParseException;
import org.jbibtex.Value;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class VersionImport {

    public Model importXlsx(String modelFile, String dataFile, Map<String, String> prefixes) {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(prefixes);

        if (modelFile != null) {
            Map<String, ExcelModel> modelMapper = importModel(modelFile, model);
            if (dataFile != null)
                importData(model, modelMapper, dataFile);
        }

        return model;
    }

    private Map<String, ExcelModel> importModel(String modelFile, Model model) {
        Map<String, ExcelModel> modelMapper = new HashMap<String, ExcelModel>();
        XSSFWorkbook wbook = VersionHelper.readXlsxFile(modelFile);
        storeModelMap(wbook, modelMapper);
        executeImportModel(model, modelMapper);

        return modelMapper;
    }

    private void executeImportModel(Model model, Map<String, ExcelModel> modelMapper) {

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
                        model.add(property, RDF.type, OWL.FunctionalProperty);
                    }
                }
            }
        }
    }

    private void createAddPropResource(Model model, Resource ontClass, List<String> additionalProps) {
        Iterator<String> propsIterator = additionalProps.iterator();
        while (propsIterator.hasNext()) {
            String[] props = propsIterator.next().split("=");
            createDatatypeProperty(model, ontClass, XSD.xstring, props[0]);
        }
    }

    private Property createObjectProperty(Model model, Resource domain, Resource range, String propertyName) {
        Property property = model.createProperty(model.getNsPrefixURI(""), propertyName);

        model.add(model.createStatement(property, RDFS.domain, domain));
        model.add(model.createStatement(property, RDF.type, OWL.ObjectProperty));
        if (range != null)
            model.add(model.createStatement(property, RDFS.range, range));

        return property;
    }

    private Resource createModelClass(Model model, String className, String superClassName) {

        Resource cls = model.createResource(model.getNsPrefixURI("") + className, OWL.Class);
        if (superClassName != null) {
            Resource superCls = model.createResource(model.getNsPrefixURI("") + superClassName);
            cls.addProperty(RDFS.subClassOf, superCls);
        } else {
            cls.addProperty(RDFS.subClassOf, OWL.Thing);
        }

        return cls;
    }

    private void importData(Model model, Map<String, ExcelModel> modelMapper, String dataFile) {
        XSSFWorkbook workbook = VersionHelper.readXlsxFile(dataFile);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        for (XSSFSheet xssfSheet : workbook) {
            if (xssfSheet.getSheetName().equalsIgnoreCase(VersionHelper.MAPPING_SHEET))
                continue;
            processSheet(xssfSheet, modelMapper, model);
            System.out.println("At " + dateFormat.format(Calendar.getInstance().getTime()) + " done "
                + xssfSheet.getSheetName());
        }
    }

    private void processSheet(XSSFSheet sheet, Map<String, ExcelModel> modelMapper, Model model) {

        ExcelModel map = modelMapper.get(sheet.getSheetName());
        if (map == null)
            return;

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

                    Resource domain = instanceProperty.getPropertyResourceValue(RDFS.range);
                    if (domain == null)
                        continue;
                    instancePropertyValue = transformToKey(domain.getLocalName(), instancePropertyValue);
                    instancePropertyObject =
                        model.createResource(model.getNsPrefixURI("") + instancePropertyValue, domain);

                    if (!mapEntry.getAdditionalProperties().isEmpty()) {
                        // TODO: HARDCODE
                        Property bokTopicID = createDatatypeProperty(model, domain, XSD.xstring, "bokTopicID");
                        listStatement.add(ResourceFactory.createStatement(instancePropertyObject.asResource(),
                                bokTopicID, ResourceFactory.createPlainLiteral(values[i].trim())));
                        Iterator<String> propsIterator = mapEntry.getAdditionalProperties().iterator();
                        while (propsIterator.hasNext()) {
                            String[] props = propsIterator.next().split("=");
                            Property prop =
                                createDatatypeProperty(model, instancePropertyObject.asResource(), XSD.xstring,
                                        props[0]);
                            model.add(instancePropertyObject.asResource(), prop,
                                    ResourceFactory.createPlainLiteral(props[1].trim()));
                        }
                    }
                } else if (mapEntry.getDatatype().equalsIgnoreCase("Date")) {
                    instancePropertyObject =
                        ResourceFactory.createTypedLiteral(instancePropertyValue.trim(), XSDDatatype.XSDdate);
                } else if (mapEntry.getDatatype().equalsIgnoreCase("int")) {
                    instancePropertyObject =
                        ResourceFactory.createTypedLiteral(instancePropertyValue.trim(), XSDDatatype.XSDinteger);
                } else {
                    instancePropertyObject = ResourceFactory.createPlainLiteral(instancePropertyValue.trim());
                }

                listStatement
                        .add(ResourceFactory.createStatement(instance, instanceProperty, instancePropertyObject));
            }
        }

        return listStatement;
    }

    private void createBibTexModelEntry(Model model, Resource instance, String bibTexString) {
        String bibtexPrefix = "bib_";

        BibTeXDatabase bdb;
        try {
            bdb = VersionHelper.parseBibTeX(bibTexString);
            Map<Key, BibTeXEntry> map = bdb.getEntries();
            Iterator<Key> iter = map.keySet().iterator();
            while (iter.hasNext()) {
                Key key = iter.next();

                BibTeXEntry bib = map.get(key);
                Map<Key, Value> m = bib.getFields();
                Iterator<Key> it = m.keySet().iterator();

                while (it.hasNext()) {

                    Key bibtexField = it.next();

                    Property property =
                        createDatatypeProperty(model, instance.getPropertyResourceValue(RDF.type), XSD.xstring,
                                bibtexPrefix + bibtexField);

                    Value v = m.get(bibtexField);
                    RDFNode node = ResourceFactory.createPlainLiteral(v.toUserString().trim());
                    model.add(instance, property, node);
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private void storeModelMap(XSSFWorkbook wbook, Map<String, ExcelModel> modelMapper) {
        XSSFSheet sheet = wbook.getSheet(VersionHelper.MAPPING_SHEET);
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

            ExcelModelEntry entry =
                new ExcelModelEntry(excelColumn, ontoProperty, key, datatype, num, card, allowedVals, addProps);
            map.put(num, entry);
        }
    }

    private Property createDatatypeProperty(Model model, Resource domain, Resource range, String propertyName) {
        Property property = model.createProperty(model.getNsPrefixURI(""), propertyName);

        model.add(model.createStatement(property, RDFS.domain, domain));
        model.add(model.createStatement(property, RDF.type, OWL.DatatypeProperty));
        if (range != null)
            model.add(model.createStatement(property, RDFS.range, range));

        return property;
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
