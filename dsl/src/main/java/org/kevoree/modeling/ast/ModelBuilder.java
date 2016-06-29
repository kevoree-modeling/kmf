package org.kevoree.modeling.ast;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.kevoree.modeling.ast.impl.*;
import org.kevoree.modeling.ast.impl.Class;
import org.kevoree.modeling.ast.impl.Enum;

import java.io.File;
import java.util.List;

public class ModelBuilder {

    public static void main(String[] args) throws Exception {
        System.out.println(parse(new File("/Users/duke/dev/dsl/src/main/exemples/ml_1.mm")));
    }

    public static KModel parse(String content) {
        KModel model = new Model();
        return build(new ANTLRInputStream(content), model);
    }

    public static KModel parse(File content) throws Exception {
        KModel model = new Model();
        return build(new ANTLRFileStream(content.getAbsolutePath()), model);
    }

    public static KModel parse(File content, KModel model) throws Exception {
        return build(new ANTLRFileStream(content.getAbsolutePath()), model);
    }

    private static KModel build(ANTLRInputStream in, KModel model) {

        BufferedTokenStream tokens = new CommonTokenStream(new org.kevoree.modeling.ast.MetaModelLexer(in));
        org.kevoree.modeling.ast.MetaModelParser parser = new org.kevoree.modeling.ast.MetaModelParser(tokens);
        org.kevoree.modeling.ast.MetaModelParser.MetamodelContext mmctx = parser.metamodel();
        for (org.kevoree.modeling.ast.MetaModelParser.EnumDeclrContext enumDeclrContext : mmctx.enumDeclr()) {
            String fqn = null;
            if (enumDeclrContext.TYPE_NAME() != null) {
                fqn = enumDeclrContext.TYPE_NAME().toString();
            }
            if (enumDeclrContext.IDENT() != null) {
                fqn = enumDeclrContext.IDENT().toString();
            }
            final KEnum enumClass = (KEnum) getOrAddEnum(model, fqn);
            for (TerminalNode literal : enumDeclrContext.enumLiterals().IDENT()) {
                enumClass.addLiteral(literal.getText());
            }
        }
        for (org.kevoree.modeling.ast.MetaModelParser.ClassDeclrContext classDeclrContext : mmctx.classDeclr()) {
            String classFqn = null;
            if (classDeclrContext.TYPE_NAME() != null) {
                classFqn = classDeclrContext.TYPE_NAME().toString();
            }
            if (classDeclrContext.IDENT() != null) {
                classFqn = classDeclrContext.IDENT().toString();
            }
            final KClass newClass = (KClass) getOrAddClass(model, classFqn);
            //process parents
            if (classDeclrContext.parentsDeclr() != null) {
                if (classDeclrContext.parentsDeclr().TYPE_NAME() != null) {
                    final KClass newClassTT = (KClass) getOrAddClass(model, classDeclrContext.parentsDeclr().TYPE_NAME().toString());
                    newClass.setParent(newClassTT);
                }
                if (classDeclrContext.parentsDeclr().IDENT() != null) {
                    final KClass newClassTT = (KClass) getOrAddClass(model, classDeclrContext.parentsDeclr().IDENT().toString());
                    newClass.setParent(newClassTT);
                }
            }
            for (org.kevoree.modeling.ast.MetaModelParser.AttributeDeclarationContext attDecl : classDeclrContext.attributeDeclaration()) {
                String name = attDecl.IDENT().getText();
                org.kevoree.modeling.ast.MetaModelParser.AttributeTypeContext attType = attDecl.attributeType();
                String value;
                if (attType.TYPE_NAME() != null) {
                    value = attType.TYPE_NAME().getText();
                } else {
                    value = attType.getText();
                }
                final KAttribute attribute = new Attribute(name, value);
                processAnnotations(attribute, attDecl.annotation());
                processSemanticBloc(attribute, attDecl.semanticDeclr());
                newClass.addProperty(attribute);
            }
            for (org.kevoree.modeling.ast.MetaModelParser.RelationDeclarationContext relDecl : classDeclrContext.relationDeclaration()) {
                String name = relDecl.IDENT().get(0).getText();
                String type;
                if (relDecl.TYPE_NAME() == null) {
                    type = relDecl.IDENT(1).toString();
                } else {
                    type = relDecl.TYPE_NAME().toString();
                }
                final KRelation relation = new Relation(name, type);
                processAnnotations(relation, relDecl.annotation());
                processSemanticBloc(relation, relDecl.semanticDeclr());
                newClass.addProperty(relation);
            }
        }

        for (org.kevoree.modeling.ast.MetaModelParser.IndexDeclrContext indexDeclrContext : mmctx.indexDeclr()) {
            String name = indexDeclrContext.IDENT().get(0).getText();
            String type;
            if (indexDeclrContext.TYPE_NAME() == null) {
                type = indexDeclrContext.IDENT(1).toString();
            } else {
                type = indexDeclrContext.TYPE_NAME().toString();
            }
            final KIndex indexClass = (KIndex) getOrAddIndex(model, name, (KClass) model.get(type));
            for (TerminalNode literal : indexDeclrContext.indexLiterals().IDENT()) {
                indexClass.addProperty(literal.getText());
            }
        }
        return model;
    }

