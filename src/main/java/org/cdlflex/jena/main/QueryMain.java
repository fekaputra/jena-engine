package org.cdlflex.jena.main;

import org.cdlflex.jena.engine.QueryEngine;

public class QueryMain {

    public static void main(String[] args) {

        String rampup = "data/rampup/ramp";
        String ske = "data/ske/ske";

        QueryEngine rampupTester = new QueryEngine(rampup, rampup + ".owl", "data/rampup/ramp.rules", false);

        rampupTester.QueryExec(rampup + "0.rq", null, true);
        rampupTester.QueryExec(rampup + "1.rq", null, true);

        QueryEngine skeTester = new QueryEngine(ske, ske + ".owl", "data/ske/ske.rules", false);

        skeTester.QueryExec(ske + "1.rq", null, true);
        skeTester.QueryExec(ske + "2.rq", null, true);
        skeTester.QueryExec(ske + "3.rq", null, true);
        skeTester.QueryExec(ske + "4.rq", null, true);
        skeTester.QueryExec(ske + "5.rq", null, true);
        skeTester.QueryExec(ske + "6.rq", null, true);
        skeTester.QueryExec(ske + "7.rq", null, true);
        skeTester.QueryExec(ske + "8.rq", null, true);
    }
}
