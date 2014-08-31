package org.cdlflex.jena.versions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXParser;
import org.jbibtex.ParseException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

public class VersionHelper {

    public static final String MAPPING_SHEET = "Ontology Mapping";

    /**
     * read the query from file
     * 
     * @param filename
     * @return
     */
    public static String readQuery(String filename) {
        String content = null;
        File file = new File(filename);
        try {
            FileReader reader = new FileReader(file);
            char[] chars = new char[(int) file.length()];
            reader.read(chars);
            content = new String(chars);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    /**
     * create temp file. add prefix. add rules. return the rules
     */
    public static List<Rule> readRules(Map<String, String> prefixes, String ruleFile) {
        List<Rule> rules = null;
        File file = new File("temp.rule");
        file.deleteOnExit();
        try {
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            Iterator<String> nsiter = prefixes.keySet().iterator();
            while (nsiter.hasNext()) {
                String key = nsiter.next();
                String value = prefixes.get(key);
                writer.println("@prefix " + key + ": <" + value + ">");
            }
            writer.print(readFile(ruleFile, StandardCharsets.UTF_8));
            writer.flush();
            writer.close();

            BufferedReader br = new BufferedReader(new FileReader(file));
            rules = Rule.parseRules(Rule.rulesParserFromReader(br));
            br.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rules;
    }

    /**
     * read file and return it as String
     * 
     * @param path
     * @param encoding
     * @return
     * @throws IOException
     */
    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return encoding.decode(ByteBuffer.wrap(encoded)).toString();
    }

    public static String transformToKey(String entry) {
        entry = entry.trim();
        entry = entry.replace('(', '_');
        entry = entry.replace(')', '_');
        entry = entry.replace(' ', '_');
        entry = entry.replace(',', '_');
        entry = entry.replace(':', '_');
        entry = entry.replace('-', '_');
        return entry;
    }

    public static String generateKey(String defaultURI, String user, String docID) {
        StringBuilder builder = new StringBuilder();

        builder.append(defaultURI);
        builder.append(user);
        builder.append("-");
        builder.append(docID);

        return VersionHelper.transformToKey(builder.toString());
    }

    public static XSSFWorkbook readXlsxFile(String fileString) {
        try {
            InputStream inp = new FileInputStream(fileString);
            return new XSSFWorkbook(inp);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void writeFile(Model model, String URL) {

        try {
            FileOutputStream fileOut = new FileOutputStream(URL);
            model.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BibTeXDatabase parseBibTeX(String bibTexString) throws IOException, ParseException {
        Reader reader = new StringReader(bibTexString);
        BibTeXParser parser = new BibTeXParser();
        return parser.parse(reader);
    }
}
