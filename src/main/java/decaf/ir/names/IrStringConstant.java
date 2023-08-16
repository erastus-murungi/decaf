package decaf.ir.names;


import java.util.Objects;

import decaf.analysis.syntax.ast.Type;
import decaf.ir.IndexManager;

public class IrStringConstant extends IrConstant {
  private final String content;
  private final String contentEscaped;

  public IrStringConstant(String content) {
    super(
        Type.String,
        IndexManager.genStringConstantLabel()
    );
    this.content = content;
    this.contentEscaped = content.substring(
                                     1,
                                     content.length() - 1
                                 )
                                 .translateEscapes();
  }

  private IrStringConstant(
      String label,
      String content,
      String contentEscaped
  ) {
    super(
        Type.String,
        label
    );
    this.content = content;
    this.contentEscaped = contentEscaped;
  }

  @Override
  public String toString() {
    return String.format("@.%s",
                         getLabel());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    IrStringConstant that = (IrStringConstant) o;
    return Objects.equals(
        getLabel(),
        that.getLabel()
    );
  }

  @Override
  public IrStringConstant copy() {
    return new IrStringConstant(
        label,
        content,
        contentEscaped
    );
  }

  public int size() {
    return contentEscaped.length();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLabel());
  }

  @Override
  public String getValue() {
    return content;
  }

}
