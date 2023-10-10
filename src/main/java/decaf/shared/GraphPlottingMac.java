package decaf.shared;

import decaf.analysis.cfg.CfgBlock;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.function.Function;

public class GraphPlottingMac {
    private static final String TEMP_DIR = "./dot";
    private static final String DOT = "/opt/homebrew/bin/dot";
    private static final int DPI = 106;
    private final StringBuilder graph = new StringBuilder();

    private GraphPlottingMac() {
    }

    public static void createDotGraph(String dotFormat, String fileName) {
        GraphPlottingMac gv = new GraphPlottingMac();
        gv.addWithNewLine(gv.addGraphPrologue());
        gv.add(dotFormat);
        gv.addWithNewLine(gv.addGraphEpilogue());
        String type = "pdf";
        File out = new File("graph/" + fileName + "." + type);
        gv.writeGraphToFile(gv.getGraph(gv.getDotSource(), type), out);
    }

    public static String writeCFG(String methodName, CompilationContext compilationContext) {
        return writeCFG(methodName, compilationContext, CfgBlock::getSourceCode);
    }

    public static String writeCFG(@NotNull String methodName,
                                  @NotNull CompilationContext compilationContext,
                                  @NotNull Function<CfgBlock, String> labelFunction) {
        var subGraphs = new ArrayList<String>();
        subGraphs.add(String.format("subgraph cluster_%s { \n label = %s", escape(methodName), escape(methodName)));
        var stack = new Stack<CfgBlock>();
        var nodes = new ArrayList<String>();
        var edges = new ArrayList<String>();
        var seen = new HashSet<CfgBlock>();
        stack.push(compilationContext.getEntryCfgBlock(methodName).orElseThrow());
        while (!stack.isEmpty()) {
            var cfgBlock = stack.pop();
            if (seen.contains(cfgBlock)) {
                continue;
            } else {
                seen.add(cfgBlock);
            }
            if (compilationContext.isEntryBlock(cfgBlock)) {
                nodes.add(String.format("   %s [shape=record, style=filled, fillcolor=green, label=%s];",
                                        cfgBlock.hashCode(),
                                        "\"<from_node>" + escape(labelFunction.apply(cfgBlock)) + "\""
                                       ));
            }
            if (compilationContext.isExitBlock(cfgBlock)) {
                nodes.add(String.format("   %s [shape=record, style=filled, fillcolor=gray, label=%s];",
                                        cfgBlock.hashCode(),
                                        "\"<from_node>" + escape(labelFunction.apply(cfgBlock)) + "\""
                                       ));
            } else {
                nodes.add(String.format("   %s [shape=record, label=%s];",
                                        cfgBlock.hashCode(),
                                        "\"<from_node>" + escape(labelFunction.apply(cfgBlock)) + "\""
                                       ));
            }
            if (cfgBlock.getAlternateSuccessor().isPresent() && cfgBlock.getSuccessor().isPresent()) {
                nodes.add(String.format("   %s [shape=record, label=%s];",
                                        cfgBlock.hashCode(),
                                        "\"{<from_node>" +
                                        escape(labelFunction.apply(cfgBlock)) +
                                        "|{<from_true> T|<from_false>F}" +
                                        "}\""
                                       ));
                var trueChild = cfgBlock.getSuccessor().get();
                var falseChild = cfgBlock.getAlternateSuccessor().get();
                stack.push(falseChild);
                edges.add(String.format("   %s -> %s;",
                                        cfgBlock.hashCode() + ":from_false",
                                        falseChild.hashCode() + ":from_node"
                                       ));
                stack.push(trueChild);
                edges.add(String.format("   %s -> %s;",
                                        cfgBlock.hashCode() + ":from_true",
                                        trueChild.hashCode() + ":from_node"
                                       ));
            } else if (cfgBlock.getSuccessor().isPresent()) {
                var autoChild = cfgBlock.getSuccessor().get();
                edges.add(String.format("   %s -> %s;",
                                        cfgBlock.hashCode() + ":from_node",
                                        autoChild.hashCode() + ":from_node"
                                       ));
                stack.push(autoChild);
            } else {
                assert compilationContext.isExitBlock(cfgBlock);
                nodes.add(String.format("   %s [shape=record, style=filled, fillcolor=gray, label=%s];",
                                        cfgBlock.hashCode(),
                                        "\"<from_node>" + escape(labelFunction.apply(cfgBlock)) + "\""
                                       ));
            }
        }
        subGraphs.addAll(edges);
        subGraphs.addAll(nodes);
        subGraphs.add("}");

        return String.join("\n", subGraphs);
    }

    public static void printGraph(CompilationContext compilationContext, Function<CfgBlock, String> labelFunction) {
        printGraph(compilationContext, labelFunction, "cfg");
    }

    public static void printGraph(CompilationContext compilationContext) {
        printGraph(compilationContext, CfgBlock::getSourceCode, "cfg");
    }

    public static void printGraph(CompilationContext compilationContext, String filename) {
        printGraph(compilationContext, CfgBlock::getSourceCode, filename);
    }

