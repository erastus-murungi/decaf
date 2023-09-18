package decaf.shared.env;


import decaf.analysis.syntax.ast.types.ArrayType;
import decaf.analysis.syntax.ast.types.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import decaf.analysis.syntax.ast.Block;
import decaf.shared.descriptors.Descriptor;
import decaf.shared.descriptors.MethodDescriptor;


public class Scope extends HashMap<String, Descriptor> {
  @NotNull
  public final For target;
  @Nullable
  public final Block owner;
  @Nullable
  public Scope parent;
  @NotNull
  public List<Scope> children;

  public Scope(
      @Nullable Scope parent,
      @NotNull For target,
      @Nullable Block owner
  ) {
    super();
    this.parent = parent;
    this.target = target;
    this.owner = owner;
    this.children = new ArrayList<>();
  }

  public Scope(@NotNull For target) {
    super();
    this.parent = null;
    this.target = target;
    this.owner = null;
    this.children = new ArrayList<>();
  }

  public static Scope forGlobals() {
    return new Scope(
        For.Field
    );
  }

  public Optional<MethodDescriptor> lookupMethod(@NotNull String methodName) {
    return lookup(methodName)
               .filter(descriptor -> descriptor instanceof MethodDescriptor)
               .map(descriptor -> (MethodDescriptor) descriptor);
  }

  public Optional<Descriptor> lookupImport(@NotNull String stringId) {
    return lookup(stringId)
               .filter(Descriptor::isImport);
  }

  public void addDescriptor(@NotNull String label, @NotNull Descriptor descriptor) {
    put(label, descriptor);
    descriptor.setEnclosingScope(this);
  }

  private static String padRight(
      String s,
      Optional<Integer> n
  ) {
    return n.map(integer -> s + " ".repeat(integer + 4 - s.length()))
            .orElse(s);
  }

