package edu.mit.compilers.utils;

// GraphViz.java - a simple API to call dot from Java programs

/*$Id$*/
/*
 ******************************************************************************
 *                                                                            *
 *                    (c) Copyright Laszlo Szathmary                          *
 *                                                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms of the GNU Lesser General Public License as published by   *
 * the Free Software Foundation; either version 2.1 of the License, or        *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful, but        *
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY *
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public    *
 * License for more details.                                                  *
 *                                                                            *
 * You should have received a copy of the GNU Lesser General Public License   *
 * along with this program; if not, write to the Free Software Foundation,    *
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA.                              *
 *                                                                            *
 ******************************************************************************
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Block;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.registerallocation.InterferenceGraph;
import edu.mit.compilers.symboltable.SymbolTable;

/**
 * <dl>
 * <dt>Purpose: GraphViz Java API
 * <dd>
 *
 * <dt>Description:
 * <dd> With this Java class you can simply call dot
 *      from your Java programs.
 * <dt>Example usage:
 * <dd>
 * <pre>
 *    GraphViz gv = new GraphViz();
 *    gv.addln(gv.start_graph());
 *    gv.addln("A -> B;");
 *    gv.addln("A -> C;");
 *    gv.addln(gv.end_graph());
 *
 *    System.out.println(gv.getDotSource());
 *
 *    String type = "gif";
 *    File out = new File("out." + type);   // out.gif in this example
 *    gv.writeGraphToFile( gv.getGraph( gv.getDotSource(), type ), out );
 * </pre>
 * </dd>
 *
 * </dl>
 *
 * @author Laszlo Szathmary (<a href="jabba.laci@gmail.com">jabba.laci@gmail.com</a>)
 * @version v0.1, 2003/12/04 (December) -- first release
 */
public class GraphVizPrinter {
    /**
     * Detects the client's operating system.
     */
    private final static String osName = System.getProperty("os.name")
            .replaceAll("\\s", "")
            .toLowerCase(Locale.ROOT);
    /**
     * ##############################################################
     * #                    Linux Configurations                    #
     * ##############################################################
     * # The dir. where temporary files will be created.
     * tempDirForLinux = /tmp
     * # Where is your dot program located? It will be called externally.
     * dotForLinux = /usr/bin/dot
     *
     * ##############################################################
     * #                   Windows Configurations                   #
     * ##############################################################
     * # The dir. where temporary files will be created.
     * tempDirForWindows = c:/temp
     * # Where is your dot program located? It will be called externally.
     * dotForWindows = "c:/Program Files (x86)/Graphviz 2.28/bin/dot.exe"
     *
     * ##############################################################
     * #                    Mac Configurations                      #
     * ##############################################################
     * # The dir. where temporary files will be created.
     * tempDirForMacOSX = /tmp
     * # Where is your dot program located? It will be called externally.
     * dotForMacOSX = /usr/local/bin/dot
     */

    /**
     * The dir. where temporary files will be created.
     */
    private static final String TEMP_DIR = "./dot";

    /**
     * Where is your dot program located? It will be called externally.
     */
    private static final String DOT = (osName.equals("windows")) ? "c:/Program Files (x86)/Graphviz 2.28/bin/dot.exe" : (osName.equals("macosx")) ? "/usr/local/bin/dot" : "/usr/bin/dot";

    /**
     * The image size in dpi. 96 dpi is normal size. Higher values are 10% higher each.
     * Lower values 10% lower each.
     * <p>
     * dpi patch by Peter Mueller
     */
    private final int[] dpiSizes = {46, 51, 57, 63, 70, 78, 86, 96, 106, 116, 128, 141, 155, 170, 187, 206, 226, 249};

    /**
     * Define the index in the image size array.
     */
    private int currentDpiPos = 7;
    /**
     * The source of the graph written in dot language.
     */
    private StringBuilder graph = new StringBuilder();

    /**
     * Constructor: creates a new GraphViz object that will contain
     * a graph.
     */
    public GraphVizPrinter() {
    }

    public static void createDotGraph(String dotFormat, String fileName) {
        GraphVizPrinter gv = new GraphVizPrinter();
        gv.addln(gv.start_graph());
        gv.add(dotFormat);
        gv.addln(gv.end_graph());
        // String type = "gif";
        String type = "pdf";
        // gv.increaseDpi();
        gv.decreaseDpi();
        gv.decreaseDpi();
        File out = new File(fileName + "." + type);
        gv.writeGraphToFile(gv.getGraph(gv.getDotSource(), type), out);
    }

