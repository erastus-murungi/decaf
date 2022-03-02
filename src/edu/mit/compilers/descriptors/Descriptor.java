package edu.mit.compilers.descriptors;

import edu.mit.compilers.ast.BuiltinType;

/**
 * Every descriptor should at least have a type and an identifier
 */
public abstract class Descriptor {
    public BuiltinType type;
    public String id;

    public Descriptor(BuiltinType type, String id) {
        this.type = type;
        this.id = id;
    }
}
