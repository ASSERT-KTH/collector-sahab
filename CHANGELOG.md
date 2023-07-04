
# 0.5.3
## Changelog

## 🔄️ Changes
- a09356e style: Refactor CollectorAgent.java for readability and performance (#275)
- c2c1233 revert: remove sbom-monitor as it is being tested in #268

## 🧰 Tasks
- 7f74813 chore: releasing version 0.5.3
- c406590 chore(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.4.0-beta-13
- 9e8a21e chore(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.4.0-beta-12
- 0a7de8f chore(deps): update dependency org.apache.maven:maven-model to v3.9.3
- a064656 chore(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.4.0-beta-11
- 6115aa2 chore(deps): update dependency org.apache.maven.plugins:maven-shade-plugin to v3.5.0
- bdd3ca6 chore(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.4.0-beta-10
- 74c646a chore(deps): update dependency org.seleniumhq.selenium:selenium-java to v4.10.0
- 41d345d chore(deps): update dependency commons-io:commons-io to v2.13.0
- 153b812 chore(deps): update dependency org.apache.maven.plugins:maven-surefire-plugin to v3.1.2
- c0ff91f chore(deps): update dependency org.apache.maven.plugins:maven-failsafe-plugin to v3.1.2
- b98c21b chore(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.4.0-beta-9
- 4f2c650 chore(deps): update dependency info.picocli:picocli to v4.7.4
- 376eae6 chore(deps): update dependency net.bytebuddy:byte-buddy-dep to v1.14.5
- e84b94e chore(deps): update dependency org.kohsuke:github-api to v1.315
- e22427a chore(deps): update dependency com.fasterxml.jackson.core:jackson-databind to v2.15.2
- 08d55a8 chore(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.4.0-beta-8
- 3f89871 chore: setting SNAPSHOT version 0.5.3-SNAPSHOT

## 🛠  Build
- f0160f4 test: disable flaky test (#277)
- 1c77671 ci: Update GitHub Actions workflows to test with multiple Maven versions (#276)
- eda302d ci: use git diff to compute diff
- 5a210ac ci: fix syntax
- c15ac2c ci: provide absolute path to diff utility
- 748ba5c ci: use the correct reference for base reference
- 590806a ci: fix sbom monitor job
- c1c941f ci: add a job for posting diff in SBOM as PR comments


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ()
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))
- renovate[bot] ([@renovate[bot]](https://github.com/apps/renovate))

# 0.5.2
## Changelog

## 🐛 Fixes
- 4a432a7 fix(trace-diff): prevent trailing dot in the last array element (#256)

## 🧰 Tasks
- d1365a9 chore: releasing version 0.5.2
- 2d0160f chore(deps): update dependency com.diffplug.spotless:spotless-maven-plugin to v2.37.0 (#255)
- 61430c3 chore(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.4.0-beta-7
- 2c11c9f chore(deps): update dependency org.apache.maven.plugins:maven-dependency-plugin to v3.6.0
- 4ef657b chore(deps): update dependency fr.inria.gforge.spoon.labs:gumtree-spoon-ast-diff to v1.62
- df75869 chore(deps): update dependency org.apache.maven.plugins:maven-source-plugin to v3.3.0
- e3fa78d chore: setting SNAPSHOT version 0.5.2-SNAPSHOT

## 🛠  Build
- a413f12 ci: remove JRELEASER_VERSION file


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ()
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))
- renovate[bot] ([@renovate[bot]](https://github.com/apps/renovate))

# 0.5.1
## Changelog

## 🐛 Fixes
- fcea85a fix: avoid caching modules to prevent race-condition (#245)

## 🧰 Tasks
- 625a7eb chore: releasing version 0.5.1
- d039302 chore(deps): update dependency commons-io:commons-io to v2.12.0
- e045714 chore(deps): update dependency com.fasterxml.jackson.core:jackson-databind to v2.15.1
- 52d6ecf chore(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.4.0-beta-6
- 3aa4be3 chore(deps): update dependency org.apache.maven.plugins:maven-assembly-plugin to v3.6.0
- 217248c chore(deps): update dependency org.apache.maven:maven-model to v3.9.2
- 997f608 chore(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.4.0-beta-5
- 317b782 chore(deps): update dependency fr.inria.gforge.spoon.labs:gumtree-spoon-ast-diff to v1.61
- 823c779 chore(deps): update dependency org.seleniumhq.selenium:selenium-java to v4.9.1
- ba3be20 chore(deps): update dependency org.apache.maven.plugins:maven-surefire-plugin to v3.1.0
- 59ec5d5 chore(deps): update dependency org.apache.maven.plugins:maven-failsafe-plugin to v3.1.0
- 8837463 chore: setting SNAPSHOT version 0.5.1-SNAPSHOT

## 🛠  Build
- 31ec3a5 ci: actually sign commits
- abd235e ci: sign commits that are created by GitHub action bot while releasing
- 5156ecf test: add test to verify collection of data for null objects (#236)
- c7418b4 ci: use latest version for jreleaser

## 📝 Documentation
- 90062fb docs: add a better example diff


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ()
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))
- renovate[bot] ([@renovate[bot]](https://github.com/apps/renovate))

# 0.5.0
## Changelog

## 🚀 Features
- 1b2bc12 feat: record matched lines inside constructor (#228)

## 🔄️ Changes
- 31bb9b5 refactor: throw exception when hash is not long enough (#227)

## 🧰 Tasks
- 0b44b01 chore: releasing version 0.5.0
- 17a0fec chore(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.4.0-beta-4
- 8d42d6b chore: setting SNAPSHOT version 0.4.2-SNAPSHOT

## 🛠  Build
- 2f48d9a test: ensure value returned by constructor is recorded (#229)


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ()
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))
- renovate[bot] ([@renovate[bot]](https://github.com/apps/renovate))

# 0.4.1
## Changelog

## 🔀 Merge
- 5d635ae Merge pull request #223 from ASSERT-KTH/fix/return-diff-on-unchanged

## 🐛 Fixes
- 55bf933 fix: return diff on unchanged methods ignored

## 🧰 Tasks
- de20591 chore: releasing version 0.4.1
- 65db9be chore: removed unused file
- 80172a0 chore: spotless applied
- 49734d5 chore(deps): update dependency org.jsoup:jsoup to v1.16.1
- 296c328 chore: setting SNAPSHOT version 0.4.1-SNAPSHOT


## Contributors
We'd like to thank the following people for their contributions:
- GitHub ()
- Khashayar Etemadi ([@khaes-kth](https://github.com/khaes-kth))
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))
- renovate[bot] ([@renovate[bot]](https://github.com/apps/renovate))

# 0.4.0
## Changelog

## 🚀 Features
- 89fdf98 feat: exclude random values (#205)

## 🧰 Tasks
- 4ba3a0c chore: releasing version 0.4.0
- f4a8c34 chore(deps): update dependency org.junit.jupiter:junit-jupiter to v5.9.3
- cb92601 chore(deps): update dependency org.jacoco:jacoco-maven-plugin to v0.8.10
- ae11acb chore: setting SNAPSHOT version 0.3.4-SNAPSHOT

## 📝 Documentation
- bba4f00 docs: add maven central badge
- 43ec451 docs: update description of tool (#209)


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ()
- Khashayar Etemadi ([@khaes-kth](https://github.com/khaes-kth))
- Martin Monperrus ([@monperrus](https://github.com/monperrus))
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))
- renovate[bot] ([@renovate[bot]](https://github.com/apps/renovate))

# 0.3.3
## Changelog

## 🐛 Fixes
- 1fa9773 fix: do not load classes for verification (#207)

## 🧰 Tasks
- 878026a chore: releasing version 0.3.3
- 9f9425e chore(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.4.0-beta-3 (#196)
- f477bc6 chore: setting SNAPSHOT version 0.3.3-SNAPSHOT


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ()
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))
- renovate[bot] ([@renovate[bot]](https://github.com/apps/renovate))

# 0.3.2
## Changelog

## 🐛 Fixes
- d834103 fix: update versions for maven.compile(r) (#204)

## 🧰 Tasks
- 59bbbaf chore: releasing version 0.3.2
- 12c1fca chore: setting SNAPSHOT version 0.3.2-SNAPSHOT


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ()
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))

# 0.3.1
## Changelog

## 🐛 Fixes
- e8eaf3c fix: remove instrumentation of throw instruction (#203)

## 🔄️ Changes
- db1ce2b refactor: create another methods.txt file for right version (#202)

## 🧰 Tasks
- bc28c84 chore: releasing version 0.3.1
- 863364c chore: setting SNAPSHOT version 0.3.1-SNAPSHOT


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ()
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))

# 0.3.0
## Changelog

## 🚀 Features
- 029f638 feat: update properties to `1.6` if it is `5` or `1.5` (#199)

## 🧰 Tasks
- be45366 chore: releasing version 0.3.0
- e0487dc chore: setting SNAPSHOT version 0.2.1-SNAPSHOT


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ()
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))

# 0.2.0
## Changelog

## 🚀 Features
- 64cc8c7 feat: add option to clean up generated directories after JVM exit (#197)

## 🔄️ Changes
- 385cd4c refactor: make hardcode consistent with actual behaviour (#193)

## 🧰 Tasks
- dc6034e chore: releasing version 0.2.0
- a7be54a chore(deps): update dependency fr.inria.gforge.spoon.labs:gumtree-spoon-ast-diff to v1.60 (#194)
- 63d627a chore: setting SNAPSHOT version 0.1.7-SNAPSHOT

## 📝 Documentation
- 65b48cc docs: fix default value of `numberOfArrayElements`
- 3ed5dd7 docs: add artifact verification protocol (#189)


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ()
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))
- renovate[bot] ([@renovate[bot]](https://github.com/apps/renovate))

# 0.1.6
## Changelog

## 🐛 Fixes
- e900ed1 fix: clone the first `20` elements of array (#191)

## 🔄️ Changes
- 96eeffa style: please spotless
- 12f2ff0 revert: Revert "chore: releasing version 0.1.6"
- 48f18e5 revert: Revert "chore: releasing version 0.1.6"
- 1ffe9ee revert: Revert "chore: releasing version 0.1.6"
- 80d89f3 revert: Revert "chore: releasing version 0.1.6"

## 🧰 Tasks
- f877f8c chore: releasing version 0.1.6
- 2a95b17 chore: releasing version 0.1.6
- a914931 chore: releasing version 0.1.6
- bbb1504 chore: update jreleaser version
- ff21763 chore: releasing version 0.1.6
- 9531db9 chore: releasing version 0.1.6
- b7d18c3 chore(deps): update dependency com.fasterxml.jackson.core:jackson-databind to v2.15.0
- a05227c chore(deps): update dependency org.seleniumhq.selenium:selenium-java to v4.9.0
- 16ad213 chore(deps): update dependency org.apache.maven.plugins:maven-javadoc-plugin to v3.5.0
- cfef8f8 chore: configure maven central deployment (#186)
- 54af9b0 chore: setting SNAPSHOT version 0.1.6-SNAPSHOT

## 🛠  Build
- 5057670 ci: comply with pomchecker run by jreleaser
- c0e3970 ci: fix POM to comply with maven central requirements
- aeb3003 ci: be consistent

## 📝 Documentation
- ffdbc72 docs: fix @throw docstring of MatchedLineFinder (#192)


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ()
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))
- renovate[bot] ([@renovate[bot]](https://github.com/apps/renovate))

# 0.1.5
## Changelog

## 🔄️ Changes
- 01c6469 perf: exclude our own code (#185)

## 🧰 Tasks
- a23b7e4 chore: releasing version 0.1.5
- 2f0725a chore: set a real group ID (#184)
- 8c08fde chore: remove redundant version
- b090b08 chore: change commit message of renovate PRs
- 7d697b6 chore: setting SNAPSHOT version 0.1.5-SNAPSHOT


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ([@betterqualityassuranceuser](https://github.com/betterqualityassuranceuser))
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))

# 0.1.4
## Changelog

## 🐛 Fixes
- e766785 fix: clone array before storing it as runtime value (#179)
- a2326b1 fix(deps): update dependency info.picocli:picocli to v4.7.3 (#180)

## 🧰 Tasks
- 5a5ab86 chore: releasing version 0.1.4
- e336fce chore: setting SNAPSHOT version 0.1.4-SNAPSHOT


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ([@betterqualityassuranceuser](https://github.com/betterqualityassuranceuser))
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))
- renovate[bot] ([@renovate[bot]](https://github.com/apps/renovate))

# 0.1.3
## Changelog

## 🐛 Fixes
- fd657ff fix: remove debugging configuration from compiler plugin (#178)

## 🧰 Tasks
- 0f4d403 chore: releasing version 0.1.3
- 98c5e5f chore: setting SNAPSHOT version 0.1.3-SNAPSHOT


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ([@betterqualityassuranceuser](https://github.com/betterqualityassuranceuser))
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))

# 0.1.2
## Changelog

## 🐛 Fixes
- 27f5dfa fix: set source and target version as `1.6` if they are `5` or `1.5` (#177)
- b6dc916 fix(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.4.0-beta-2

## 🧰 Tasks
- 6cabc11 chore: releasing version 0.1.2
- 4aaff37 chore: specify the version of SNAPSHOT
- 266cc57 chore: use tag name for name of the release
- ea98d3c chore: setting SNAPSHOT version 0.1.1


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ([@algomaster99](https://github.com/algomaster99))
- GitHub ([@betterqualityassuranceuser](https://github.com/betterqualityassuranceuser))
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))
- renovate[bot] ([@renovate[bot]](https://github.com/apps/renovate))

# 0.1.1
## Changelog

## 🐛 Fixes
- a29058f fix: append `.` to array element (#173)
- f8c357d fix: print canonical class name instead of toString representation (#172)
- 75f0829 fix: force fetching of agent (#171)

## 🧰 Tasks
- 9847a2a chore: releasing version 0.1.1
- 4aaee45 chore: contributor is a contributor!
- 5698fbf chore: prevent files generated by jreleaser from being pushed (#170)
- a14cf17 chore: delete files pushed by JReleaser
- 1e127bc chore: setting SNAPSHOT version 0.1.0


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ()
- GitHub ([@betterqualityassuranceuser](https://github.com/betterqualityassuranceuser))
- github-actions[bot] ([@github-actions[bot]](https://github.com/apps/github-actions))

# 0.1.0
## Changelog

## 🔀 Merge
- 1253a0a Merge pull request #69 from algomaster99/start-maintaining-version

## 🚀 Features
- 71bc8b2 feat: add ability to run specific tests (#148)
- 5172474 feat: integrate patch explainer to run along with Main (#147)
- 92fcbf4 feat: parse default methods in interfaces (#118)
- 6244924 feat: pass methods explicitly to record their return values (#76)

## 🐛 Fixes
- 8bcf830 fix: prevent generation of backup poms (#167)
- 7ad1712 fix: add more attributes to prevent build from failing (#161)
- 60136b3 fix(deps): update dependency org.seleniumhq.selenium:selenium-java to v4.8.3
- 584b910 fix(deps): update dependency org.ow2.asm:asm-util to v9.5
- 7c8a156 fix(deps): update dependency org.ow2.asm:asm-tree to v9.5
- 319a09f fix(deps): update dependency org.ow2.asm:asm to v9.5
- 3b58dc0 fix(deps): update dependency org.kohsuke:github-api to v1.314
- 8c2d5f9 fix(deps): update dependency org.jsoup:jsoup to v1.15.4
- e499e7b fix(deps): update dependency fr.inria.gforge.spoon.labs:gumtree-spoon-ast-diff to v1.59
- 651c87a fix(deps): update dependency org.junit.jupiter:junit-jupiter to v5.9.2
- 2f1b0c2 fix(deps): update dependency com.googlecode.json-simple:json-simple to v1.1.1
- cb9b470 fix: prevent failing tests if none are found (#150)
- 4713ab8 fix: put fully qualified class names with `/` as delimiter (#146)
- 6da313e fix(deps): update dependency net.bytebuddy:byte-buddy-dep to v1.14.4
- dba8a28 fix(deps): update dependency info.picocli:picocli to v4.7.2
- d886b7b fix(deps): update dependency fr.inria.gforge.spoon:spoon-core to v10.3.0 (#132)
- ed893af fix: include all line numbers that are part of multi-line expression (#121)
- 45728df fix: ignore imports when computing mapping (#117)
- ea1ad94 fix: add timeout so that event is not looked for forever
- 1007c58 fix: fix serialisation of special floating value (#102)
- e02b54d fix: serialise special floating point values (#100)
- 145c948 fix: check for ignoring prior to performing operation on statement
- 734a545 fix: skip lines in other file that are affected by pure insert and delete operations (#91)
- 3eb35f1 fix: skip all statements inside inserted nodes (#89)
- 6d5316c fix: correct computation of `diff method` when there is an `InsertOperation` (#75)
- 3bec872 fix: optimise serialisation of output (#74)
- 986e769 fix: put breakpoints inside case blocks (#70)

## 🔄️ Changes
- 556b6c1 refactor: use one fixed path for chromium driver (#165)
- e87123d refactor: use our own Pair (#157)
- 412f874 refactor: use `jackson` in `trace-diff` (#156)
- 035b6da refactor: move common utilities to commons module (#155)
- ad63110 refactor: integrate PatchExplainer as trace-diff (#139)
- 87c6aee refactor: remove unnecessary nesting
- e0037b1 refactor: modularise project (#125)
- 4aa3d36 refactor: use instrumentation to collect trace (#123)
- ca668bc perf: destroy debuggee after the debugger ends (#120)
- a33c5f9 refactor: rewrite MatchedLineFinder to find breakpoints using UNIX diff instead of AST diff (#93)
- 47a344b style: mark options as required to prevent NPE
- 229ced3 refactor: log invalid breakpoints (#71)

## 🧰 Tasks
- 901237f chore: releasing version 0.1.0
- e608ddf chore: fix name of artifact jar (#169)
- 802ff45 chore: releasing version 0.1.0
- cd23829 chore: package before release (#168)
- be58a87 chore: releasing version 0.1.0
- b47e5cd chore: add workflow to release collector-sahab (#166)
- 307af29 chore(deps): update actions/checkout action to v3 (#138)
- cb2ba05 chore(deps): update dependency org.apache.maven.plugins:maven-dependency-plugin to v3.5.0
- 502abff chore: remove assembly of `trace-diff` (#163)
- 830acb2 chore: distinguish local directory easily
- 843e8b8 chore: transform all POMs in all submodules (#160)
- 772af15 chore: bundle agent (#159)
- b778076 chore(deps): update actions/setup-java action to v3 (#154)
- 623bdb4 chore: remove unused dependency (#158)
- d8cf826 chore(deps): update dependency com.diffplug.spotless:spotless-maven-plugin to v2.36.0
- 5a85e4b chore(deps): update dependency org.jacoco:jacoco-maven-plugin to v0.8.9
- 40085ee chore: enable automerge
- 9a210f1 chore: add renovate configuration
- a1cda64 chore: port script from Python to Java (#127)
- f979de8 chore: prevent creation of dependency-reduced-pom
- edf7375 chore: rename package (#124)
- bddecba chore: Port gson to jackson
- 01ed5bc chore: rename argument file
- 8b7744a chore: use `@` to pass arguments to Java command (#111)
- 138ce15 chore: fix clean up of generated directories
- 5e7dbf3 chore: remove suppression of slf4j statements
- 7dfc9ec chore: add features to run bribe-sahab on multiple commits simultaneously (#106)
- 5d2980c chore: add config.json to gitignore
- 645552f chore: make spotless compatible with maven 3.8.6 (#107)
- 67ed8ab chore: wrap classpath finder inside try-catch
- 05419f0 chore: prioritise our built classes (#99)
- 0aa4b1f chore: run all JUnit tests in a single container (#97)
- 49917a7 chore: update `JUnitTestRunner` to use the latest JUnit dependencies (#95)
- b50a129 chore: sanitise bash input
- 8888ff2 chore: remove explict setting of maven compiler version
- e52a5c8 chore: handle multi-modular maven projects (#87)

## 📝 Documentation
- d36c004 docs: add citation information
- ad87ef5 docs: instruction to get chromium driver
- b9d693a docs: update prerequisite
- e2f384f docs: add example diff
- fc4003e docs: add integration test badge
- 28751ba docs: update README
- 19bfdb8 docs: fix example

---
- 0023c4c Revert "chore: releasing version 0.1.0"
- 038303a Revert "chore: releasing version 0.1.0"
- ae67783 tests: add infrastructure for integration tests (#162)
- ca58725 tests: add infrastructure for integration tests (#162)
- 243d983 doc: fix module names
- 8d07eba doc: update README based on the sprint in the last two weeks :relieved:
- 325bb98 doc: Give authorship to Khashayar
- 24c50f0 refator: rename parent module (#126)
- b7d5f7d tests: prove JUnit does not care about 'Test' prefix or suffix (#114)
- ab964e0 tests: prove JUnit cannot discover tests in abstract classes (#113)
- 17c910b tests: add test to check data is collected from methods that are run by nested tests
- ee12bdd chrore: Use config file for JAR and output directory (#105)
- fb1957c tests: load classpath from cp.txt (#98)
- f0803bc Build classpath instead of copying dependencies (#96)
- 5cbad7c tests: add test that shows inaccuracy between GitHub diff and AST Diff (#90)
- b2de6b5 tests: add failing test for case when argument difference is not detected (#82)
- 09d2702 tests: ensure methods.json is created even if there are no breakpoints (#81)
- 501e6e0 tests: skip recording return values if fully qualified name does not match (#80)
- 69088ca Prepare for next release


## Contributors
We'd like to thank the following people for their contributions:
- Aman Sharma ()
- I-Al-Istannen ([@I-Al-Istannen](https://github.com/I-Al-Istannen))
- Khashayar Etemadi ([@khaes-kth](https://github.com/khaes-kth))