    public static String writeDominatorGraph(Map<BasicBlock, BasicBlock> immediateDominators) {
        List<String> nodes = new ArrayList<>();
        List<String> edges = new ArrayList<>();
        for (Map.Entry<BasicBlock, BasicBlock> entry : immediateDominators.entrySet()) {
            nodes.add(String.format("   %s [shape=record, label=%s, color=blue];", entry.getKey()
                    .hashCode(), "\"<from_node>" + escape(entry.getKey()
                    .toString()).replace(" ", "\u2007") + "\""));
            edges.add(String.format("   %s -> %s;", entry.getValue()
                    .hashCode() + ":from_node", entry.getKey()
                    .hashCode() + ":from_node"));
        }

        nodes.addAll(edges);
        return String.join("\n", nodes);
    }

    public static void printDominatorTree(Map<BasicBlock, BasicBlock> immediateDominators) {
        createDotGraph(writeDominatorGraph(immediateDominators), "dom");
    }

    public static void printInterferenceGraph(InterferenceGraph interferenceGraph, String fileName) {
        createDotGraph(writeInterferenceGraph(interferenceGraph), fileName);
    }

    public static String writeInterferenceGraph(InterferenceGraph interferenceGraph) {
        List<String> nodes = new ArrayList<>();
        List<String> edges = new ArrayList<>();
        for (var entry : interferenceGraph.getUniqueInterferenceGraphEdges()) {
            nodes.add(String.format("   %s [shape=ellipse, style=filled, fillcolor=gray, label=\"%s\"];", entry.first()
                    .hashCode(), escape(entry.first()
                    .toString()).replace(" ", "\u2007")));
            nodes.add(String.format("   %s [shape=ellipse, style=filled, fillcolor=gray, label=\"%s\"];", entry.second()
                    .hashCode(), escape(entry.second()
                    .toString()).replace(" ", "\u2007")));
            edges.add(String.format("   %s -> %s [arrowhead=none];", entry.first()
                    .hashCode(), entry.second()
                    .hashCode()));
        }

        for (var entry : interferenceGraph.getUniqueMoveEdges()) {
            nodes.add(String.format("   %s [shape=ellipse, style=filled, fillcolor=gray, label=\"%s\"];", entry.first()
                    .hashCode(), escape(entry.first()
                    .toString()).replace(" ", "\u2007")));
            nodes.add(String.format("   %s [shape=ellipse, style=filled, fillcolor=gray, label=\"%s\"];", entry.second()
                    .hashCode(), escape(entry.second()
                    .toString()).replace(" ", "\u2007")));
            edges.add(String.format("   %s -> %s [arrowhead=none, style=dotted, label=\"%s\"];", entry.first()
                    .hashCode(), entry.second()
                    .hashCode(), String.format("%s <=> %s", escape(entry.first
                    .irAssignableValue()
                    .toString()), escape(entry.second
                    .irAssignableValue()
                    .toString()))));
        }

        nodes.addAll(edges);
        return String.join("\n", nodes);
    }

    public static String writeSymbolTable(AST root,
                                          HashMap<String, SymbolTable> methods) {
        List<String> subGraphs = new ArrayList<>();// add this node
        Stack<AST> stack = new Stack<>();
        List<String> nodes = new ArrayList<>();
        List<String> edges = new ArrayList<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            AST ast = stack.pop();
            for (Pair<String, AST> astPair : ast.getChildren()) {
                AST child = astPair.second;
                if (child instanceof Block) {
                    SymbolTable blockSymbolTable = ((Block) child).blockSymbolTable;
                    if (blockSymbolTable.isEmpty())
                        continue;
                    nodes.add(String.format("   %s [shape=record, label=%s, color=blue];", blockSymbolTable.hashCode(), "\"<from_node>" + escape(blockSymbolTable.toString()).replace(" ", "\u2007") + "\""));
                    for (SymbolTable symbolTable : blockSymbolTable.children) {
                        if (symbolTable.isEmpty())
                            continue;
                        edges.add(String.format("   %s -> %s;", blockSymbolTable.hashCode() + ":from_node", symbolTable.hashCode() + ":from_node"));
                    }
                }
                if (child instanceof MethodDefinition) {
                    SymbolTable blockSymbolTable = methods.get(((MethodDefinition) child).methodName.getLabel());
                    if (blockSymbolTable.isEmpty())
                        continue;
                    nodes.add(String.format("   %s [shape=record, label=%s, color=blue];", blockSymbolTable.hashCode(), "\"<from_node>" + escape(blockSymbolTable.myToString(((MethodDefinition) child).methodName.getLabel())).replace(" ", "\u2007") + "\""));
                    for (SymbolTable symbolTable : blockSymbolTable.children) {
                        if (symbolTable.isEmpty())
                            continue;
                        edges.add(String.format("   %s -> %s;", blockSymbolTable.hashCode() + ":from_node", symbolTable.hashCode() + ":from_node"));
                    }

                }
                stack.push(child);
            }
        }
        subGraphs.addAll(edges);
        subGraphs.addAll(nodes);

