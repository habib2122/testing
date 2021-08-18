/**
 * OWASP Benchmark Project
 *
 * <p>This file is part of the Open Web Application Security Project (OWASP) Benchmark Project For
 * details, please see <a
 * href="https://owasp.org/www-project-benchmark/">https://owasp.org/www-project-benchmark/</a>.
 *
 * <p>The OWASP Benchmark is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, version 2.
 *
 * <p>The OWASP Benchmark is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details
 *
 * @author Dave Wichers
 * @created 2015
 */
package org.owasp.benchmark.score.parsers;

import java.io.File;
import java.util.List;
import org.owasp.benchmark.score.BenchmarkScore;
import org.owasp.benchmark.score.TestCaseResult;
import org.owasp.benchmark.score.TestSuiteResults;
import org.w3c.dom.Node;

public class BurpReader extends Reader {

    // filename passed in so we can extract the scan time if it is included in the filename
    // root of XML doc passed in so we can parse the results
    public TestSuiteResults parse(File f, Node root) throws Exception {

        TestSuiteResults tr =
                new TestSuiteResults("Burp Suite Pro", true, TestSuiteResults.ToolType.DAST);

        // <issues burpVersion="1.6.24"
        // exportTime="Wed Aug 19 23:27:54 EDT 2015">

        String version = getAttributeValue("burpVersion", root);
        tr.setToolVersion(version);

        // If the filename includes an elapsed time in seconds (e.g., TOOLNAME-seconds.xml) set the
        // compute time on the scorecard.
        tr.setTime(f);

        List<Node> issueList = getNamedChildren("issue", root);

        for (Node issue : issueList) {
            TestCaseResult tcr = parseBurpVulnerability(issue);
            if (tcr != null) {
                //                System.out.println( tcr.getNumber() + "\t" + tcr.getCWE() + "\t" +
                // tcr.getEvidence() );
                tr.put(tcr);
            }
        }
        return tr;
    }

    // <issue>
    // <serialNumber>5773821289236842496</serialNumber>
    // <type>2097920</type>
    // <name>Cross-site scripting (reflected)</name>
    // <host ip="127.0.0.1">https://localhost:8443</host>
    // <path><![CDATA[/benchmark/BenchmarkTest00023]]></path>
    // <location><![CDATA[/benchmark/BenchmarkTest00023 [vector parameter]]]></location>
    // <severity>High</severity>
    // <confidence>Certain</confidence>
    // <issueBackground></remediationBackground>
    // <references></references>
    // <issueDetail></issueDetail>
    // </issue>

    private TestCaseResult parseBurpVulnerability(Node issue) {
        TestCaseResult tcr = new TestCaseResult();
        String cwe = getNamedChild("type", issue).getTextContent();
        tcr.setCWE(cweLookup(cwe));

        String name = getNamedChild("name", issue).getTextContent();
        tcr.setCategory(name);
        tcr.setEvidence(name);

        // String confidence = getNamedChild( "confidence", issue ).getTextContent();
        // tcr.setConfidence( makeIntoInt( confidence ) );

        String testcase = getNamedChild("path", issue).getTextContent();
        testcase = testcase.substring(testcase.lastIndexOf('/') + 1);
        testcase = testcase.split("\\.")[0];
        if (testcase.startsWith(BenchmarkScore.TESTCASENAME)) {
            String testno = testcase.substring(BenchmarkScore.TESTCASENAME.length());
            try {
                tcr.setNumber(Integer.parseInt(testno));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return tcr;
        }

        return null;
    }
    // https://portswigger.net/kb/issues - This page lists all the issue types Burp looks for, and
    // their
    // customer ID #'s. There are more on this page. The following primarily lists those
    // that are currently relevant in the Benchmark.
    static int cweLookup(String id) {
        switch (id) {
            case "1048832":
                return 78; // Command Injection
            case "1049088":
                return 89; // SQL Injection
            case "1049344":
                return 22; // File Path Traversal
            case "1049600":
                return 611; // XXE
            case "1049856":
                return 90; // LDAP Injection
            case "1050112":
                return 643; // XPATH Injection
            case "1050368":
                return 643; // XML Injection - Meaning what?
            case "1051392":
                return 22; // File Path Manipulation - Not sure exact difference with 1049344 above
            case "2097408":
                return 79; // Stored XSS
            case "2097920":
                return 79; // Reflected XSS
            case "2097936":
                return 79; // DOM-Based XSS (Probably want separate ID for this in the future)
            case "2098944":
                return 352; // CSRF Vulnerability
            case "3146240":
                return 918; // External service interaction (DNS)
            case "4194560":
                return 9999; // Referer Dependent Response
            case "4194576":
                return 9999; // X-Forwarded-For header dependency
            case "4197376":
                return 20; // Input returned in response (reflected)
            case "4197632":
                return 20; // Suspicious input transformation (reflected)
            case "5243392":
                return 614; // SSL cookie without secure flag set
            case "5244416":
                return 9998; // Cookie without HttpOnly flag set - There is no CWE defined for this
                // weakness
            case "5245344":
                return 8888; // Clickjacking - There is no CWE # for this.
            case "5245360":
                return 16; // Browser cross-site scripting filter disabled
            case "5245952":
                return 9999; // Ajax request header manipulation (DOM-based) - Map to nothing right
                // now.
            case "5247488":
                return 9999; // DOM Trust Boundary Violation - Map to nothing right now.
            case "6291968":
                return 200; // Information Disclosure - Email Address Disclosed
            case "6292736":
                return 200; // Information Disclosure - Credit Card # Disclosed
            case "7340288":
                return 525; // Information Exposure Through Browser Caching-Cacheable HTTPS Response
            case "8389120":
                return 9999; // HTML doesn't specify character set - Don't care. Map to nothing.
            case "8389632":
                return 9999; // Incorrect Content Type - Don't care. Map to nothing right now.
            case "8389888":
                return 16; // Content type is not specified
        } // end switch(id)
        System.out.println("Unknown Burp rule id: " + id);
        return -1;
    }
}
