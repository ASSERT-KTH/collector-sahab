package se.assertteam.module;

import se.assertteam.CollectorAgent;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Java9ModuleCracker implements ModuleCracker {

    private final Set<String> openModules;
    private final Instrumentation instrumentation;

    public Java9ModuleCracker(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        this.openModules = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    @Override
    public void crack(Class<?> source) {
        Module module = source.getModule();

        if (!this.openModules.add(module.getName())) {
            return;
        }

        Map<String, Set<Module>> toOpen = module.getPackages().stream()
                .collect(Collectors.toMap(
                        it -> it,
                        it -> Set.of(CollectorAgent.class.getClassLoader().getUnnamedModule())
                ));
        instrumentation.redefineModule(
                module,
                Set.of(),
                Map.of(),
                toOpen,
                Set.of(),
                Map.of()
        );
    }
}
