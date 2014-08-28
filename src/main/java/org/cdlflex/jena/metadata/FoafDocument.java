package org.cdlflex.jena.metadata;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

public class FoafDocument {
    public static final String FOAF_DOCUMENT = Prefix.FOAF + "Document";
    public static final String FOAF_HOMEPAGE = Prefix.FOAF + "homepage";
    public static final String FOAF_MAKER = Prefix.FOAF + "maker";
    public static final String SKE_OID = Prefix.SKE + "oid";

    Resource documentCls;
    Resource documentInst;

    Property homepage;
    Property maker;
    Property oid;

    String oidStr;

    public FoafDocument(Model model, String oid) {
        if (!oid.startsWith(Prefix.SKE)) {
            oid = Prefix.SKE + oid;
        }

        this.documentCls = model.createResource(FOAF_DOCUMENT);
        this.documentInst = model.createResource(oid, documentCls);

        this.homepage = model.createProperty(FOAF_HOMEPAGE);
        this.maker = model.createProperty(FOAF_MAKER);
        this.oid = model.createProperty(SKE_OID);
        this.oidStr = oid.replace(Prefix.SKE, "");

        documentInst.addProperty(this.oid, this.oidStr);

    }

    public Resource getResource() {
        return documentInst;
    }

    public String getHomepage() {
        Statement stmt = documentInst.getProperty(homepage);
        if (stmt == null || stmt.getObject() == null) {
            return null;
        }
        return stmt.getObject().asLiteral().getString();
    }

    public FoafPerson getMaker() {
        Statement stmt = documentInst.getProperty(homepage);
        if (stmt == null || stmt.getObject() == null || !stmt.getObject().isURIResource()) {
            return null;
        }
        FoafPerson person = new FoafPerson(documentInst.getModel(), stmt.getObject().asResource().getURI());
        return person;
    }

    public String getOid() {
        return oidStr;
    }

    public void setHomepage(String homepage) {
        documentInst.removeAll(this.homepage);
        documentInst.addProperty(this.homepage, ResourceFactory.createPlainLiteral(homepage));

    }

    public void setMaker(FoafPerson person) {
        documentInst.removeAll(maker);
        documentInst.addProperty(maker, person.getResource());
    }
}
