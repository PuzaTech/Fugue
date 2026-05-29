# Fugue Modernization Plan

This document captures the full multi-sprint plan to modernize the Fugue topic modeling package,
including implementation tasks and the testing strategy that protects correctness across every change.

---

## Table of Contents

1. [Pre-Sprint: Golden Baseline](#pre-sprint-golden-baseline)
2. [Sprint 1 — Security & CI Foundation](#sprint-1--security--ci-foundation)
3. [Sprint 2 — Java Runtime & Build Modernization](#sprint-2--java-runtime--build-modernization)
4. [Sprint 3 — Java Code Modernization](#sprint-3--java-code-modernization)
5. [Sprint 4 — Python Wrapper Modernization](#sprint-4--python-wrapper-modernization)
6. [Sprint 5 — Tests, Docs & Release](#sprint-5--tests-docs--release)
7. [Testing Principles](#testing-principles)
8. [Critical Path Warnings](#critical-path-warnings)

---

## Pre-Sprint: Golden Baseline

Establish the safety net before any code changes. Every later sprint verifies against these artifacts.

| # | Task | Detail |
|---|---|---|
| B.1 | **Capture LDA golden output** | Write `LDAIntegrationTest.java`: run LDA with `topics=5`, `iters=20`, deterministic seed (`random=1`), on the `ap-test.json` fixture. Assert: (a) perplexity is finite, (b) topic-word distributions sum to 1.0 within 1e-6, (c) every topic has at least one non-zero word. Store expected perplexity as a named constant. |
| B.2 | **Capture model serialization round-trip** | Write `LDASerializationTest.java`: train, serialize to a temp file, reload via `LDAModel`, assert `alpha`, `beta`, `wordTopicCounts`, and `topicCounts` arrays are element-wise equal to 1e-10. |
| B.3 | **Expand DataReader contract tests** | Add to `DataReaderTest`: (a) empty file → empty doc list, (b) file with no TOKEN features → docs filtered out, (c) `topk=0` → reads all documents, (d) `topk=2` with a 5-doc file → exactly 2 docs returned. |
| B.4 | **Expand Message contract tests** | Add to `MessageTest`: missing key → `getParam` returns `null`, overwrite key → latest value returned, `null` value stored and retrieved correctly. |
| B.5 | **Capture CLI defaults** | Add to `MainEntranceTest`: call `parseOptions` with an empty `args[]`; assert every default value matches the constants in `defaultMessage()` exactly (topics=100, iters=1000, LDASampler=normal, etc.). |
| B.6 | **Capture RandomUtils deterministic sequence** | Add to `RandomUtilsTest`: call `nextDouble()` five consecutive times on a seed-22 instance; assert the full sequence to 1e-12. Repeat for `nextInt(5)`. This guards the Sprint 3.5 refactor. |
| B.7 | **Python smoke tests** | Create `tests/test_utils.py` with pytest: (a) `tokenize("Hello World") == ["hello", "world"]`, (b) `cleaned("abc123") == "abc"`, (c) `cleaned("a") == ""` (too short), (d) `compute_term_stats([["a","b","a"]])` → `tf["a"]==2`, `df["a"]==1`. |
| B.8 | **CI baseline job** | Add a temporary GitHub Actions workflow that runs `./gradlew check` on the unmodified code to confirm a green reference point before any changes land. |

---

## Sprint 1 — Security & CI Foundation

**Goal**: Fix the exposed secret, patch the Log4j CVE window, replace Travis CI with GitHub Actions.

### Implementation Tasks

| # | Task | Detail |
|---|---|---|
| 1.1 | **Remove exposed Code Climate token** | Delete `.travis.yml`. The file contains a hardcoded `repo_token` that must be rotated. Add a note in the commit message to rotate the token on Code Climate. |
| 1.2 | **Upgrade Log4j `2.13.2` → `2.24.x`** | Edit `build.gradle`. Run `./gradlew build` to confirm no API breakage. Log4j 2.13.2 predates several high-severity CVEs. |
| 1.3 | **Upgrade Gson `2.8.2` → `2.11.x` and Commons CLI `1.4` → `1.9.x`** | Edit `build.gradle`. Run tests to verify no deserialization or option-parsing regressions. |
| 1.4 | **Fix deprecated JaCoCo DSL** | In `build.gradle`, change `xml.enabled = true` / `html.enabled = true` to the Gradle 7+ API: `reports { xml { required = true }; html { required = true } }`. |
| 1.5 | **Create GitHub Actions CI workflow** | Write `.github/workflows/ci.yml`: trigger on push and pull_request, matrix over JDK 17 and 21, steps: checkout → setup-java (temurin) → `./gradlew check` → upload JaCoCo report to Codecov. |
| 1.6 | **Update Gradle wrapper to 8.x** | Run `./gradlew wrapper --gradle-version 8.13` and commit the updated wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`). |

### Guard Tests for Sprint 1

| # | Guard Test | When |
|---|---|---|
| 1-T1 | **Gson deserialization smoke** — re-run `DataReaderTest` (all cases including B.3). Gson 2.9+ changed lenient-parsing defaults; assert that `Document` with extra JSON fields is still parsed without error. | Before task 1.3 |
| 1-T2 | **Commons CLI regression** — re-run full `MainEntranceTest` including B.5 defaults. Assert `--LDAHyperOpt slice` still parses correctly. | Before task 1.3 |
| 1-T3 | **Log4j no-crash test** — after upgrading Log4j, run `MainEntrance.main(null)` and assert no `ClassNotFoundException` or configuration error is thrown. Log4j 2.20+ changed default config-file discovery. | Before task 1.2 |
| 1-T4 | **JaCoCo report artifact check** — add a CI step: `ls build/reports/jacoco/test/html/index.html` fails the build if the report is absent. | Task 1.4 |
| 1-T5 | **GitHub Actions matrix smoke** — CI must pass on both JDK 17 and JDK 21. The matrix catches accidental use of preview APIs without the required flag. | Task 1.5 |

---

## Sprint 2 — Java Runtime & Build Modernization

**Goal**: Move to Java 21, migrate to Gradle Kotlin DSL, migrate to JUnit 5.

### Implementation Tasks

| # | Task | Detail |
|---|---|---|
| 2.1 | **Bump source/target compatibility to Java 21** | Change `sourceCompatibility` and `targetCompatibility` to `'21'` in `build.gradle`. Add `--enable-preview` only if preview features are explicitly used. |
| 2.2 | **Migrate `build.gradle` → `build.gradle.kts`** | Rewrite the entire build file in Kotlin DSL. Delete `build.gradle`. Verify `./gradlew build`, `./gradlew fatJar`, and `./gradlew jacocoTestReport` all pass. |
| 2.3 | **Migrate JUnit 4 → JUnit 5 dependency** | Replace `junit:junit:4.13` with `org.junit.jupiter:junit-jupiter:5.11.x` in `build.gradle.kts`. Add `useJUnitPlatform()` to the `test {}` block. |
| 2.4 | **Migrate all test files to JUnit 5 annotations** | Across all 10 test files: replace `import org.junit.Test` → `import org.junit.jupiter.api.Test`, `@Before`/`@After` → `@BeforeEach`/`@AfterEach`, `Assert.*` → `Assertions.*`. Remove `throws Exception` from test method signatures (not required in JUnit 5). |
| 2.5 | **Upgrade Jafama to latest** | Check Maven Central for the latest Jafama release. Upgrade if available; otherwise evaluate replacing with `java.lang.Math` for functions where Jafama provides no measurable benefit. |
| 2.6 | **Add Spotless plugin with Google Java Format** | Add `id("com.diffplug.spotless")` to `build.gradle.kts`; configure `googleJavaFormat()`. Run `./gradlew spotlessApply` on all sources. Wire `spotlessCheck` into the `check` lifecycle. |

### Guard Tests for Sprint 2

| # | Guard Test | When |
|---|---|---|
| 2-T1 | **Test count gate** — before the JUnit migration, count all `@Test` methods (currently N). Add a Gradle test task configuration that fails if fewer than N tests are executed. Prevents silent test loss. | Before task 2.3 |
| 2-T2 | **JUnit 5 migration verification** — after task 2.4, run `./gradlew test --info` and assert the executed test count equals the pre-migration count from 2-T1. | After task 2.4 |
| 2-T3 | **Kotlin DSL build equivalence** — after task 2.2, assert: (a) `./gradlew jar` produces `fugue-topicmodeling-*.jar`, (b) `./gradlew fatJar` produces the all-deps JAR, (c) `META-INF/MANIFEST.MF` inside the fat JAR contains `Main-Class: com.hongliangjie.fugue.MainEntrance`. | After task 2.2 |
| 2-T4 | **Java 21 LDA integration** — re-run the golden LDA integration test (B.1) on Java 21 and assert the perplexity matches the baseline value within 1e-6. | After task 2.1 |
| 2-T5 | **Spotless idempotency** — `./gradlew spotlessCheck` (not `spotlessApply`) must pass on all committed code. Add as a required CI check so formatting-only diffs cannot slip through. | Task 2.6 |

---

## Sprint 3 — Java Code Modernization

**Goal**: Adopt Java 21 language features; eliminate legacy patterns; reduce boilerplate.

> **Highest-risk sprint.** Converting POJOs to records breaks Gson deserialization (records have no
> no-arg constructor by default) and renames accessor methods (`getX()` → `x()`). Read all guard
> tests carefully before starting any task.

### Implementation Tasks

| # | Task | Detail |
|---|---|---|
| 3.1 | **Convert `Feature` to a Java record** | Replace the mutable POJO with `public record Feature(String featureType, String featureName, Double featureValue) {}`. Update all call sites. See guards 3-T1 and 3-T2 first. |
| 3.2 | **Convert `Document` to a Java record** | Replace the POJO with `record Document(String docId, List<Feature> features) {}`. Override `toString()` to preserve the existing `"docId featureCount"` format. See guards 3-T3 and 3-T4 first. |
| 3.3 | **Replace `Hashtable` with `HashMap` in `Message.java`** | `Hashtable` is a legacy synchronized collection. `Message` is never accessed concurrently. Swap to `HashMap<String, Object>`. |
| 3.4 | **Introduce SLF4J facade over Log4j** | Add `slf4j-api` and `log4j-slf4j2-impl` dependencies. Change all `LogManager.getLogger` → `LoggerFactory.getLogger` in `MainEntrance`, `TopicModelDriver`, `DataReader`, `LDA`. Add `src/main/resources/log4j2.xml` with a console appender. |
| 3.5 | **Simplify `RandomUtils`** | Remove the abstract inner-class hierarchy. Replace with a functional design: a private `DoubleSupplier` field set at construction time. Expose two factory-style constructors: `new RandomUtils()` (native) and `new RandomUtils(long seed)` (deterministic). The seed must remain `22` to preserve the deterministic sequence. See guard 3-T7 first. |
| 3.6 | **Apply `var` and modern syntax in `LDA.java`** | Apply `var` for local variable type inference throughout `LDA.java`. Use enhanced switch/pattern matching where applicable. This is a purely syntactic change; no logic should change. |
| 3.7 | **Rename and convert `ModelCountainer` to a record** | Fix the typo: `ModelCountainer` → `ModelContainer`. Convert to a record. Because `double[]` and `int[]` are mutable, document that the record provides referential immutability only. |
| 3.8 | **Add `log4j2.xml` configuration file** | Create `src/main/resources/log4j2.xml` with a `Console` appender using pattern `%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n` and root level `INFO`. |

### Guard Tests for Sprint 3

| # | Guard Test | When |
|---|---|---|
| 3-T1 | **`Feature` accessor rename** — update `FeatureTest`, `DocumentTest`, and `DataReaderTest` to call `f.featureType()` / `f.featureName()` / `f.featureValue()` instead of `f.getFeatureType()` etc. Run tests on the existing POJO first to confirm they pass, then convert to a record. | Before task 3.1 |
| 3-T2 | **`Feature` Gson snake_case deserialization** — the JSON format uses `feature_type`, `feature_name`, `feature_value` (snake_case) while the record uses camelCase. Write a test asserting `gson.fromJson("{\"feature_type\":\"TOKEN\",\"feature_name\":\"word\",\"feature_value\":1.0}", Feature.class)` returns correct values. If it fails, register `FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES` in a `GsonBuilder` inside `DataReader`. | Before task 3.1 |
| 3-T3 | **`Document` record `toString` override** — `DocumentTest.testToString()` asserts `"abc 1"`. Java records auto-generate `toString()` as `Document[docId=abc, features=[...]]`. Write the assertion first (it will fail), forcing `toString()` to be overridden before the record conversion is considered complete. | Before task 3.2 |
| 3-T4 | **`Document` record Gson round-trip** — JSON has `doc_id` (snake_case). Write `DataReaderTest` cases that parse a known JSON line and assert `doc.docId()` equals the expected value. Register `FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES` in `DataReader`'s `Gson` instance if needed. | Before task 3.2 |
| 3-T5 | **`Message` HashMap null behavior** — `Hashtable` throws `NullPointerException` on null keys or values; `HashMap` does not. Write a test: `msg.setParam("x", null)` followed by `msg.getParam("x") == null`. Verify `MessageTest` still passes after the swap. | Before task 3.3 |
| 3-T6 | **SLF4J facade no-crash** — after task 3.4, run `./gradlew test` and confirm no `ClassNotFoundException` for `LogManager` or `Logger`. Add a test that instantiates `DataReader` and invokes `read()` on the test fixture; any logger misconfiguration will throw at field initialization. | After task 3.4 |
| 3-T7 | **`RandomUtils` sequence preservation** — before task 3.5, record the full 5-value `nextDouble()` sequence for seed 22 (from B.6). After refactoring, assert the deterministic constructor with seed 22 produces the **identical** sequence to 1e-12. Re-run `MultinomialDistributionTest` in full (it uses `new RandomUtils(1)` with 1,000,000 samples). | Before and after task 3.5 |
| 3-T8 | **`ModelContainer` rename regression** — after task 3.7, re-run the golden LDA integration test (B.1) and assert the perplexity matches the baseline value within 1e-6. The rename and record conversion must not change sampling behavior. | After task 3.7 |
| 3-T9 | **`var` refactor safety** — task 3.6 is purely syntactic. Run `./gradlew check` with `-Xlint:all` and assert zero new warnings in modified files. | After task 3.6 |

---

## Sprint 4 — Python Wrapper Modernization

**Goal**: Full Python 3.10+ compatibility; typed; installable via pip.

### Implementation Tasks

| # | Task | Detail |
|---|---|---|
| 4.1 | **Fix all `iteritems()` calls** | Replace `.iteritems()` with `.items()` in `compute_dictionary()`, `load_model()`, and `load_models()`. |
| 4.2 | **Remove Python 2 codec boilerplate** | Delete the `codecs.getreader('utf-8')(sys.stdin)` and `codecs.getwriter('utf8')(sys.stdout)` wrapping in `__main__`. Python 3 handles UTF-8 natively. |
| 4.3 | **Replace bare `except:` with typed exceptions** | Change `except:` to `except (json.JSONDecodeError, KeyError):` in `parse_docs()` and `load_raw()`. Bare except clauses swallow `KeyboardInterrupt` and `SystemExit`. |
| 4.4 | **Use context managers for all file I/O** | Refactor every `open()` / `close()` pattern to `with open(...) as f:` throughout the module. |
| 4.5 | **Add `pyproject.toml` for packaging** | Create `pyproject.toml` with `[build-system]` (setuptools), `[project]` metadata (name, version, requires-python = ">=3.10"), and an entry point `fugue = fugue.cli:main`. Move Python source to `src/fugue/` package layout. |
| 4.6 | **Add type hints** | Annotate all function signatures: `str`, `list[str]`, `dict[str, int]`, `dict[str, float]`, `argparse.Namespace`, return types. |
| 4.7 | **Add ruff linting and mypy type checking to CI** | Add a `python-lint` job to `.github/workflows/ci.yml`: `pip install ruff mypy && ruff check src/ && mypy src/` on Python 3.10, 3.11, and 3.12. |

### Guard Tests for Sprint 4

| # | Guard Test | When |
|---|---|---|
| 4-T1 | **`iteritems()` output equivalence** — before task 4.1, run `compute_dictionary` and `load_models` on a small fixture and capture output to a file. After the fix, assert byte-for-byte identical output. | Before task 4.1 |
| 4-T2 | **`compute_dictionary` ordering stability** — write a pytest test with a fixed input dict (known collision-heavy keys) and assert the sorted output list is identical before and after the change. Python 3 `dict.items()` is insertion-ordered (3.7+) but ordering of frequency-sorted results must be verified. | Before task 4.1 |
| 4-T3 | **`parse_docs` exception handling** — write tests: (a) valid JSON line → document written to output, (b) malformed JSON (`{bad}`) → line skipped, no crash, (c) JSON missing `title` key → line skipped, no crash. | Before task 4.3 |
| 4-T4 | **File handle leak check** — after task 4.4, write a test that calls `parse_docs` on a 3-line fixture and then immediately opens the output file for reading. A file still held open would cause a permission error on Windows; on Linux it verifies no unclosed handle via `/proc/self/fd`. | After task 4.4 |
| 4-T5 | **`tokenize` and `cleaned` edge cases** — add to `tests/test_utils.py`: `tokenize("") == []`, `tokenize("123") == []`, `cleaned("") == ""`, `cleaned("a") == ""` (single char filtered), `cleaned("ab") == "ab"`. | Before task 4.6 |
| 4-T6 | **Installable package smoke test** — after task 4.5, run `pip install -e . --quiet` in a fresh venv and assert `python -c "from fugue.cli import main"` imports without error. | After task 4.5 |
| 4-T7 | **Python test matrix** — the CI `python-lint` job must not be the only Python CI job. Add a `python-test` job that runs `pytest tests/ -v --tb=short` on Python 3.10, 3.11, and 3.12. | Task 4.7 |

---

## Sprint 5 — Tests, Docs & Release

**Goal**: Higher test confidence, Javadoc published, SBOM generated, 1.0 shipped.

### Implementation Tasks

| # | Task | Detail |
|---|---|---|
| 5.1 | **Expand `DataReaderTest`** | Add tests for: valid multi-doc file, doc with zero TOKEN features is excluded, file with mixed feature types (only TOKENs pass through), `topk` boundary values. |
| 5.2 | **Add parameterized JUnit 5 tests for `LogGamma` and `MathExp`** | Use `@ParameterizedTest` + `@CsvSource` with known mathematical identities: `Γ(1) = 0`, `Γ(0.5) = √π/2`, `Γ(n) = (n-1)!` for integer n. For `MathExp`: `exp(0) = 1`, `exp(1) ≈ 2.71828`, and Jafama vs `Math.exp` agreement to 1e-6. |
| 5.3 | **Add property-based tests for `MultinomialDistribution`** | Add `net.jqwik:jqwik` dependency. Write properties: (a) sampled index is always in `[0, dimensions)`, (b) 100,000 samples from a uniform distribution have each bucket within 5σ of the expected count, (c) setting all mass on one index always samples that index. |
| 5.4 | **Add `cyclonedx-gradle-plugin` for SBOM** | Add `id("org.cyclonedx.bom")` to `build.gradle.kts`. Wire `cyclonedxBom` into the `check` lifecycle. Output `build/reports/bom.json`. |
| 5.5 | **Enable Javadoc generation** | Add `javadoc` task configuration to `build.gradle.kts`. Write `/** */` doc comments on all public API classes: `MainEntrance`, `LDA`, `DataReader`, `TopicModel`, `TopicModelDriver`, `Message`. |
| 5.6 | **Add GitHub Actions release workflow** | Write `.github/workflows/release.yml`: trigger on `v*` tag push. Steps: checkout → setup-java → `./gradlew fatJar` → create GitHub Release → upload the fat JAR as a release asset. |
| 5.7 | **Bump version to `1.0` and write `CHANGELOG.md`** | Update version in `build.gradle.kts`. Create `CHANGELOG.md` documenting all changes from 0.1 across Sprints 1–5. |
| 5.8 | **Update `README.md`** | Replace Travis CI badge with GitHub Actions badge. Update Java version requirement to 21. Add `pip install` instructions for the Python wrapper. Add a quickstart example. |

### Guard Tests for Sprint 5

| # | Guard Test | When |
|---|---|---|
| 5-T1 | **New tests run in CI** — each new test class from tasks 5.1–5.3 must be confirmed present in CI output (test count gate from 2-T1 increases accordingly). | Tasks 5.1–5.3 |
| 5-T2 | **Fat JAR end-to-end smoke** — add a CI step that executes: `java -jar build/libs/fugue-topicmodeling-all-*.jar --task train --inputFile src/test/resources/ap-test.json --topics 5 --iters 3 --saveModel 0` and asserts exit code 0. This is the final end-to-end gate before any release. | Task 5.6 |
| 5-T3 | **SBOM completeness check** — after task 5.4, parse `build/reports/bom.json` and assert it contains entries for `log4j`, `gson`, `commons-cli`, and `jafama`. A missing entry indicates plugin misconfiguration. | Task 5.4 |
| 5-T4 | **Javadoc zero-error build** — `./gradlew javadoc` must produce zero errors (warnings are allowed). Add as a required CI check. | Task 5.5 |
| 5-T5 | **Version consistency check** — add a CI step that extracts the version from `build.gradle.kts`, the first heading in `CHANGELOG.md`, and the JAR manifest, and asserts all three match. | Task 5.7 |
| 5-T6 | **Release workflow dry-run** — before tagging `v1.0`, trigger the release workflow via `workflow_dispatch` with `dry-run: true` to confirm the JAR builds and the upload step is reachable without actually publishing. | Task 5.6 |

---

## Testing Principles

1. **Establish a golden baseline first.** Capture deterministic outputs (perplexity, RNG sequences,
   Python dict output) before any sprint begins. Assert against them after every sprint.

2. **Write regression guards before the task that could break them.** Tests come first, then the
   change. A test that passes on the old code and would fail on a broken new implementation is a
   true regression guard.

3. **Three test tiers must all be green before merging any sprint.**
   - *Unit*: pure logic, no I/O (math utils, distributions, Message, Feature, Document).
   - *Integration*: component wiring (DataReader + Gson + serialization types; LDA + RandomUtils).
   - *End-to-end*: full pipeline (fat JAR CLI → train → model file written).

4. **Java and Python tested independently.** Gradle suite and pytest suite are separate CI jobs.
   Both must be green; one green does not compensate for the other failing.

5. **Test count gate enforced across sprints.** The number of executed tests must not decrease
   between sprints. A drop indicates silently removed or ignored tests.

---

## Critical Path Warnings

Three tasks have **hard test-first requirements**. Write the test, confirm it passes on the current
code, then make the change.

### Warning 1 — `Feature` accessor rename (before Sprint 3, task 3.1)

`FeatureTest`, `DocumentTest`, and `DataReaderTest` all call `f.getFeatureType()`,
`f.getFeatureName()`, and `f.getFeatureValue()`. Java records generate `featureType()`,
`featureName()`, and `featureValue()` — without the `get` prefix. If the tests are not updated
before the record conversion, the build will fail to compile. Update all three test files first,
confirm they compile and pass against the existing POJO, then convert to a record.

Additionally, Gson deserializes JSON field `feature_type` (snake_case) into a record component
named `featureType` (camelCase). Without `FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES` registered
on the `GsonBuilder`, `DataReader` will silently produce `null` values for all fields. Write and
run the deserialization test (3-T2) before the record conversion.

### Warning 2 — `RandomUtils` sequence preservation (before Sprint 3, task 3.5)

`MultinomialDistributionTest` uses `new RandomUtils(1)` to draw 1,000,000 samples and asserts the
empirical distribution matches the theoretical one within tight tolerances. Any change to the
internal random sequence (different seed wiring, different call order) will cause this test to fail
in a way that looks like a statistical fluke. Capture the full deterministic sequence (B.6) before
refactoring, and assert it is identical after the refactor. The deterministic seed must remain `22`
(hardcoded in `DeterministicRandom`).

### Warning 3 — Python `iteritems()` output capture (before Sprint 4, task 4.1)

Python 2's `dict.iteritems()` iterated in an unspecified (hash-dependent) order. Python 3's
`.items()` is insertion-ordered (guaranteed since 3.7). For dictionaries built by counting
operations, insertion order depends on the order items were first seen — which may differ from the
implicit order a Python 2 program happened to rely on. Capture the output of `compute_dictionary`
and `load_models` on a small fixture before the change and assert byte-for-byte identical output
after. Any ordering difference in the sorted output reveals a pre-existing Python 2 assumption.
