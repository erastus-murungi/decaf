package edu.mit.compilers.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        List<File> files = new ArrayList<>();
        try {
            files = Files.list(Paths.get(filepath))
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .collect(Collectors.toList());

            files.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files.stream().sorted(Comparator.comparing(File::getName)).collect(Collectors.toList());
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
        var doubleSummaryStatistics =  DoubleStream.of(reductionRatios).summaryStatistics();
        System.out.format("\t%s\t%10.4f%%\n", "AVERAGE REDUCTION", doubleSummaryStatistics.getAverage() * 100);
        System.out.format("\t%s\t%10d\n", "TOTAL #ASM LINES REMOVED REDUCTION", IntStream.of(nLinesRemoved).summaryStatistics().getSum());

    }

    private static Compilation compileTest(File testFile) throws IOException {
        var compilation = new Compilation(readFile(testFile), false);
        compilation.run();
        return compilation;
    }
}
