package edu.mit.compilers.utils;

import com.google.common.base.Stopwatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class TestRunner {
    public static final String DEFAULT_DATAFLOW_TESTS_ROOT = "tests/optimizer/dcf";

    public static void run() {
        try {
            compileTests(DEFAULT_DATAFLOW_TESTS_ROOT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
    }

    private static List<File> getAllTestFiles(String filepath) {
        try (var files = Files.list(Paths.get(filepath))) {
            return files.map(Path::toFile)
                    .filter(File::isFile).sorted(Comparator.comparing(File::getName)).toList();
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }


    public static void testCodegen() {
        final var filepath = "tests/codegen/input";
        var allTestFiles = getAllTestFiles(filepath);

        int nPassed = 0;
        for (var file: allTestFiles) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            System.out.print("compiling... " + file.getName());
            try {
                var compilation = compileTest(file);
                var expectedOutputFile = "./tests/codegen/output/" + file.getName() + ".out";
                var expected = readFile(Paths.get(expectedOutputFile).toFile());
                System.out.println("\rcompiled... " + file.getName() + " in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
                if (!expected.strip().equals(compilation.output.strip())) {
                    System.out.println(Utils.coloredPrint("\tFAIL: " + file.getName(), Utils.ANSIColorConstants.ANSI_RED));
                    System.out.println("EXPECTED:");
                    System.out.println(expected);
                    System.out.println("RECEIVED:");
                    System.out.println(compilation.output);
                } else {
                    System.out.println(Utils.coloredPrint("\tPASSED: " + file.getName(), Utils.ANSIColorConstants.ANSI_GREEN_BOLD));
                    nPassed += 1;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.format("Passed %d out of %d\n", nPassed, allTestFiles.size());
    }

    private static void compileTests(String filepath) throws IOException {
        List<File> allTestFiles = getAllTestFiles(filepath);
        var nTestFiles = allTestFiles.size();

        String[] fileNames = new String[nTestFiles];
        double[] reductionRatios = new double[nTestFiles];
        int[] nLinesRemoved = new int[nTestFiles];
        for (var indexOfTestFile = 0; indexOfTestFile < nTestFiles; indexOfTestFile++) {
            var testFile = allTestFiles.get(indexOfTestFile);
            if (testFile.getName().equals("test.dcf"))
                continue;
            System.out.println(testFile.getAbsolutePath());
            var compilation = compileTest(testFile);
            fileNames[indexOfTestFile] = testFile.getName();
            reductionRatios[indexOfTestFile] = compilation.getNLinesOfCodeReductionFactor();
            nLinesRemoved[indexOfTestFile] = compilation.getNLinesRemovedByAssemblyOptimizer();
        }
        System.out.format("%10s\t%20s\t%15s\t%15s\n", "INDEX", "FILE NAME", "REDUCTION RATIO", "#ASM LINES REMOVED");
        for (int i = 0; i < nTestFiles; i++) {
            System.out.format("%10d\t%20s\t%10.4f%%\t%10d\n", i, fileNames[i], reductionRatios[i] * 100, nLinesRemoved[i]);
        }
        var doubleSummaryStatistics = DoubleStream.of(reductionRatios).summaryStatistics();
        System.out.format("\t%s\t%10.4f%%\n", "AVERAGE REDUCTION", doubleSummaryStatistics.getAverage() * 100);
        System.out.format("\t%s\t%10d\n", "TOTAL #ASM LINES REMOVED REDUCTION", IntStream.of(nLinesRemoved).summaryStatistics().getSum());

    }

    private static Compilation compileTest(File testFile) throws IOException {
        var compilation = new Compilation(readFile(testFile), false);
        compilation.run();
        return compilation;
    }
}
