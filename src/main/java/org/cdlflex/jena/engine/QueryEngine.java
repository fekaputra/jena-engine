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
import java.util.List;
import java.util.Map;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.cdlflex.jena.helper.OwlHelper;
import org.cdlflex.jena.helper.excel.ExcelHelper;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFVisitor;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;

/**
 * PI Query Tester Designed to work with a custom inference engine & queries in
 * files
 * 
 * @author Juang fajar.juang@gmail.com
 * 
 */
public class QueryEngine {
    private final String defaultURI;
    private final Dataset dataset;
    private final Workbook workbook;

    private OntModel model;
    private OutputStream outputStream;
    private Sheet sheet;

    /**
     * Constructor.
     * 
     * @param owlFile owlfile name.
     * @param ruleFile custom rule file.
     */
    public QueryEngine(String owlFile, String ruleFile, boolean isUsingFile) {
        model = null;
        dataset = RDFDataMgr.loadDataset(owlFile);
        defaultURI = dataset.getDefaultModel().getNsPrefixURI("");
        workbook = new XSSFWorkbook();

        if (!isUsingFile) {
            outputStream = System.out;
        } else {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd:MM:yyyy_HH:mm");
                Date resultdate = new Date(System.currentTimeMillis());
                outputStream = System.out;
                outputStream = new FileOutputStream("result/output_" + sdf.format(resultdate) + ".txt");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        initRules(ruleFile);
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
        ParameterizedSparqlString qString = new ParameterizedSparqlString();
        qString.setNsPrefixes(model.getNsPrefixMap());
        sheet = workbook.createSheet(fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf(".")));
        formulateQuery(qString, fileName);
        if (params != null && !params.isEmpty())
            setParam(qString, params);
        doQueryExec(fileName, qString, debug);
    }

    private void formulateQuery(ParameterizedSparqlString qString, String fileName) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String brLine;
            int i = 0;
            while ((brLine = br.readLine()) != null) {
                if (brLine.startsWith("#")) {
                    Row r = sheet.createRow(i++);
                    Cell c = r.createCell(0, Cell.CELL_TYPE_STRING);
                    c.setCellValue(brLine);
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

    private void doQueryExec(String fileName, ParameterizedSparqlString qString, boolean debug) {
        if (debug)
            System.out.println(qString.toString());

        long execTime = System.currentTimeMillis();
        QueryExecution qe = QueryExecutionFactory.create(qString.asQuery(), model);
        ResultSet rs = qe.execSelect();
        ResultSetFormatter.out(outputStream, rs);
        execTime = System.currentTimeMillis() - execTime;
        try {
            outputStream.write(("execution time: " + Long.toString(execTime) + " ms \n\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        qe.close();

        doAnotherQuery(qString, fileName);
    }

    private void doAnotherQuery(ParameterizedSparqlString qString, String sheetName) {
        QueryExecution qe = QueryExecutionFactory.create(qString.asQuery(), model);
        ResultSet rs = qe.execSelect();
        List<String> vars = rs.getResultVars();
        Iterator<String> it = vars.iterator();
        if (workbook.getSheet(sheet.getSheetName()) == null) {
            sheet = workbook
                    .createSheet(sheetName.substring(sheetName.lastIndexOf("/") + 1, sheetName.lastIndexOf(".")));
        }
        Row r = sheet.createRow(sheet.getLastRowNum() + 1);
        int cellCounter = 0;
        CellStyle cs = workbook.createCellStyle();
        Font f = workbook.createFont();
        f.setBoldweight(Font.BOLDWEIGHT_BOLD);
        cs.setFont(f);
        while (it.hasNext()) {
            Cell cell = r.createCell(cellCounter++);
            cell.setCellValue(it.next().toUpperCase());
            cell.setCellStyle(cs);
        }

        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            it = vars.iterator();
            r = sheet.createRow(sheet.getLastRowNum() + 1);
            cellCounter = 0;
            while (it.hasNext()) {
                RDFNode value = qs.get(it.next());
                if (value == null)
                    return;
                String valueInString = (String) value.visitWith(new RDFVisitor() {

                    @Override
                    public Object visitURI(Resource r, String uri) {
                        return r.getLocalName();
                    }

                    @Override
                    public Object visitLiteral(Literal l) {
                        return l.getValue().toString();
                    }

                    @Override
                    public Object visitBlank(Resource r, AnonId id) {
                        return "<Empty>";
                    }
                });
                Cell cell = r.createCell(cellCounter++);
                try {
                    int inte = Integer.parseInt(valueInString);
                    cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                    cell.setCellValue(inte);
                } catch (Exception e) {
                    cell.setCellValue(valueInString);
                }
            }
        }
        qe.close();

        // column size adjustment
        for (int i = sheet.getFirstRowNum(); i < sheet.getLastRowNum(); i++) {
            sheet.autoSizeColumn(i);
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

    private void initRules(String ruleFile) {
        Model m = dataset.getDefaultModel();
        if (ruleFile != null) {
            Reasoner reasoner = new GenericRuleReasoner(OwlHelper.readRules(defaultURI, ruleFile));
            reasoner = reasoner.bindSchema(m);
            OntModelSpec ontModelSpec = OntModelSpec.OWL_DL_MEM;
            ontModelSpec.setReasoner(reasoner);
            model = ModelFactory.createOntologyModel(ontModelSpec, m);
        } else {
            OntModelSpec ontModelSpec = OntModelSpec.OWL_MEM;
            model = ModelFactory.createOntologyModel(ontModelSpec, m);
        }
    }

    public void exportModel(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            model.writeAll(writer, null, null);

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public void exportResult(String filename) {
        ExcelHelper.writeFile(workbook, filename);
    }
}
