package se.assertkth.tracediff.statediff.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ExecDiffHelper {
    public static Pair<Map<Integer, Integer>, Map<Integer, Integer>> getMappingFromExecDiff
            (File execDiffReport, boolean isHitDataIncluded)
            throws IOException {
        Map<Integer, Integer> leftRightMapping = new HashMap<>(), rightLeftMapping = new HashMap<>();
        Document doc = Jsoup.parse(execDiffReport, "UTF-8");
        Elements srcRows = doc.selectFirst("tbody").children();

        srcRows.forEach(tr -> {
            Elements cols = tr.children();
            int leftLineNumCol = isHitDataIncluded ? 1 : 0;
            try {
                int srcLine = Integer.parseInt(cols.get(leftLineNumCol).attr("data-line-number")),
                        dstLine = Integer.parseInt(cols.get(leftLineNumCol + 1).attr("data-line-number"));
                leftRightMapping.put(srcLine, dstLine);
                rightLeftMapping.put(dstLine, srcLine);
            } catch (NumberFormatException e) {
            }
        });

        return Pair.of(leftRightMapping, rightLeftMapping);
    }

    public static void addLineInfoAfter
            (
                    Integer line,
                    String infoHtml,
                    File ghDiff,
                    boolean isOriginalLine,
                    boolean isHitDataIncluded
            ) throws Exception {
        Element tag = Jsoup.parse(infoHtml, "UTF-8", Parser.xmlParser()).children().first();

        Document doc = Jsoup.parse(ghDiff, "UTF-8");

        doc.outputSettings().prettyPrint(false);

        int tdIndInParent = isOriginalLine ? 0 : 1;
        tdIndInParent += isHitDataIncluded ? 1 : 0;

        Elements targetTds = doc.select("td[data-line-number={line}]".replace("{line}", line.toString()));
        for (Element td : targetTds){
            if(td.parent().children().get(tdIndInParent).attr("data-line-number").equals(line.toString()))
                td.parent().appendChild(tag);
        }

        String outputHtml = doc.outerHtml();
        outputHtml = outputHtml.replace("  <div class=\"container-xl d-flex flex-column flex-lg-row flex-items-center p-responsive height-full position-relative z-1\">",
                "  <div class=\"container-xl d-flex flex-column flex-lg-row flex-items-center p-responsive position-relative z-1\">");

        FileUtils.writeStringToFile(ghDiff, outputHtml, "UTF-8");
    }
}
