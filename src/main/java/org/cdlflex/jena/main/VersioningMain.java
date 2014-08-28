package org.cdlflex.jena.main;

import org.cdlflex.jena.helper.CommonHelper;
import org.cdlflex.jena.metadata.FoafDocument;
import org.cdlflex.jena.metadata.FoafPerson;
import org.cdlflex.jena.metadata.Prefix;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;

public class VersioningMain {
    static Dataset dataset;
    static Model model;

    public static void main(String[] args) {
        String link = "data/version";

        dataset = CommonHelper.readFile(link);
        dataset.begin(ReadWrite.WRITE);
        System.out.println("test1");
        model = dataset.getDefaultModel();
        System.out.println("test2");

        FoafPerson p1 = new FoafPerson(model, "p1");
        p1.setName("Fajar Juang");
        System.out.println("test3");

        FoafDocument d1 = new FoafDocument(model, "d1");
        d1.setMaker(p1);
        d1.setHomepage(Prefix.SKE + p1.getOid() + "_" + d1.getOid());
        System.out.println("test4");
        model.commit();
        System.out.println("test6");

        dataset.commit();

        CommonHelper.writeFile(dataset, "test.owl");
        System.out.println("test5");
        dataset.end();
    }

}
