package org.kevoree.modeling.ast.impl;

import org.kevoree.modeling.ast.KClass;
import org.kevoree.modeling.ast.KIndex;
import org.kevoree.modeling.ast.KProperty;

import java.util.Set;
import java.util.TreeSet;

public class Index implements KIndex {

    private final Set<KProperty> literals;

    private final String pack;

    private final String name;

    private final KClass clazz;

    public Index(String fqn, KClass clazz) {
        this.clazz = clazz;
        if (fqn.contains(".")) {
            name = fqn.substring(fqn.lastIndexOf('.') + 1);
            pack = fqn.substring(0, fqn.lastIndexOf('.'));
        } else {
            name = fqn;
            pack = null;
        }
        literals = new TreeSet<KProperty>();
    }

    @Override
    public KProperty[] properties() {
        return literals.toArray(new KProperty[literals.size()]);
    }

    @Override
    public void addProperty(String value) {
        KProperty prop = clazz.property(value);
        literals.add(prop);
        prop.addIndex(this);
    }

    @Override
    public KClass type() {
        return this.clazz;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String fqn() {
        if (pack != null) {
            return pack + "." + name;
        } else {
            return name;
        }
    }

    @Override
    public String pack() {
        return pack;
    }

}
