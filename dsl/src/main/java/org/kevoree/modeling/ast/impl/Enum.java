package org.kevoree.modeling.ast.impl;

import org.kevoree.modeling.ast.KEnum;

import java.util.*;

public class Enum implements KEnum {

    private final Set<String> literals;

    private final String pack;

    private final String name;

    public Enum(String fqn) {
        if (fqn.contains(".")) {
            name = fqn.substring(fqn.lastIndexOf('.')+1);
            pack = fqn.substring(0, fqn.lastIndexOf('.'));
        } else {
            name = fqn;
            pack = null;
        }
        literals = new TreeSet<String>();
    }

    @Override
    public String[] literals() {
        return literals.toArray(new String[literals.size()]);
    }

    @Override
    public void addLiteral(String value) {
        literals.add(value);
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
