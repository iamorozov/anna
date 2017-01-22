package com.morozov.core;

import com.morozov.core.patterns.Singleton;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Set;

@SupportedAnnotationTypes({"*"})
public class Processor extends AbstractProcessor{

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(Singleton.class)) {
            Singleton ca = e.getAnnotation(Singleton.class);
            TypeElement clazz = (TypeElement) e.getEnclosingElement();
            try {
                JavaFileObject f = processingEnv.getFiler().
                        createSourceFile(clazz.getQualifiedName() + "Autogenerate");
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                        "Creating " + f.toUri());
                Writer w = f.openWriter();
                try {
                    String pack = clazz.getQualifiedName().toString();
                    PrintWriter pw = new PrintWriter(w);
                    pw.println("package "
                            + pack.substring(0, pack.lastIndexOf('.')) + ";");
                    pw.println("\npublic class "
                            + clazz.getSimpleName() + "Autogenerate {");

                    TypeMirror type = e.asType();

                    pw.println("\n    public " + ca.className() + " result = \"" + ca.value() + "\";");

                    pw.println("    public int type = " + ca.type() + ";");


                    pw.println("\n    protected " + clazz.getSimpleName()
                            + "Autogenerate() {}");
                    pw.println("\n    /** Handle something. */");
                    pw.println("    protected final void handle"
                            + "(" + ca.className() + " value" + ") {");
                    pw.println("\n//" + e);
                    pw.println("//" + ca);
                    pw.println("\n        System.out.println(value);");
                    pw.println("    }");
                    pw.println("}");
                    pw.flush();
                } finally {
                    w.close();
                }
            } catch (IOException x) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        x.toString());
            }
        }
        return true;
    }
}
