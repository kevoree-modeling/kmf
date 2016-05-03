package org.kevoree.modeling.ast;

public interface KEnum extends KClassifier {

    String[] literals();

    void addLiteral(String value);

}
