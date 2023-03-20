/*
 * Copyright 2023 PARAMETA Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iconloop.score.test;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import foundation.icon.annotation_processor.AbstractProcessor;
import foundation.icon.annotation_processor.ProcessorUtil;
import score.Context;
import score.annotation.EventLog;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class EventImplementProcessor extends AbstractProcessor {

    private Set<ClassName> processed = new HashSet<>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> s = new HashSet<>();
        s.add(EventImplements.class.getCanonicalName());
        s.add(EventImplement.class.getCanonicalName());
        return s;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    static AnnotationMirror getAnnotationMirror(Element element, Class<? extends Annotation> annClass) {
        for(AnnotationMirror am : element.getAnnotationMirrors()) {
            if (TypeName.get(am.getAnnotationType()).toString().equals(annClass.getName())) {
                return am;
            }
        }
        return null;
    }

    static Object getAnnotationValue(AnnotationMirror am, String annMethod) {
        Objects.requireNonNull(am);
        for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(annMethod)) {
                return entry.getValue().getValue();
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        boolean ret = false;
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotationElements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : annotationElements) {
                EventImplements annContainer = element.getAnnotation(EventImplements.class);
                if (annContainer != null) {
                    EventImplement[] anns = annContainer.value();
                    List<AnnotationMirror> ams = (List<AnnotationMirror>)getAnnotationValue(
                            getAnnotationMirror(element, EventImplements.class), "value");
                    if (ams == null || anns.length != ams.size()) {
                        throw new RuntimeException("invalid list of AnnotationMirror element:"+element);
                    }
                    for (int i = 0; i < ams.size(); i++) {
                        AnnotationMirror am = ams.get(i);
                        DeclaredType clazz = (DeclaredType)getAnnotationValue(am, "value");
                        if (clazz == null) {
                            throw new RuntimeException("value is required, element:" + element);
                        }
                        generateImplementClass(anns[i], (TypeElement)clazz.asElement());
                    }
                } else {
                    EventImplement ann = element.getAnnotation(EventImplement.class);
                    DeclaredType clazz = (DeclaredType)getAnnotationValue(
                            getAnnotationMirror(element, EventImplement.class), "value");
                    TypeElement typeElement = clazz == null ?
                            (TypeElement)element : (TypeElement)clazz.asElement();
                    generateImplementClass(ann, typeElement);
                }
                ret = true;
            }
        }
        return ret;
    }
    private void generateImplementClass(EventImplement ann, TypeElement typeElement) {
        if (!typeElement.getKind().isClass()) {
            throw new RuntimeException("not support, element:" + typeElement);
        }
        ClassName className = ClassName.get(ClassName.get(typeElement).packageName(),
                generateClassSimpleName(typeElement) + ann.suffix());
        if (processed.contains(className)) {
            return;
        }
        processed.add(className);
        messager.noteMessage("process %s to %s", typeElement.asType(), className);
        TypeSpec typeSpec = typeSpec(className, typeElement);
        JavaFile javaFile = JavaFile.builder(className.packageName(), typeSpec).build();
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            messager.warningMessage("create javaFile error : %s", e.getMessage());
        }
    }

    private String generateClassSimpleName(TypeElement typeElement) {
        if (typeElement.getNestingKind().isNested()) {
            return generateClassSimpleName((TypeElement) typeElement.getEnclosingElement())
                    + typeElement.getSimpleName().toString();
        } else {
            return typeElement.getSimpleName().toString();
        }
    }

    private TypeSpec typeSpec(ClassName className, TypeElement typeElement) {
        TypeSpec.Builder builder = TypeSpec
                .classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .superclass(typeElement.asType());
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
                ExecutableElement ee = (ExecutableElement) enclosedElement;
                List<ParameterSpec> params = ProcessorUtil.getParameterSpecs(ee);
                StringJoiner stringJoiner = new StringJoiner(",");
                params.forEach((v) -> {
                    stringJoiner.add(v.name);
                });
                builder.addMethod(
                        MethodSpec.constructorBuilder()
                                .addModifiers(ee.getModifiers())
                                .addParameters(params)
                                .addStatement("super($L)", stringJoiner.toString())
                                .build());
            }
        }
        builder.addMethods(overrideMethods(typeElement));
        return builder.build();
    }

    private List<MethodSpec> overrideMethods(TypeElement typeElement) {
        List<MethodSpec> methods = new ArrayList<>();
        TypeMirror superClass = typeElement.getSuperclass();
        if (!superClass.getKind().equals(TypeKind.NONE) && !superClass.toString().equals(Object.class.getName())) {
            messager.noteMessage("superClass[kind:%s, name:%s]", superClass.getKind().name(), superClass.toString());
            List<MethodSpec> superMethods = overrideMethods(super.getTypeElement(superClass));
            addMethods(methods, superMethods, typeElement);
        }
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind().equals(ElementKind.METHOD)) {
                EventLog ann = enclosedElement.getAnnotation(EventLog.class);
                if (ann != null) {
                    ExecutableElement ee = (ExecutableElement) enclosedElement;
                    List<String> params = ee.getParameters().stream()
                            .map(v -> v.getSimpleName().toString())
                            .collect(Collectors.toList());
                    if (ann.indexed() < 0 || ann.indexed() > params.size()) {
                        throw new IndexOutOfBoundsException(String.format("indexed in %s.%s",
                                typeElement.getSimpleName(), enclosedElement.getSimpleName()));
                    }
                    List<String> indexed = new ArrayList<>();
                    indexed.add(eventSignature(ee));
                    MethodSpec.Builder builder = MethodSpec.overriding((ExecutableElement) enclosedElement)
                            .addAnnotation(AnnotationSpec.builder(EventLog.class).addMember("indexed", "$L", ann.indexed()).build());
                    if (ann.indexed() > 0) {
                        indexed.addAll(params.subList(0, ann.indexed()));
                        params = params.subList(ann.indexed(), params.size());
                    }
                    builder.addStatement("$T.logEvent($L, $L)",
                            Context.class, objectArray(indexed), objectArray(params));
                    addMethod(methods, builder.build(), typeElement);
                }
            }
        }
        return methods;
    }

    private void addMethods(List<MethodSpec> methods, List<MethodSpec> methodSpecs, TypeElement element) {
        for (MethodSpec methodSpec : methodSpecs) {
            addMethod(methods, methodSpec, element);
        }
    }

    private void addMethod(List<MethodSpec> methods, MethodSpec methodSpec, TypeElement typeElement) {
        if (methodSpec != null) {
            CodeBlock indexed = eventIndexed(methodSpec);
            MethodSpec conflictMethod = ProcessorUtil.getConflictMethod(methods, methodSpec);
            if (conflictMethod != null) {
                methods.remove(conflictMethod);
                if (!indexed.equals(eventIndexed(conflictMethod))) {
                    messager.warningMessage(
                            "Redeclare '%s %s(%s)' in %s",
                            conflictMethod.returnType.toString(),
                            conflictMethod.name,
                            ProcessorUtil.parameterSpecToString(conflictMethod.parameters),
                            typeElement.getQualifiedName());
                }
            }
            methods.add(methodSpec);
        }
    }

    private CodeBlock eventIndexed(MethodSpec methodSpec) {
        return methodSpec.annotations.stream()
                .filter((ann) -> ann.type.toString().equals(EventLog.class.getName()))
                .findAny().orElseThrow().members.get("indexed").get(0);
    }

    private static final Map<String, String> eventTypeStrings = Map.of(
            "byte", "int",
            "char", "int",
            "short", "int",
            "int", "int",
            "long", "int",
            "java.math.BigInteger", "int",
            "java.lang.String", "str",
            "byte[]", "bytes",
            "boolean", "bool",
            "score.Address", "Address");

    private String eventSignature(ExecutableElement ee) {
        StringJoiner stringJoiner = new StringJoiner(",");
        for (VariableElement ve : ee.getParameters()) {
            String eventTypeString = eventTypeStrings.get(ve.asType().toString());
            if (eventTypeString == null) {
                throw new RuntimeException("not allowed event parameter type, element:" +
                        ee.getEnclosingElement() + ", method:" + ee + ", argument:" + ve.getSimpleName());
            }
            stringJoiner.add(eventTypeString);
        }
        return String.format("\"%s(%s)\"",
                ee.getSimpleName(), stringJoiner);
    }

    private String objectArray(List<String> names) {
        StringJoiner stringJoiner = new StringJoiner(", ");
        for (String name : names) {
            stringJoiner.add(name);
        }
        return "new Object[]{" + stringJoiner + "}";
    }
}
