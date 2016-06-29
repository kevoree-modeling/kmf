package org.kmf.generator;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Visibility;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaEnumSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.kevoree.modeling.ast.*;
import org.kevoree.modeling.ast.impl.Model;
import org.mwg.Graph;
import org.mwg.GraphBuilder;
import org.mwg.Type;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    public void deepScan(File target) throws Exception {
        String[] everythingInThisDir = target.list();
        for (String name : everythingInThisDir) {
            if (name.trim().endsWith(extension)) {
                ModelBuilder.parse(new File(target, name), model);
            } else {
                File current = new File(target, name);
                if (current.isDirectory()) {
                    deepScan(current);
                }
            }
        }
    }

    public void generate(String name, File target) {

        boolean useML = false;

        sources = new ArrayList<JavaSource>();
        //Generate all NodeType
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
            } else if (classifier instanceof KClass) {
                final JavaClassSource javaClass = Roaster.create(JavaClassSource.class).setFinal(true);
                KClass loopClass = (KClass) classifier;
                if (classifier.pack() != null) {
                    javaClass.setPackage(classifier.pack());
                }
                javaClass.setName(classifier.name());

                String parentName = "org.mwg.plugin.AbstractNode";
                if (loopClass.parent() != null) {
                    parentName = loopClass.parent().fqn();
                }
                javaClass.setSuperType(parentName);

                MethodSource<JavaClassSource> constructor = javaClass.addMethod().setConstructor(true);
                constructor.addParameter("long", "p_world");
                constructor.addParameter("long", "p_time");
                constructor.addParameter("long", "p_id");
                constructor.addParameter(Graph.class, "p_graph");
                constructor.addParameter("long[]", "currentResolution");
                constructor.setBody("super(p_world, p_time, p_id, p_graph, currentResolution);");
                constructor.setVisibility(Visibility.PUBLIC);

                //add helper name
                javaClass.addField()
                        .setVisibility(Visibility.PUBLIC)
                        .setFinal(true)
                        .setName("NODE_NAME")
                        .setType(String.class)
                        .setStringInitializer(javaClass.getCanonicalName())
                        .setStatic(true);

                for (KProperty prop : loopClass.properties()) {

                    //add helper name
                    javaClass.addField()
                            .setVisibility(Visibility.PUBLIC)
                            .setFinal(true)
                            .setName(prop.name().toUpperCase())
                            .setType(String.class)
                            .setStringInitializer(prop.name())
                            .setStatic(true);

                    //POJO generation
                    if (!prop.derived() && !prop.learned()) {

                        if (prop instanceof KRelation) {
                            //generate getter
                            MethodSource<JavaClassSource> getter = javaClass.addMethod();
                            getter.setVisibility(Visibility.PUBLIC).setFinal(true);
                            getter.setReturnType(typeToClassName(prop.type()) + "[]");
                            getter.setName(toCamelCase("get " + prop.name()));
                            getter.setBody(
                                    "org.mwg.DeferCounter waiter = this.graph().newCounter(1);\n" +
                                            "this.rel(" + prop.name().toUpperCase() + ", waiter.wrap());\n" +
                                            "org.mwg.Node[] raw = (org.mwg.Node[]) waiter.waitResult();" +
                                            typeToClassName(prop.type()) + "[] casted = new " + typeToClassName(prop.type()) + "[raw.length];" +
                                            "System.arraycopy(raw,0,casted,0,raw.length);\n" +
                                            "return casted;");

                            //generate setter
                            MethodSource<JavaClassSource> add = javaClass.addMethod();
                            add.setVisibility(Visibility.PUBLIC).setFinal(true);
                            add.setName(toCamelCase("addTo " + prop.name()));
                            add.setReturnType(classifier.fqn());
                            add.addParameter(typeToClassName(prop.type()), "value");
                            add.setBody("super.add(" + prop.name().toUpperCase() + ",(org.mwg.Node)value);return this;");

                            //generate setter
                            MethodSource<JavaClassSource> remove = javaClass.addMethod();
                            remove.setVisibility(Visibility.PUBLIC).setFinal(true);
                            remove.setName(toCamelCase("removeFrom " + prop.name()));
                            remove.setReturnType(classifier.fqn());
                            remove.addParameter(typeToClassName(prop.type()), "value");
                            remove.setBody("super.remove(" + prop.name().toUpperCase() + ",(org.mwg.Node)value);return this;");

                        } else {

                            if (prop.algorithm() != null) {
                                useML = true;
                                //attribute will be processed as a sub node
                                //generate getter
                                MethodSource<JavaClassSource> getter = javaClass.addMethod();
                                getter.setVisibility(Visibility.PUBLIC).setFinal(true);
                                getter.setReturnType(typeToClassName(prop.type()));
                                getter.setName(toCamelCase("get " + prop.name()));

                                getter.setBody("\t\tfinal org.mwg.DeferCounter waiter = this.graph().newCounter(1);\n" +
                                        "this.rel(" + prop.name().toUpperCase() + ", new org.mwg.Callback<org.mwg.Node[]>() {\n" +
                                        "@Override\n" +
                                        "public void on(org.mwg.Node[] raw) {\n" +
                                        "if (raw == null || raw.length == 0) {\n" +
                                        "waiter.count();\n" +
                                        "} else {\n" +
                                        "org.mwg.ml.RegressionNode casted = (org.mwg.ml.RegressionNode) raw[0];\n" +
                                        "casted.extrapolate(waiter.wrap());\n" +
                                        "}\n" +
                                        "}\n" +
                                        "});\n" +
                                        "return (" + typeToClassName(prop.type()) + ") waiter.waitResult();");

                                //generate setter
                                MethodSource<JavaClassSource> setter = javaClass.addMethod();
                                setter.setVisibility(Visibility.PUBLIC).setFinal(true);
                                setter.setName(toCamelCase("set " + prop.name()));
                                setter.setReturnType(classifier.fqn());
                                setter.addParameter(typeToClassName(prop.type()), "value");

                                StringBuffer buffer = new StringBuffer();
                                buffer.append(" final org.mwg.DeferCounter waiter = this.graph().newCounter(1);\n" +
                                        "        final " + classifier.fqn() + " selfPointer = this;\n" +
                                        "        this.rel(" + prop.name().toUpperCase() + ", new org.mwg.Callback<org.mwg.Node[]>() {\n" +
                                        "            @Override\n" +
                                        "            public void on(org.mwg.Node[] raw) {\n" +
                                        "                if (raw == null || raw.length == 0) {\n" +
                                        "                    org.mwg.ml.RegressionNode casted = (org.mwg.ml.RegressionNode) graph().newTypedNode(world(),time(),\"" + prop.algorithm() + "\");\n" +
                                        "                    selfPointer.add(" + prop.name().toUpperCase() + ",casted);\n");

                                for (String key : prop.parameters().keySet()) {
                                    buffer.append("casted.set(\"" + key + "\"," + prop.parameters().get(key) + ");\n");
                                }

                                buffer.append("                 casted.learn(value, waiter.wrap());\n" +
                                        "                } else {\n" +
                                        "                    org.mwg.ml.RegressionNode casted = (org.mwg.ml.RegressionNode) raw[0];\n" +
                                        "                    casted.learn(value, waiter.wrap());\n" +
                                        "                }\n" +
                                        "            }\n" +
                                        "        });\n" +
                                        "        waiter.waitResult();\n" +
                                        "        return this;");

                                setter.setBody(buffer.toString());
                            } else {

                                //generate getter
                                MethodSource<JavaClassSource> getter = javaClass.addMethod();
                                getter.setVisibility(Visibility.PUBLIC).setFinal(true);
                                getter.setReturnType(typeToClassName(prop.type()));
                                getter.setName(toCamelCase("get " + prop.name()));
                                getter.setBody("return (" + typeToClassName(prop.type()) + ") super.get(" + prop.name().toUpperCase() + ");");


                                //generate setter
                                MethodSource<JavaClassSource> setter = javaClass.addMethod();
                                setter.setVisibility(Visibility.PUBLIC).setFinal(true);
                                setter.setName(toCamelCase("set " + prop.name()));
                                setter.setReturnType(classifier.fqn());
                                setter.addParameter(typeToClassName(prop.type()), "value");

                                StringBuffer buffer = new StringBuffer();
                                if (prop.indexes().length > 0) {
                                    buffer.append("final " + classifier.fqn() + " self = this;\n");
                                    buffer.append("final org.mwg.DeferCounter waiterUnIndex = this.graph().newCounter(" + prop.indexes().length + ");\n");
                                    buffer.append("final org.mwg.DeferCounter waiterIndex = this.graph().newCounter(" + prop.indexes().length + ");\n");

                                    for (KIndex index : prop.indexes()) {
                                        String queryParam = "";
                                        for (KProperty loopP : index.properties()) {
                                            if (!queryParam.isEmpty()) {
                                                queryParam += ",";
                                            }
                                            queryParam += loopP.name();
                                        }
                                        buffer.append("this.graph().unindex(\"" + index.fqn() + "\",this,\"" + queryParam + "\",waiterUnIndex.wrap());");
                                    }

                                    buffer.append("waiterUnIndex.then(new org.mwg.plugin.Job() {");
                                    buffer.append("@Override\n");
                                    buffer.append("public void run() {\n");
                                    buffer.append("self.setProperty(" + prop.name().toUpperCase() + ", (byte) " + nameToType(prop.type()) + ", value);");
                                    for (KIndex index : prop.indexes()) {
                                        String queryParam = "";
                                        for (KProperty loopP : index.properties()) {
                                            if (!queryParam.isEmpty()) {
                                                queryParam += ",";
                                            }
                                            queryParam += loopP.name();
                                        }
                                        buffer.append("self.graph().index(\"" + index.fqn() + "\",self,\"" + queryParam + "\",waiterIndex.wrap());");
                                    }

                                    buffer.append("}\n});");
                                    buffer.append("waiterIndex.waitResult();\n");

                                } else {
                                    buffer.append("super.setProperty(" + prop.name().toUpperCase() + ", (byte)" + nameToType(prop.type()) + ",value);");
                                }
                                buffer.append("return this;");
                                setter.setBody(buffer.toString());
                            }

                        }

                    }
                }

                sources.add(javaClass);

            }
        }
        //Generate plugin
        final JavaClassSource pluginClass = Roaster.create(JavaClassSource.class);
        if (name.contains(".")) {
            pluginClass.setPackage(name.substring(0, name.lastIndexOf('.')));
            pluginClass.setName(name.substring(name.lastIndexOf('.') + 1) + "Plugin");
        } else {
            pluginClass.setName(name + "Plugin");
        }
        pluginClass.setSuperType("org.mwg.plugin.AbstractPlugin");
        MethodSource<JavaClassSource> pluginConstructor = pluginClass.addMethod().setConstructor(true);
        StringBuilder constructorContent = new StringBuilder();
        constructorContent.append("super();\n");
        for (KClassifier classifier : model.classifiers()) {
            if (classifier instanceof KClass) {
                String fqn = classifier.fqn();
                constructorContent.append("\t\tdeclareNodeType(" + fqn + ".NODE_NAME, new org.mwg.plugin.NodeFactory() {\n" +
                        "\t\t\t@Override\n" +
                        "\t\t\tpublic org.mwg.Node create(long world, long time, long id, org.mwg.Graph graph, long[] initialResolution) {\n" +
                        "\t\t\t\treturn (org.mwg.Node)new " + fqn + "(world,time,id,graph,initialResolution);\n" +
                        "\t\t\t}\n" +
                        "\t\t});");
            }
        }

        pluginConstructor.setBody(constructorContent.toString());
        sources.add(pluginClass);

        //Generate model
        final JavaClassSource modelClass = Roaster.create(JavaClassSource.class);
        if (name.contains(".")) {
            modelClass.setPackage(name.substring(0, name.lastIndexOf('.')));
            modelClass.setName(name.substring(name.lastIndexOf('.') + 1) + "Model");
        } else {
            modelClass.setName(name + "Model");
        }
        modelClass.addField().setName("_graph").setVisibility(Visibility.PRIVATE).setType(Graph.class).setFinal(true);

        MethodSource<JavaClassSource> modelConstructor = modelClass.addMethod().setConstructor(true);
        modelConstructor.addParameter(GraphBuilder.class, "builder");
        if (useML) {
            modelConstructor.setBody("this._graph = builder.withPlugin(new org.mwg.ml.MLPlugin()).withPlugin(new samplePlugin()).build();");
        } else {
            modelConstructor.setBody("this._graph = builder.withPlugin(new samplePlugin()).build();");
        }
        modelClass.addMethod().setName("graph").setBody("return this._graph;").setVisibility(Visibility.PUBLIC).setFinal(true).setReturnType(Graph.class);

        for (KClassifier classifier : model.classifiers()) {
            if (classifier instanceof KClass) {
                MethodSource<JavaClassSource> loopNewMethod = modelClass.addMethod().setName(toCamelCase("new " + classifier.name()));
                loopNewMethod.setVisibility(Visibility.PUBLIC).setFinal(true);
                loopNewMethod.setReturnType(classifier.fqn());
                loopNewMethod.addParameter("long", "world");
                loopNewMethod.addParameter("long", "time");
                loopNewMethod.setBody("return (" + classifier.fqn() + ")this._graph.newTypedNode(world,time," + classifier.fqn() + ".NODE_NAME);");
            }
            if (classifier instanceof KIndex) {
                KIndex casted = (KIndex) classifier;
                MethodSource<JavaClassSource> loopFindMethod = modelClass.addMethod().setName(toCamelCase("find " + classifier.name()));
                loopFindMethod.setVisibility(Visibility.PUBLIC).setFinal(true);
                loopFindMethod.setReturnType(casted.type().fqn());
                loopFindMethod.addParameter("long", "world");
                loopFindMethod.addParameter("long", "time");
                loopFindMethod.addParameter("String", "query");
                loopFindMethod.setBody("" +
                        "        final org.mwg.DeferCounter waiter = _graph.newCounter(1);\n" +
                        "        this._graph.find(world, time, \"" + casted.fqn() + "\", query, new org.mwg.Callback<org.mwg.Node[]>() {\n" +
                        "            @Override\n" +
                        "            public void on(org.mwg.Node[] result) {\n" +
                        "                if (result.length > 0) {\n" +
                        "                    waiter.wrap().on(result[0]);\n" +
                        "                } else {\n" +
                        "                    waiter.count();\n" +
                        "                }\n" +
                        "            }\n" +
                        "        });\n" +
                        "        return (" + casted.type().fqn() + ") waiter.waitResult();");

                MethodSource<JavaClassSource> loopFindAllMethod = modelClass.addMethod().setName(toCamelCase("findAll " + classifier.name()));
                loopFindAllMethod.setVisibility(Visibility.PUBLIC).setFinal(true);
                loopFindAllMethod.setReturnType(casted.type().fqn()+"[]");
                loopFindAllMethod.addParameter("long", "world");
                loopFindAllMethod.addParameter("long", "time");
                loopFindAllMethod.setBody("" +
                        "        final org.mwg.DeferCounter waiter = _graph.newCounter(1);\n" +
                        "        this._graph.findAll(world, time, \"" + casted.fqn() + "\", new org.mwg.Callback<org.mwg.Node[]>() {\n" +
                        "            @Override\n" +
                        "            public void on(org.mwg.Node[] result) {\n" +
                        "                "+casted.type().fqn()+"[] typedResult = new "+casted.type().fqn()+"[result.length];\n" +
                        "                System.arraycopy(result, 0, typedResult, 0, result.length);\n" +
                        "                waiter.wrap().on(typedResult);" +
                        "            }\n" +
                        "        });\n" +
                        "        return (" + casted.type().fqn() + "[]) waiter.waitResult();");

            }
        }


        sources.add(modelClass);

        //DEBUG print
        for (JavaSource src : sources) {

            File targetPkg;
            if (src.getPackage() != null) {
                targetPkg = new File(target.getAbsolutePath() + File.separator + src.getPackage().replace(".", File.separator));
            } else {
                targetPkg = target;
            }
            targetPkg.mkdirs();
            File targetSrc = new File(targetPkg, src.getName() + ".java");
            try {
                FileWriter writer = new FileWriter(targetSrc);
                writer.write(src.toString());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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

    private static byte nameToType(final String name) {
        switch (name) {
            case "Integer":
                return Type.INT;
            case "Long":
                return Type.LONG;
            case "String":
                return Type.STRING;
        }
        return -1;
    }

    private static String typeToClassName(String mwgTypeName) {
        byte mwgType = nameToType(mwgTypeName);
        switch (mwgType) {
            case Type.BOOL:
                return java.lang.Boolean.class.getCanonicalName();
            case Type.DOUBLE:
                return java.lang.Double.class.getCanonicalName();
            case Type.INT:
                return java.lang.Integer.class.getCanonicalName();
            case Type.LONG:
                return java.lang.Long.class.getCanonicalName();
            case Type.STRING:
                return java.lang.String.class.getCanonicalName();
        }
        return mwgTypeName;
    }


}
