package org.cdlflex.jena.metadata;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

public class FoafPerson {
    public static final String FOAF_PERSON = Prefix.FOAF + "Person";
    public static final String FOAF_NAME = Prefix.FOAF + "name";
    public static final String SKE_OID = Prefix.SKE + "oid";

    Resource personCls;
    Resource personInst;

    Property name;
    Property oid;

    String oidStr;

    public FoafPerson(Model model, String oid) {
        if (!oid.startsWith(Prefix.SKE)) {
            oid = Prefix.SKE + oid;
        }

        this.personCls = model.createResource(FOAF_PERSON);
        this.personInst = model.createResource(oid, personCls);
        this.name = model.createProperty(FOAF_NAME);
        this.oid = model.createProperty(SKE_OID);
        this.oidStr = oid.replace(Prefix.SKE, "");

        personInst.addProperty(this.oid, this.oidStr);
    }

    public Resource getResource() {
        return personInst;
    }

    public String getName() {
        Statement stmt = personInst.getProperty(name);
        if (stmt == null || stmt.getObject() == null) {
            return null;
        }
        return stmt.getObject().asLiteral().getString();
    }

    public String getOid() {
        return oidStr;
    }

    public void setName(String name) {
        Model model = personInst.getModel();
        model.add(personInst, this.name, ResourceFactory.createPlainLiteral(name));
    }
}
