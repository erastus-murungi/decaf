package edu.mit.compilers.descriptors;

import edu.mit.compilers.ast.BuiltinType;

/**
 * An Array Descriptor is just a tuple of (Array size, String id, Array Element Type)
 */
public class ArrayDescriptor extends Descriptor {
    public final Long size;

    public ArrayDescriptor(String id, Long size, BuiltinType type) {
        super(type, id);
        this.size = size;
    }
}
