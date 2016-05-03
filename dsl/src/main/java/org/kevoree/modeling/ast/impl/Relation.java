package org.kevoree.modeling.ast.impl;

import org.kevoree.modeling.ast.KRelation;

public class Relation extends Property implements KRelation {
    public Relation(String name, String type) {
        super(name, type);
    }
}
