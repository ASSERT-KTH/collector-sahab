
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