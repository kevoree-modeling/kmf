package org.kevoree.modeling.ast;

public interface KModel {

    KClassifier[] classifiers();

    void addClassifier(KClassifier classifier);

    KClassifier get(String fqn);

}
