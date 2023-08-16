package decaf.ir.codes;


import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import decaf.analysis.syntax.ast.AST;
import decaf.ir.names.IrValue;
import decaf.synthesis.asm.AsmWriter;

public abstract class Instruction {
  public static final String INDENT = "    ";
  public static final String DOUBLE_INDENT = INDENT + INDENT;
  private final UUID uuid;
  private AST source;
  private String comment;

  public Instruction(
      AST source,
      String comment
  ) {
    this.comment = comment;
    this.setSource(source);
    uuid = UUID.randomUUID();
  }

  public Instruction(AST source) {
    this(
        source,
        null
    );
  }

  public Instruction() {
    this(
        null,
        null
    );
  }

  public UUID getUuid() {
    return uuid;
  }

  public Optional<String> getComment() {
    return Optional.ofNullable(comment);
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public abstract void accept(AsmWriter asmWriter);

  public abstract List<IrValue> genIrValuesSurface();

  public List<IrValue> genIrValues() {
    return genIrValuesSurface();
  }

  public <T extends IrValue> List<T> genIrValuesFiltered(Class<T> tClass) {
    return genIrValuesSurface().stream()
                               .filter(irValue -> tClass.isAssignableFrom(irValue.getClass()))
                               .map(tClass::cast)
                               .toList();
  }

  public List<IrValue> genIrValuesFiltered(Predicate<IrValue> irValuePredicate) {
    return genIrValuesSurface().stream()
                               .filter(irValuePredicate)
                               .toList();
  }

  public abstract String syntaxHighlightedToString();

  public abstract Instruction copy();

  public String noCommentsSyntaxHighlighted() {
    if (getComment().isPresent()) {
      String comment = getComment().get();
      setComment(null);
      var res = syntaxHighlightedToString();
      setComment(comment);
      return res;
    }
    return syntaxHighlightedToString();
  }

  public String noCommentsToString() {
    if (getComment().isPresent()) {
      String comment = getComment().get();
      setComment(null);
      var res = toString();
      setComment(comment);
      return res;
    }
    return toString();
  }

  public AST getSource() {
    return source;
  }

  public void setSource(AST source) {
    this.source = source;
  }
}