    public static void printGraph(CompilationContext compilationContext,
                                  Function<CfgBlock, String> labelFunction,
                                  String graphFilename) {
        var dots = compilationContext.getEntryCfgBlocks()
                                     .keySet()
                                     .stream()
                                     .map(methodName -> writeCFG(methodName, compilationContext, labelFunction))
                                     .toList();
        GraphPlottingMac.createDotGraph(String.join("\n", dots), graphFilename);

    }

    /**
     * escape()
     * <p>
     * Escape a give String to make it safe to be printed or stored.
     *
     * @param s The input String.
     * @return The output String.
     **/
    public static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\r", "\\r")
                .replace("\f", "\\f")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("<", "\\<")
                .replace(">", "\\>")
                .replace("\n", "\\l")
                .replace("||", "\\|\\|")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}");

    }

    /**
     * Returns the graph's source description in dot language.
     *
     * @return Source of the graph in dot language.
     */
    public String getDotSource() {
        return this.graph.toString();
    }

    /**
     * Adds a string to the graph's source (without newline).
     */
    public void add(String line) {
        this.graph.append(line);
    }

    /**
     * Adds a string to the graph's source (with newline).
     */
    public void addWithNewLine(String line) {
        this.graph.append(line).append("\n");
    }

    /**
     * Returns the graph as an image in binary format.
     *
     * @param dot_source Source of the graph to be drawn.
     * @param type       Type of the output image to be produced, e.g.: gif, dot, fig, pdf, ps, svg, png.
     * @return A byte array containing the image of the graph.
     */
    public byte[] getGraph(String dot_source, String type) {
        File dot;
        byte[] img_stream;

        try {
            dot = writeDotSourceToFile(dot_source);
            if (dot != null) {
                img_stream = get_img_stream(dot, type);
                if (!dot.delete()) {
                    System.err.println("Warning: " + dot.getAbsolutePath() + " could not be deleted!");
                }
                return img_stream;
            }
            return null;
        } catch (java.io.IOException ioe) {
            return null;
        }
    }

    /**
     * Writes the graph's image in a file.
     *
     * @param img A byte array containing the image of the graph.
     * @param to  A File object to where we want to write.
     */
    public void writeGraphToFile(byte[] img, File to) {
        try {
            FileOutputStream fos = new FileOutputStream(to);
            fos.write(img);
            fos.close();
        } catch (java.io.IOException ignored) {
        }
    }

    /**
     * It will call the external dot program, and return the image in
     * binary format.
     *
     * @param dot  Source of the graph (in dot language).
     * @param type Type of the output image to be produced, e.g.: gif, dot, fig, pdf, ps, svg, png.
     * @return The image of the graph in .gif format.
     */
    private byte[] get_img_stream(File dot, String type) {
        File img;
        byte[] img_stream = null;

        try {
            img = File.createTempFile("graph_", "." + type, new File(GraphPlottingMac.TEMP_DIR));
            Runtime rt = Runtime.getRuntime();

            String[] args = {DOT, "-T" + type, "-Gdpi=" + DPI, dot.getAbsolutePath(), "-o", img.getAbsolutePath()};
            Process p = rt.exec(args);

            p.waitFor();

            FileInputStream in = new FileInputStream(img.getAbsolutePath());
            img_stream = new byte[in.available()];
            in.read(img_stream);
            // Close it if we need to
            in.close();

            if (!img.delete()) {
                System.err.println("Warning: " + img.getAbsolutePath() + " could not be deleted!");
            }
        } catch (java.io.IOException ioe) {
            System.err.println("Error:    in I/O processing of tempfile in dir " + GraphPlottingMac.TEMP_DIR + "\n");
            System.err.println("       or in calling external command");
            ioe.printStackTrace();
        } catch (java.lang.InterruptedException ie) {
            System.err.println("Error: the execution of the external program was interrupted");
            ie.printStackTrace();
        }

        return img_stream;
    }

    /**
     * Writes the source of the graph in a file, and returns the written file
     * as a File object.
     *
     * @param str Source of the graph (in dot language).
     * @return The file (as a File object) that contains the source of the graph.
     */
    private File writeDotSourceToFile(String str) throws java.io.IOException {
        File temp;
        try {
            temp = File.createTempFile("dorrr", ".dot", new File(GraphPlottingMac.TEMP_DIR));
            FileWriter fout = new FileWriter(temp);
            fout.write(str);
            BufferedWriter br = new BufferedWriter(new FileWriter("dotsource.dot"));
            br.write(str);
            br.flush();
            br.close();
            fout.close();
        } catch (Exception e) {
            System.err.println("Error: I/O error while writing the dot source to temp file!");
            return null;
        }
        return temp;
    }

    public String addGraphPrologue() {
        return "digraph G {  graph [fontname = \"Menlo\"];\n" +
               " node [fontname = \"Menlo\"];\n" +
               " edge [fontname = \"Menlo\"];";
    }

    public String addGraphEpilogue() {
        return "}";
    }

    public String addSubgraphPrologue(int clusterid) {
        return "subgraph cluster_" + clusterid + " {";
    }

    /**
     * Returns a string that is used to end a graph.
     *
     * @return A string to close a graph.
     */
    public String end_subgraph() {
        return "}";
    }

}