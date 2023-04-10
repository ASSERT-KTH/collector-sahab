package se.assertkth.tracediff.trace.utils;

import com.github.gumtreediff.matchers.Mapping;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.*;
import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import se.assertkth.tracediff.trace.models.LineMapping;
import spoon.reflect.declaration.CtElement;

public class SpoonHelper {
    public static LineMapping getLineMapping(File source, File target) throws Exception {
        LineMapping res = new LineMapping();

        Diff diff = new AstComparator().compare(source, target);

        Iterator<Mapping> it = diff.getMappingsComp().iterator();
        while (it.hasNext()) {
            Mapping mapping = it.next();

            if (mapping.first != null
                    && mapping.second != null
                    && (mapping.first.getChildren().isEmpty()
                            || mapping.second.getChildren().isEmpty())) {

                CtElement srcElem = (CtElement) mapping.first.getMetadata("spoon_object"),
                        dstElem = (CtElement) mapping.second.getMetadata("spoon_object");

                if (!srcElem.getPosition().isValidPosition()
                        || !dstElem.getPosition().isValidPosition()) continue;

                int srcLine = srcElem.getPosition().getLine(),
                        dstLine = dstElem.getPosition().getLine();
                res.addMapping(srcLine, dstLine);
            }
        }

        diff.getRootOperations().forEach(op -> removeChangedLinesFromMapping(res, op));

        return res;
    }

    private static void removeChangedLinesFromMapping(LineMapping mapping, Operation op) {
        if (op instanceof InsertOperation) {
            removeNodeFromMapping(op.getSrcNode(), mapping.getDstToSrc(), mapping.getDstNewLines());
        } else if (op instanceof DeleteOperation) {
            removeNodeFromMapping(op.getSrcNode(), mapping.getSrcToDst(), mapping.getSrcNewLines());
        } else if (op instanceof MoveOperation || op instanceof UpdateOperation) {
            removeNodeFromMapping(op.getSrcNode(), mapping.getSrcToDst(), mapping.getSrcNewLines());
            removeNodeFromMapping(op.getDstNode(), mapping.getDstToSrc(), mapping.getDstNewLines());
        }
    }

    private static void removeNodeFromMapping(CtElement node, Map<Integer, Integer> mapping, Set<Integer> newLines) {
        if (!node.getPosition().isValidPosition()) return;
        IntStream.rangeClosed(node.getPosition().getLine(), node.getPosition().getEndLine())
                .forEach(x -> {
                    mapping.remove(x);
                    newLines.add(x);
                });
    }
}
