package org.kevoree.modeling.ast.impl;

import org.kevoree.modeling.ast.KClass;
import org.kevoree.modeling.ast.KProperty;

import java.util.HashMap;
import java.util.Map;

public class Class implements KClass {

    private final String pack;

    private final String name;

    private final Map<String, KProperty> properties;

    private KClass parent;

    public Class(String fqn) {
        if (fqn.contains(".")) {
            name = fqn.substring(fqn.lastIndexOf('.') + 1);
            pack = fqn.substring(0, fqn.lastIndexOf('.'));
        } else {
            name = fqn;
            pack = null;
        }
        properties = new HashMap<String, KProperty>();
    }

    @Override
    public KProperty[] properties() {
        return properties.values().toArray(new KProperty[properties.size()]);
    }

    @Override
    public void addProperty(KProperty property) {
        properties.put(property.name(), property);
    }

    @Override
    public KClass parent() {
        return parent;
    }

    @Override
    public void setParent(KClass parent) {
        this.parent = parent;
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