  /**
   * Look up a irAssignableValue recursively up the scope hierarchy
   *
   * @param stringId the id to lookup in the symbol table hierarchy
   * @return Optional.empty if the descriptor is not found else Optional[Descriptor]
   */
  public Optional<Descriptor> lookup(String stringId) {
    var currentScope = this;
    while (currentScope != null && !currentScope.containsKey(stringId)) {
      currentScope = currentScope.parent;
    }
    if (currentScope == null) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(currentScope.get(stringId));
    }
  }

  public Optional<Descriptor> lookupNonMethod(@NotNull String stringId) {
    return lookup(stringId)
               .filter(descriptor -> !(descriptor instanceof MethodDescriptor));
  }

  /**
   * Look up a irAssignableValue recursively up the scope hierarchy to see is there is incorrect shadowing parameter
   *
   * @param stringId the id to lookup in the symbol table hierarchy
   * @return true if there is incorrect shadowing of parameter and false otherwise
   */
  public boolean isShadowingParameter(String stringId) {
    Descriptor currentDescriptor = get(stringId);
    if (parent == null) {
      return currentDescriptor != null && target == For.Arguments;
    } else {
      if (currentDescriptor != null && target == For.Arguments)
        return true;
      else
        return parent.isShadowingParameter(stringId);
    }
  }

  public String toString() {
    return myToString(
        "",
        ""
    );
  }

  public String myToString(String suffix) {
    return myToString(
        "",
        suffix
    );
  }

  public String myToString(
      String indent,
      String suffix
  ) {
    var repeat = " ".repeat(Math.max(
        0,
        indent.length() - 8
    ));
    var repeat1 = "-".repeat(Math.min(
        indent.length(),
        8
    ));
    if (isEmpty()) {
      return (repeat + repeat1 + "EmptySymbolTable " + suffix);
    }
    final var IDENTIFIER = "Identifier";
    final var DESCRIPTOR_CLASSES = "Descriptor Types";
    final var BUILTIN_TYPES = "Builtin Types";
    final var ARRAY_LENGTH = "Array Length";

    var maxLengthIds = Stream.concat(
                                 Stream.of(
                                     IDENTIFIER,
                                     "-".repeat(IDENTIFIER.length())
                                 ),
                                 keySet()
                                     .stream()
                                     .map(Object::toString)
                             )
                             .map(String::length)
                             .reduce(Math::max);

    var maxLengthIdsStream = Stream.concat(
        Stream.of(
            IDENTIFIER,
            "-".repeat(IDENTIFIER.length())
        ),
        keySet()
            .stream()
            .map(Object::toString)
    );

    var ids = maxLengthIdsStream.map(((String s) -> padRight(
                                    s,
                                    maxLengthIds
                                )))
                                .toList();

    var maxMethodD = Stream.concat(
                               Stream.of(
                                   DESCRIPTOR_CLASSES,
                                   "-".repeat(DESCRIPTOR_CLASSES.length())
                               ),
                               keySet()
                                   .stream()
                                   .map(Object::getClass)
                                   .map(Class::getSimpleName)
                           )
                           .map(String::length)
                           .reduce(Math::max);
    var descriptorTypes = Stream.concat(
                                    Stream.of(
                                        DESCRIPTOR_CLASSES,
                                        "-".repeat(DESCRIPTOR_CLASSES.length())
                                    ),
                                    values()
                                        .stream()
                                        .map(Object::getClass)
                                        .map(Class::getSimpleName)
                                )
                                .map(s -> padRight(
                                    s,
                                    maxMethodD
                                ))
                                .toList();

    var list1 = new ArrayList<>(values());
    var builtins = new ArrayList<String>();
    for (var descriptor1 : list1) {
      Type type = descriptor1.getType();
      String toString = type.toString();
      builtins.add(toString);
    }
    var maxLengthTypes = Stream.concat(
                                   Stream.of(
                                       BUILTIN_TYPES,
                                       "-".repeat(BUILTIN_TYPES.length())
                                   ),
                                   builtins.stream()
                               )
                               .map(String::length)
                               .reduce(Math::max);
    List<String> builtinTypes = Stream.concat(
                                          Stream.of(
                                              BUILTIN_TYPES,
                                              "-".repeat(BUILTIN_TYPES.length())
                                          ),
                                          builtins.stream()
                                      )
                                      .map(Object::toString)
                                      .map((String s) -> padRight(
                                          s,
                                          maxLengthTypes
                                      ))
                                      .toList();

    List<String> list = new ArrayList<>();
    for (Descriptor descriptor : list1) {
      String o;
      if (descriptor.isForArray()) {
        var arrayType = (ArrayType) descriptor.getType();
        o = String.valueOf(arrayType.getNumElements());
      } else {
        o = "N / A";
      }
      list.add(o);
    }
    Optional<Integer> maxLengthArraySize = Stream.concat(
                                                     Stream.of(
                                                         ARRAY_LENGTH,
                                                         "-".repeat(ARRAY_LENGTH.length())
                                                     ),
                                                     list.stream()
                                                 )
                                                 .map(String::length)
                                                 .reduce(Math::max);
    List<String> arraySizes = Stream.concat(
                                        Stream.of(
                                            ARRAY_LENGTH,
                                            "-".repeat(ARRAY_LENGTH.length())
                                        ),
                                        list.stream()
                                    )
                                    .map(s -> padRight(
                                        s,
                                        maxLengthArraySize
                                    ))
                                    .toList();


    List<String> rows = new ArrayList<>();
    rows.add(repeat + repeat1 + "SymbolTable: " + suffix);
    for (int i = 0; i < ids.size(); i++) {
      rows.add(indent + String.join(
          "",
          ids.get(i),
          descriptorTypes.get(i),
          builtinTypes.get(i),
          arraySizes.get(i)
      ));
    }
    return String.join(
        "\n",
        rows
    );
  }

  public enum For {
    Arguments,
    Field
  }
}
