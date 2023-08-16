import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import decaf.shared.TestRunner;

public class MainTest {
  @Test
  public void testScanner() {
    assertTrue(TestRunner.testScanner(false));
  }

  @Test
  public void testParser() {
    assertTrue(TestRunner.testParser(false));
  }
  @Test
  public void testSemantics() {
    assertTrue(TestRunner.testSemantics(false));
  }
}
