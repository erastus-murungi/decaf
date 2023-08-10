package decaf.common;

public class CompilationContext {
  private static String asmOutputFilename;
  private static String sourceFilename;
  private static boolean debugModeOn;

  public static String getAsmOutputFilename() {
    return asmOutputFilename;
  }

  public static void setAsmOutputFilename(String asmOutputFilename) {
    CompilationContext.asmOutputFilename = asmOutputFilename;
  }

  public static String getSourceFilename() {
    return sourceFilename;
  }

  public static void setSourceFilename(String sourceFilename) {
    CompilationContext.sourceFilename = sourceFilename;
  }

  public static boolean isDebugModeOn() {
    return debugModeOn;
  }

  public static void setDebugModeOn(boolean debugModeOn) {
    CompilationContext.debugModeOn = debugModeOn;
  }
}
