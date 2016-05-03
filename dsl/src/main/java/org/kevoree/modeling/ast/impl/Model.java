package org.kevoree.modeling.ast.impl;

import org.kevoree.modeling.ast.KClassifier;
import org.kevoree.modeling.ast.KModel;

import java.util.HashMap;
import java.util.Map;

public class Model implements KModel {

    private final Map<String, KClassifier> classifiers;

    public Model() {
        classifiers = new HashMap<String, KClassifier>();
    }

    @Override
    public KClassifier[] classifiers() {
        return classifiers.values().toArray(new KClassifier[classifiers.size()]);
    }

    @Override
    public void addClassifier(KClassifier classifier) {
        classifiers.put(classifier.fqn(), classifier);
    }

    @Override
    public KClassifier get(String fqn) {
        return classifiers.get(fqn);
    }
}
