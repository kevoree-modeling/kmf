package org.kmf.generator;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Visibility;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaEnumSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.kevoree.modeling.ast.*;
import org.kevoree.modeling.ast.impl.Model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Generator {

    public static String extension = ".mm";

    private KModel model = new Model();

    private List<JavaSource> sources;

    public void scan(File target) throws Exception {
        String[] everythingInThisDir = target.list();
        for (String name : everythingInThisDir) {
            if (name.trim().endsWith(extension)) {
                ModelBuilder.parse(new File(target, name), model);
            }
        }
    }

    public void generate(File target) {
        sources = new ArrayList<JavaSource>();
        for (KClassifier classifier : model.classifiers()) {
            if (classifier instanceof KEnum) {
                KEnum loopEnum = (KEnum) classifier;
                final JavaEnumSource javaEnum = Roaster.create(JavaEnumSource.class);
                if (classifier.pack() != null) {
                    javaEnum.setPackage(classifier.pack());
                }
                javaEnum.setName(classifier.name());
                for (String literal : loopEnum.literals()) {
                    javaEnum.addEnumConstant(literal);
                }
                sources.add(javaEnum);
            } else {
                final JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
                KClass loopClass = (KClass) classifier;
                if (classifier.pack() != null) {
                    javaClass.setPackage(classifier.pack());
                }
                javaClass.setName(classifier.name());

                String parentName = "org.mwg.AbstractNode";
                if (loopClass.parent() != null) {
                    parentName = loopClass.parent().fqn();
                }
                javaClass.setSuperType(parentName);

                for (KProperty prop : loopClass.properties()) {

                    //add helper name
                    javaClass.addField()
                            .setVisibility(Visibility.PUBLIC)
                            .setFinal(true)
                            .setName(prop.name())
                            .setType(String.class)
                            .setStringInitializer(prop.name())
                            .setStatic(true);
                    //pojo generation
                    if (!prop.derived() && !prop.learned()) {
                        javaClass.addMethod()
                                .setName(toCamelCase("get " + prop.name()))
                                .setBody("return node.get(" + prop.name() + ");");

                        javaClass.addMethod()
                                .setName(toCamelCase("set " + prop.name()))
                                .setReturnType(classifier.fqn())
                                .setBody("return node.set(" + prop.name() + ",value);")
                                .addParameter("hello.Titi", "value");
                    }
                }

                sources.add(javaClass);

            }
        }

        //DEBUG print
        for (JavaSource src : sources) {


            System.out.println(src);
        }

    }

    private String toType(final String init) {
        return "";
    }

    private String toCamelCase(final String init) {
        if (init == null) {
            return null;
        }
        final StringBuilder ret = new StringBuilder(init.length());
        boolean isFirst = true;
        for (final String word : init.split(" ")) {
            if (isFirst) {
                ret.append(word);
                isFirst = false;
            } else {
                if (!word.isEmpty()) {
                    ret.append(word.substring(0, 1).toUpperCase());
                    ret.append(word.substring(1).toLowerCase());
                }
            }
        }
        return ret.toString();
    }

}
