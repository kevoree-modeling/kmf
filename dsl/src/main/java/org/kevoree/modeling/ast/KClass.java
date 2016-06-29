package org.kevoree.modeling.ast;

public interface KClass extends KClassifier {

    KProperty[] properties();

    void addProperty(KProperty property);

    KProperty property(String name);

    KClass parent();

    void setParent(KClass parent);

}
