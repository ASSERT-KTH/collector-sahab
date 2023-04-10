package se.assertkth.tracediff.models;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;

public class SourceInfo {
    private static final Logger logger = Logger.getLogger(SourceInfo.class.getName());
    private static final int METHOD_CONTEXT_MARGIN = 3;

    private Map<Integer, Set<String>> lineVars;

    private Launcher launcher = new Launcher();
    private CtModel model;

    public SourceInfo(File src) {
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.addInputResource(src.getAbsolutePath());
        launcher.buildModel();
        model = launcher.getModel();
        computeLineVars();
    }

    private void computeLineVars() {
        lineVars = new HashMap<>();

        model.filterChildren(new TypeFilter<>(CtVariableRead.class)).forEach(obj -> {
            try {
                CtVariableRead var = (CtVariableRead) obj;
                int line = var.getPosition().getLine();
                if (!lineVars.containsKey(line)) lineVars.put(line, new HashSet<>());
                lineVars.get(line).add(var.getVariable().getSimpleName());
            } catch (Exception e) {
                logger.info("Exception while going over variables.");
            }
        });
    }

    public Map<Integer, Set<String>> getLineVars() {
        return lineVars;
    }

    public CodeInterval getContainingMethodContextInterval(int lineNumber) {
        final CodeInterval methodContextInterval = new CodeInterval();

        for (Object obj : model.filterChildren(new TypeFilter<>(CtMethod.class)).list()) {
            CtMethod method = (CtMethod) obj;

            if (method.getPosition().isValidPosition()
                    && lineNumber >= method.getPosition().getLine()
                    && lineNumber <= method.getPosition().getEndLine()) {
                methodContextInterval.expandIfNeeded(
                        method.getPosition().getLine() - METHOD_CONTEXT_MARGIN,
                        method.getPosition().getEndLine() + METHOD_CONTEXT_MARGIN);
            }
        }
        ;

        return methodContextInterval;
    }
}
