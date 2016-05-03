package org.kevoree.modeling.ast.impl;

import org.kevoree.modeling.ast.KDependency;

public class Dependency implements KDependency {

    private final String query;

    public Dependency(String query) {
        this.query = query;
    }

    @Override
    public String query() {
        return query;
    }
}
