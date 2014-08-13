package org.cdlflex.jena.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

public class QueryHelper {

	/**
	 * read the query from file
	 * 
	 * @param filename
	 * @return 
	 */
	public static String readQuery(String filename)
	{
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
	 * changes are not automatically saved into the ontology, 
	 * 	so you have to trigger it yourself, and choose where to store your model+changes 
	 * 
	 * @param outFile
	 */
	public static void save(Model model, String outFile) {
		try {
			model.write(new FileWriter(outFile), "RDF/XML");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * linking OntModel with an owl file
	 * 
	 * @param owlFile
	 */
	public static void readOwl(OntModel model, String owlFile) {
		InputStream in = FileManager.get().open(owlFile);
		if(in==null) throw new IllegalArgumentException("File: '"+owlFile+"' not found");
		model.read(in, null);
	}
	
	/**
	 * linking OntModel with an owl file
	 * 
	 * @param owlFile
	 */
	public static OntModel readOwl(String owlFile) {
		OntModel model = ModelFactory.createOntologyModel();
		InputStream in = FileManager.get().open(owlFile);
		if(in==null) throw new IllegalArgumentException("File: '"+owlFile+"' not found");
		model.read(in, null);
		return model;
	}
	
	/**
	 * create temp file. add prefix. add rules.
	 *     return the rules
	 */
	public static List<Rule> readRules(String prefix, String ruleFile) {
	    List<Rule> rules = null;
	    try {
            PrintWriter writer = new PrintWriter("temp.rules", "UTF-8");
            writer.println("@prefix : <"+prefix+">");
            writer.print(readFile(ruleFile, StandardCharsets.UTF_8));
            writer.flush();
            writer.close();
            
            BufferedReader br = new BufferedReader(new FileReader("temp.rules"));
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
	public static String readFile(String path, Charset encoding) 
	        throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return encoding.decode(ByteBuffer.wrap(encoded)).toString();
    }
	
}
