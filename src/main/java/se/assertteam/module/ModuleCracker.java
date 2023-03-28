package se.assertteam.module;

import java.lang.instrument.Instrumentation;

public interface ModuleCracker {

    void crack(Class<?> source);

    static ModuleCracker noop() {
        return source -> {};
    }

    static ModuleCracker getApplicable(Instrumentation instrumentation) {
        try {
            Class.forName("java.lang.Module");
        } catch (ClassNotFoundException ignored) {
            return ModuleCracker.noop();
        }

        try {
            return (ModuleCracker)
                    Class.forName("se.assertteam.module.Java9ModuleCracker")
                            .getDeclaredConstructors()[0]
                            .newInstance(instrumentation);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
