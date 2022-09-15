package decaf.symboltable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import decaf.ast.Type;
import decaf.ast.Block;
import decaf.descriptors.ArrayDescriptor;
import decaf.descriptors.Descriptor;


public class SymbolTable {
    public final SymbolTableType symbolTableType;
    public final HashMap<String, Descriptor> entries = new HashMap<>();
    public final Block owner;
    public SymbolTable parent;
    public ArrayList<SymbolTable> children = new ArrayList<>();

    public SymbolTable(SymbolTable parent, SymbolTableType symbolTableType, Block owner) {
        super();
        this.parent = parent;
        this.symbolTableType = symbolTableType;
        this.owner = owner;
    }

    private static String padRight(String s, Optional<Integer> n) {
        return n.map(integer -> s + " ".repeat(integer + 4 - s.length()))
                .orElse(s);
    }

    /**
     * Look up a irAssignableValue only within the current scope
     *
     * @param stringId the id to lookup in the symbol table hierarchy
     * @return Optional.empty if the descriptor is not found else Optional[Descriptor]
     */

    public Optional<Descriptor> getDescriptorFromCurrentScope(String stringId) {
        Descriptor currentDescriptor = entries.get(stringId);
        if (currentDescriptor == null) {
            if (parent != null)
                return parent.getDescriptorFromValidScopes(stringId);
        }
        return Optional.ofNullable(currentDescriptor);
    }

    /**
     * @param key the id to lookup in the symbol table hierarchy
     * @return true if the descriptor for key is found else false
     */

    public boolean containsEntry(String key) {
        return entries.containsKey(key);
    }

    /**
     * Look up a irAssignableValue recursively up the scope hierarchy
     *
     * @param stringId the id to lookup in the symbol table hierarchy
     * @return Optional.empty if the descriptor is not found else Optional[Descriptor]
     */
    public Optional<Descriptor> getDescriptorFromValidScopes(String stringId) {
        Descriptor currentDescriptor = entries.get(stringId);
        if (currentDescriptor == null) {
            if (parent != null)
                return parent.getDescriptorFromValidScopes(stringId);
        }
        return Optional.ofNullable(currentDescriptor);
    }

    /**
     * Look up a irAssignableValue recursively up the scope hierarchy to see is there is incorrect shadowing parameter
     *
     * @param stringId the id to lookup in the symbol table hierarchy
     * @return true if there is incorrect shadowing of parameter and false otherwise
     */
    public boolean isShadowingParameter(String stringId) {
        Descriptor currentDescriptor = entries.get(stringId);
        if (parent == null) {
            return currentDescriptor != null && symbolTableType == SymbolTableType.Parameter;
        } else {
            if (currentDescriptor != null && symbolTableType == SymbolTableType.Parameter)
                return true;
            else
                return parent.isShadowingParameter(stringId);
        }
    }

    public String toString() {
        return myToString("", "");
    }

    public String myToString(String suffix) {
        return myToString("", suffix);
    }

    public String myToString(String indent, String suffix) {
        String repeat = " ".repeat(Math.max(0, indent.length() - 8));
        String repeat1 = "-".repeat(Math.min(indent.length(), 8));
        if (this.entries.size() == 0) {
            return (repeat + repeat1 + "EmptySymbolTable " + suffix);
        }
        final String IDENTIFIER = "Identifier";
        final String DESCRIPTOR_CLASSES = "Descriptor Types";
        final String BUILTIN_TYPES = "Builtin Types";
        final String ARRAY_LENGTH = "Array Length";

        Optional<Integer> maxLengthIds = Stream.concat(Stream.of(IDENTIFIER, "-".repeat(IDENTIFIER.length())), this.entries.keySet()
                        .stream()
                        .map(Object::toString))
                .map(String::length)
                .reduce(Math::max);

        Stream<String> maxLengthIdsStream = Stream.concat(Stream.of(IDENTIFIER, "-".repeat(IDENTIFIER.length())), this.entries.keySet()
                .stream()
                .map(Object::toString));

        List<String> ids = maxLengthIdsStream.map(((String s) -> padRight(s, maxLengthIds)))
                .toList();

        Optional<Integer> maxMethodD = Stream.concat(Stream.of(DESCRIPTOR_CLASSES, "-".repeat(DESCRIPTOR_CLASSES.length())), this.entries.keySet()
                        .stream()
                        .map(Object::getClass)
                        .map(Class::getSimpleName))
                .map(String::length)
                .reduce(Math::max);
        List<String> descriptorTypes = Stream.concat(Stream.of(DESCRIPTOR_CLASSES, "-".repeat(DESCRIPTOR_CLASSES.length())), this.entries.values()
                        .stream()
                        .map(Object::getClass)
                        .map(Class::getSimpleName))
                .map(s -> padRight(s, maxMethodD))
                .toList();

        List<Descriptor> list1 = new ArrayList<>(this.entries.values());
        List<String> builtins = new ArrayList<>();
        for (Descriptor descriptor1 : list1) {
            Type type = descriptor1.type;
            String toString = type.toString();
            builtins.add(toString);
        }
        Optional<Integer> maxLengthTypes = Stream.concat(Stream.of(BUILTIN_TYPES, "-".repeat(BUILTIN_TYPES.length())), builtins.stream())
                .map(String::length)
                .reduce(Math::max);
        List<String> builtinTypes = Stream.concat(Stream.of(BUILTIN_TYPES, "-".repeat(BUILTIN_TYPES.length())), builtins.stream())
                .map(Object::toString)
                .map((String s) -> padRight(s, maxLengthTypes))
                .toList();

        List<String> list = new ArrayList<>();
        for (Descriptor descriptor : list1) {
            String o = (descriptor instanceof ArrayDescriptor) ? ((ArrayDescriptor) descriptor).size.toString() : "N / A";
            list.add(o);
        }
        Optional<Integer> maxLengthArraySize = Stream.concat(Stream.of(ARRAY_LENGTH, "-".repeat(ARRAY_LENGTH.length())), list.stream())
                .map(String::length)
                .reduce(Math::max);
        List<String> arraySizes = Stream.concat(Stream.of(ARRAY_LENGTH, "-".repeat(ARRAY_LENGTH.length())), list.stream())
                .map(s -> padRight(s, maxLengthArraySize))
                .toList();


        List<String> rows = new ArrayList<>();
        rows.add(repeat + repeat1 + "SymbolTable: " + suffix);
        for (int i = 0; i < ids.size(); i++) {
            rows.add(indent + String.join("", ids.get(i), descriptorTypes.get(i), builtinTypes.get(i), arraySizes.get(i)));
        }
        return String.join("\n", rows);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
