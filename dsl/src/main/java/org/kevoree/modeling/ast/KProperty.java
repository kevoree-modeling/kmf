package org.kevoree.modeling.ast;

import java.util.Map;

public interface KProperty {

    String name();

    String type();

    String algorithm();

    void setAlgorithm(String alg);

    KDependency[] dependencies();

    void addDependency(KDependency dependency);

    Map<String, String> parameters();

    void addParameter(String param, String value);

    boolean derived();

    void setDerived();

    boolean learned();

    void setLearned();

    boolean global();

    void setGlobal();

}
