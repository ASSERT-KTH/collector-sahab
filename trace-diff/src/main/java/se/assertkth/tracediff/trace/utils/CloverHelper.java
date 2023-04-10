package se.assertkth.tracediff.trace.utils;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class CloverHelper {
    private static final Logger logger = LoggerFactory.getLogger(CloverHelper.class);

    private static final String CLOVER_MVN_XML_PATH = "/trace/clover_plugin_mvn.xml";
    private static final String CLOVER_REPORT_PATH = "target/site/clover/clover.xml";
    private static final String CLOVER_TARGET_DESCRIPTOR_PATH = "clover-descriptor.xml";
    private static final String CLOVER_RESOURCE_DESCRIPTOR_PATH = "/trace/clover_descriptor.xml";
    private static final String CLOVER_SRC_PATH_KEYWORD = "{src-path}";
    private static final String CLOVER_FILE_REPORT_SELECTOR = "[path*=" + CLOVER_SRC_PATH_KEYWORD + "]";

    // MVN related keywords
    private static final String POM_FILENAME = "pom.xml";
    private static final String BUILD_KEYWORDS = "build";
    private static final String PLUGINS_KEYWORDS = "plugins";
    private static final String CLOVER_ID = "clover-maven-plugin";
    // End of MVN related keywords

    public static Map<String, Map<Integer, Integer>> getPerLineCoverages
            (
                    File projectDir,
                    List<String> modifiedFilePaths,
                    String selectedTest
            ) throws Exception {
        setupCloverForMaven(projectDir);


        runMvnTest(projectDir, selectedTest);


        Map<String, Map<Integer, Integer>> res = new HashMap<>();
        for (String modifiedFilePath : modifiedFilePaths) {
            res.put(modifiedFilePath, getPerLineCoverage(projectDir, Path.of(modifiedFilePath)));
        }
        return res;
    }

    private static Map<Integer, Integer> getPerLineCoverage(File projectDir, Path modifiedFileRelativePath)
            throws IOException {
        Map<Integer, Integer> res = new HashMap<>();

        org.jsoup.nodes.Document doc = Jsoup.parse(Path.of(projectDir.getPath(), CLOVER_REPORT_PATH).toFile(), "UTF-8");
        org.jsoup.nodes.Element reportElem =
                doc.select(CLOVER_FILE_REPORT_SELECTOR
                        .replace(CLOVER_SRC_PATH_KEYWORD, modifiedFileRelativePath.toString())).first();

        if (reportElem == null)
            return res;

        reportElem.select("line").forEach(lineElem -> {
            int lineNum = Integer.parseInt(lineElem.attr("num"));
            int cnt = (lineElem.hasAttr("count") ? Integer.parseInt(lineElem.attr("count")) : 0)
                    + (lineElem.hasAttr("falsecount") ? Integer.parseInt(lineElem.attr("falsecount")) : 0)
                    + (lineElem.hasAttr("truecount") ? Integer.parseInt(lineElem.attr("truecount")) : 0);

            res.put(lineNum, cnt);
        });

        return res;
    }

    private static void runMvnTest(File projectDir, String selectedTest) throws Exception {
        int exitVal = -1;
        if (selectedTest == null)
            exitVal = PH.run(projectDir, "Running maven....", "mvn", "clean",
                    "clover:setup", "test", "-Dmaven.compiler.source=1.6", "-Dmaven.compiler.target=1.6",
                    "-fn", "-DfailIfNoTests=false", "clover:aggregate", "clover:clover",
                    "-Dmaven.clover.reportDescriptor=" + projectDir.getPath() + File.separator + CLOVER_TARGET_DESCRIPTOR_PATH);
        else
            exitVal = PH.run(projectDir, "Running maven....", "mvn", "clean", "-Dtest=" + selectedTest,
                    "clover:setup", "test", "-Dmaven.compiler.source=1.6", "-Dmaven.compiler.target=1.6",
                    "-fn", "-DfailIfNoTests=false", "clover:aggregate", "clover:clover",
                    "-Dmaven.clover.reportDescriptor=" + projectDir + File.separator + CLOVER_TARGET_DESCRIPTOR_PATH);
        if (exitVal != 0)
            throw new Exception("Could not run mvn.");
    }

    private static void setupCloverForMaven(File projectDir)
            throws IOException, ParserConfigurationException, SAXException, TransformerException, URISyntaxException {

        // Adding plugin to pom.xml
        File pomFile = projectDir.toPath().resolve(POM_FILENAME).toFile();
        InputStream is = new FileInputStream(pomFile);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(is);
        is.close();


        Element buildElem = getOrCreateElemByTagName(BUILD_KEYWORDS, doc.getDocumentElement(), doc);
        Element pluginsElem = getOrCreateElemByTagName(PLUGINS_KEYWORDS, buildElem, doc);
        StreamResult pluginsStreamResult = new StreamResult(new StringWriter());
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(pluginsElem), pluginsStreamResult);
        String pluginsStr = pluginsStreamResult.getWriter().toString();
        if (pluginsStr.indexOf(CLOVER_ID) < 0) {
            String cloverXmlStr = new Scanner(CloverHelper.class.getResourceAsStream(CLOVER_MVN_XML_PATH),
                    "UTF-8").useDelimiter("\\A").next();

            Document cloverDoc = db.parse(new ByteArrayInputStream(cloverXmlStr.getBytes("UTF-8")));
            Node cloverNode = doc.importNode(cloverDoc.getDocumentElement(), true);
            pluginsElem.appendChild(cloverNode);


            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            Result output = new StreamResult(pomFile);
            Source input = new DOMSource(doc);

            transformer.transform(input, output);
        }
        // End of adding plugin to pom.xml

        // Adding clover descriptor
        String descriptorText = new Scanner(CloverHelper.class.getResourceAsStream(CLOVER_RESOURCE_DESCRIPTOR_PATH),
                "UTF-8").useDelimiter("\\A").next();
        FileUtils.writeStringToFile(projectDir.toPath().resolve(CLOVER_TARGET_DESCRIPTOR_PATH).toFile(), descriptorText,
                "UTF-8");
        // End of adding clover descriptor

    }

    private static Element getOrCreateElemByTagName(String tagName, Element elem, Document doc) {
        NodeList nodeLst = elem.getElementsByTagName(tagName);
        if (nodeLst.getLength() < 1) {
            elem.appendChild(doc.createElement(tagName));
            nodeLst = elem.getElementsByTagName(tagName);
        }
        return (Element) nodeLst.item(0);
    }

}
