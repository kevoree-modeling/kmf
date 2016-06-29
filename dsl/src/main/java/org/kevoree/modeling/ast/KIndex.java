package org.kevoree.modeling.ast;

public interface KIndex extends KClassifier {

    KProperty[] properties();

    void addProperty(String value);

    KClass type();

}