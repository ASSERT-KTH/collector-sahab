package io.github.chains_project.collector.module;

import io.github.chains_project.collector.CollectorAgent;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Java9ModuleCracker implements ModuleCracker {

    private final Instrumentation instrumentation;

    public Java9ModuleCracker(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    @Override
    public void crack(Class<?> source) {
        Module module = source.getModule();

        Map<String, Set<Module>> toOpen = module.getPackages().stream()
                .collect(Collectors.toMap(
                        it -> it,
                        it -> Set.of(CollectorAgent.class.getClassLoader().getUnnamedModule())));
        instrumentation.redefineModule(module, Set.of(), Map.of(), toOpen, Set.of(), Map.of());
    }
}