        return String.join("\n", subGraphs);
    }

    public static String writeCFG(String name, BasicBlock cfg, Function<BasicBlock, String> labelFunction) {
        List<String> subGraphs = new ArrayList<>();
        subGraphs.add(String.format("subgraph cluster_%s { \n label = %s", escape(name), escape(name)));
        // add this node
        Stack<BasicBlock> stack = new Stack<>();
        List<String> nodes = new ArrayList<>();
        List<String> edges = new ArrayList<>();
        stack.push(cfg);
        Set<BasicBlock> seen = new HashSet<>();

        while (!stack.isEmpty()) {
            BasicBlock cfgBlock = stack.pop();
            if (cfgBlock instanceof NOP) {
                nodes.add(String.format("   %s [shape=record, style=filled, fillcolor=gray, label=%s];", cfgBlock.hashCode(), "\"<from_node>" + escape(labelFunction.apply(cfgBlock)) + "\""));
                BasicBlock autoChild = cfgBlock.getSuccessor();
                if (autoChild != null) {
                    edges.add(String.format("   %s -> %s;", cfgBlock.hashCode() + ":from_node", autoChild.hashCode() + ":from_node"));
                    if (!seen.contains(autoChild)) {
                        stack.push(autoChild);
                        seen.add(autoChild);
                    }
                }
            } else if (cfgBlock.hasNoBranch()) {
                nodes.add(String.format("   %s [shape=record, label=%s];", cfgBlock.hashCode(), "\"<from_node>" + escape(labelFunction.apply(cfgBlock)) + "\""));
                BasicBlock autoChild = cfgBlock.getSuccessor();
                if (autoChild != null) {
                    edges.add(String.format("   %s -> %s;", cfgBlock.hashCode() + ":from_node", autoChild.hashCode() + ":from_node"));
                    if (!seen.contains(autoChild)) {
                        stack.push(autoChild);
                        seen.add(autoChild);
                    }
                }
            } else if (cfgBlock.hasBranch()) {
                nodes.add(String.format("   %s [shape=record, label=%s];", cfgBlock.hashCode(), "\"{<from_node>" + escape(labelFunction.apply(cfgBlock)) + "|{<from_true> T|<from_false>F}" + "}\""));
                BasicBlock falseChild = cfgBlock.getFalseTarget();
                BasicBlock trueChild = cfgBlock.getTrueTarget();
                if (falseChild != null) {
                    if ((!seen.contains(falseChild))) {
                        stack.push(falseChild);
                        seen.add(falseChild);
                    }
                    edges.add(String.format("   %s -> %s;", cfgBlock.hashCode() + ":from_false", falseChild.hashCode() + ":from_node"));
                }
                if (trueChild != null) {
                    if ((!seen.contains(trueChild))) {
                        stack.push(trueChild);
                        seen.add(trueChild);
                    }
                    edges.add(String.format("   %s -> %s;", cfgBlock.hashCode() + ":from_true", trueChild.hashCode() + ":from_node"));
                }
            }
        }
        subGraphs.addAll(edges);
        subGraphs.addAll(nodes);
        subGraphs.add("}");

        return String.join("\n", subGraphs);
    }

    public static void printGraph(HashMap<String, BasicBlock> methodCFGBlocks, Function<BasicBlock, String> labelFunction) {
        printGraph(methodCFGBlocks, labelFunction, "cfg");
    }

    public static void printGraph(HashMap<String, BasicBlock> methodCFGBlocks) {
        printGraph(methodCFGBlocks, (BasicBlock::getLinesOfCodeString), "cfg");
    }

    public static void printGraph(HashMap<String, BasicBlock> methodCFGBlocks, String filename) {
        printGraph(methodCFGBlocks, (BasicBlock::getLinesOfCodeString), filename);
    }

    public static void printGraph(HashMap<String, BasicBlock> methodCFGBlocks, Function<BasicBlock, String> labelFunction, String graphFilename) {
        final String[] graph = new String[methodCFGBlocks.size()];
        final Integer[] index = {0};
        methodCFGBlocks.forEach((k, v) -> graph[index[0]++] = writeCFG(k, v, labelFunction));
        GraphVizPrinter.createDotGraph(String.join("\n", graph), graphFilename);
//        if (!osName.equals("windows")) {
//            try {
//                Process process = Runtime.getRuntime().exec("open cfg.pdf");
//                System.out.println(Utils.getStringFromInputStream(process.getInputStream()));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public static void printSymbolTables(AST root,
                                         HashMap<String, SymbolTable> methods) {
        GraphVizPrinter.createDotGraph(writeSymbolTable(root, methods), "symbolTables");
//        if (!osName.equals("windows")) {
//            try {
//                Process process = Runtime.getRuntime().exec("open cfg.pdf");
//                System.out.println(Utils.getStringFromInputStream(process.getInputStream()));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
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
                .replace("}", "\\}")
                ;

    }

    /**
     * Increase the image size (dpi).
     */
    public void increaseDpi() {
        if (this.currentDpiPos < (this.dpiSizes.length - 1)) {
            ++this.currentDpiPos;
        }
    }

    /**
     * Decrease the image size (dpi).
     */
    public void decreaseDpi() {
        if (this.currentDpiPos > 0) {
            --this.currentDpiPos;
        }
    }

    public int getImageDpi() {
        return this.dpiSizes[this.currentDpiPos];
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
    public void addln(String line) {
        this.graph.append(line)
                .append("\n");
    }

    /**
     * Adds a newline to the graph's source.
     */
    public void addln() {
        this.graph.append('\n');
    }

    public void clearGraph() {
        this.graph = new StringBuilder();
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
        byte[] img_stream = null;

        try {
            dot = writeDotSourceToFile(dot_source);
            if (dot != null) {
                img_stream = get_img_stream(dot, type);
                if (!dot.delete())
                    System.err.println("Warning: " + dot.getAbsolutePath() + " could not be deleted!");
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
     * @param img  A byte array containing the image of the graph.
     * @param file Name of the file to where we want to write.
     * @return Success: 1, Failure: -1
     */
    public int writeGraphToFile(byte[] img, String file) {
        File to = new File(file);
        return writeGraphToFile(img, to);
    }

    /**
     * Writes the graph's image in a file.
     *
     * @param img A byte array containing the image of the graph.
     * @param to  A File object to where we want to write.
     * @return Success: 1, Failure: -1
     */
    public int writeGraphToFile(byte[] img, File to) {
        try {
            FileOutputStream fos = new FileOutputStream(to);
            fos.write(img);
            fos.close();
        } catch (java.io.IOException ioe) {
            return -1;
        }
        return 1;
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
            img = File.createTempFile("graph_", "." + type, new File(GraphVizPrinter.TEMP_DIR));
            Runtime rt = Runtime.getRuntime();

            // patch by Mike Chenault
            String[] args = {DOT, "-T" + type, "-Gdpi=" + dpiSizes[this.currentDpiPos], dot.getAbsolutePath(), "-o", img.getAbsolutePath()};
            Process p = rt.exec(args);

            p.waitFor();

            FileInputStream in = new FileInputStream(img.getAbsolutePath());
            img_stream = new byte[in.available()];
            in.read(img_stream);
            // Close it if we need to
            in.close();

            if (!img.delete())
                System.err.println("Warning: " + img.getAbsolutePath() + " could not be deleted!");
        } catch (java.io.IOException ioe) {
            System.err.println("Error:    in I/O processing of tempfile in dir " + GraphVizPrinter.TEMP_DIR + "\n");
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
            temp = File.createTempFile("dorrr", ".dot", new File(GraphVizPrinter.TEMP_DIR));
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

    /**
     * Returns a string that is used to start a graph.
     *
     * @return A string to open a graph.
     */
    public String start_graph() {
        return "digraph G {  graph [fontname = \"Courier New\"];\n" +
                " node [fontname = \"Courier\"];\n" +
                " edge [fontname = \"Courier\"];";
    }

    /**
     * Returns a string that is used to end a graph.
     *
     * @return A string to close a graph.
     */
    public String end_graph() {
        return "}";
    }

    /**
     * Takes the cluster or subgraph id as input parameter and returns a string
     * that is used to start a subgraph.
     *
     * @return A string to open a subgraph.
     */
    public String start_subgraph(int clusterid) {
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

    /**
     * Read a DOT graph from a text file.
     *
     * @param input Input text file containing the DOT graph
     *              source.
     */
    public void readSource(String input) {
        StringBuilder sb = new StringBuilder();

        try {
            FileInputStream fis = new FileInputStream(input);
            DataInputStream dis = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(dis));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            dis.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        this.graph = sb;
    }

    public static void printDominatorTree(Map<BasicBlock, BasicBlock> immediateDominators, String fileName) {
        createDotGraph(writeDominatorGraph(immediateDominators), fileName);
    }

} // end of class GraphViz