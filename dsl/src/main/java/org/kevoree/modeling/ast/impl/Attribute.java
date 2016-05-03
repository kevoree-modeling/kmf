package org.kevoree.modeling.ast.impl;

import org.kevoree.modeling.ast.KAttribute;

public class Attribute extends Property implements KAttribute {
    public Attribute(String name, String type) {
        super(name, type);
    }
}