    private static void processAnnotations(KProperty property, List<org.kevoree.modeling.ast.MetaModelParser.AnnotationContext> annotations) {
        if (annotations != null) {
            for (org.kevoree.modeling.ast.MetaModelParser.AnnotationContext annotationContext : annotations) {
                if (annotationContext.getText().equals("learned")) {
                    property.setLearned();
                }
                if (annotationContext.getText().equals("derived")) {
                    property.setDerived();
                }
                if (annotationContext.getText().equals("global")) {
                    property.setGlobal();
                }
            }
        }
    }

    private static void processSemanticBloc(KProperty property, org.kevoree.modeling.ast.MetaModelParser.SemanticDeclrContext semanticDeclrContext) {
        if (semanticDeclrContext != null) {
            if (semanticDeclrContext.semanticFrom() != null) {
                for (org.kevoree.modeling.ast.MetaModelParser.SemanticFromContext fromContext : semanticDeclrContext.semanticFrom()) {
                    String val = fromContext.STRING().getText();
                    val = val.substring(1, val.length() - 1);
                    KDependency dependency = new Dependency(val);
                    property.addDependency(dependency);
                }
            }
            if (semanticDeclrContext.semanticUsing() != null) {
                for (org.kevoree.modeling.ast.MetaModelParser.SemanticUsingContext usingContext : semanticDeclrContext.semanticUsing()) {
                    String val = usingContext.STRING().getText();
                    val = val.substring(1, val.length() - 1);
                    property.setAlgorithm(val);
                }
            }
            if (semanticDeclrContext.semanticWith() != null) {
                for (org.kevoree.modeling.ast.MetaModelParser.SemanticWithContext withContext : semanticDeclrContext.semanticWith()) {
                    String value;
                    if (withContext.NUMBER() != null) {
                        value = withContext.NUMBER().getText();
                    } else if (withContext.STRING() != null) {
                        String val = withContext.STRING().getText();
                        val = val.substring(1, val.length() - 1);
                        value = val;
                    } else {
                        value = null;
                    }
                    if (value != null) {
                        property.addParameter(withContext.IDENT().getText(), value);
                    }
                }
            }
        }
    }

    private static KClassifier getOrAddClass(KModel model, String fqn) {
        KClassifier previous = model.get(fqn);
        if (previous != null) {
            return previous;
        }
        previous = new Class(fqn);
        model.addClassifier(previous);
        return previous;
    }

    private static KClassifier getOrAddIndex(KModel model, String fqn, KClass clazz) {
        KClassifier previous = model.get(fqn);
        if (previous != null) {
            return previous;
        }
        previous = new Index(fqn, clazz);
        model.addClassifier(previous);
        return previous;
    }

    private static KClassifier getOrAddEnum(KModel model, String fqn) {
        KClassifier previous = model.get(fqn);
        if (previous != null) {
            return previous;
        }
        previous = new Enum(fqn);
        model.addClassifier(previous);
        return previous;
    }


    /*
    private static boolean isPrimitiveTYpe(String nn) {
        if (nn.equals("Bool")) {
            return true;
        }
        if (nn.equals("String")) {
            return true;
        }
        if (nn.equals("Long")) {
            return true;
        }
        if (nn.equals("Int")) {
            return true;
        }
        if (nn.equals("Double")) {
            return true;
        }
        return false;
    }

    private static int toId(String nn) {
        if (nn.equals("Bool")) {
            return -1;
        }
        if (nn.equals("String")) {
            return -2;
        }
        if (nn.equals("Long")) {
            return -3;
        }
        if (nn.equals("Int")) {
            return -4;
        }
        if (nn.equals("Double")) {
            return -5;
        }
        return 0;
    }*/

}
