/*
 * Copyright 2023 PARAMETA Corp.
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
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;
import score.impl.TypeConverter;

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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class GenerateTScoreProcessor extends AbstractProcessor {

    private Set<ClassName> processed = new HashSet<>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> s = new HashSet<>();
        s.add(GenerateTScores.class.getCanonicalName());
        s.add(GenerateTScore.class.getCanonicalName());
        return s;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    static AnnotationMirror getAnnotationMirror(Element element, Class<? extends Annotation> annClass) {
        for (AnnotationMirror am : element.getAnnotationMirrors()) {
            if (TypeName.get(am.getAnnotationType()).toString().equals(annClass.getName())) {
                return am;
            }
        }
        return null;
    }

    static Object getAnnotationValue(AnnotationMirror am, String annMethod) {
        Objects.requireNonNull(am);
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
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
                GenerateTScores annContainer = element.getAnnotation(GenerateTScores.class);
                if (annContainer != null) {
                    GenerateTScore[] anns = annContainer.value();
                    List<AnnotationMirror> ams = (List<AnnotationMirror>) getAnnotationValue(
                            getAnnotationMirror(element, GenerateTScores.class), "value");
                    if (ams == null || anns.length != ams.size()) {
                        throw new RuntimeException("invalid list of AnnotationMirror element:" + element);
                    }
                    for (int i = 0; i < ams.size(); i++) {
                        AnnotationMirror am = ams.get(i);
                        DeclaredType clazz = (DeclaredType) getAnnotationValue(am, "value");
                        if (clazz == null) {
                            throw new RuntimeException("value is required, element:" + element);
                        }
                        generateImplementClass(anns[i], (TypeElement) clazz.asElement());
                    }
                } else {
                    GenerateTScore ann = element.getAnnotation(GenerateTScore.class);
                    DeclaredType clazz = (DeclaredType) getAnnotationValue(
                            getAnnotationMirror(element, GenerateTScore.class), "value");
                    TypeElement typeElement = clazz == null ?
                            (TypeElement) element : (TypeElement) clazz.asElement();
                    generateImplementClass(ann, typeElement);
                }
                ret = true;
            }
        }
        return ret;
    }

    private void generateImplementClass(GenerateTScore ann, TypeElement typeElement) {
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
                .addAnnotation(TScore.class)
                .superclass(typeElement.asType());
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
                ExecutableElement ee = (ExecutableElement) enclosedElement;
                builder.addMethod(
                        MethodSpec.constructorBuilder()
                                .addModifiers(ee.getModifiers())
                                .addParameters(GenerateTScoreProcessor.getParameterSpecs(typeElement, ee))
                                .addStatement("super($L)", paramJoin(paramNames(ee)))
                                .build());
            }
        }
        builder.addMethods(overrideMethods(typeElement));
        builder.addType(clientTypeSpec(className, builder.methodSpecs));
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
                ExecutableElement ee = (ExecutableElement) enclosedElement;
                addMethod(methods, eventMethodSpec(ee, typeElement), typeElement);
                addMethod(methods, externalMethodSpec(ee, typeElement), typeElement);
            }
        }
        return methods;
    }

    private MethodSpec eventMethodSpec(ExecutableElement ee, TypeElement typeElement) {
        EventLog ann = ee.getAnnotation(EventLog.class);
        if (ann != null) {
            List<String> params = paramNames(ee);
            if (ann.indexed() < 0 || ann.indexed() > params.size()) {
                throw new IndexOutOfBoundsException(String.format("indexed in %s.%s",
                        typeElement.getSimpleName(), ee.getSimpleName()));
            }
            List<String> indexed = new ArrayList<>();
            indexed.add(eventSignature(ee));
            MethodSpec.Builder builder = MethodSpec.overriding(ee)
                    .addAnnotation(AnnotationSpec.builder(EventLog.class).addMember("indexed", "$L", ann.indexed()).build());
            if (ann.indexed() > 0) {
                indexed.addAll(params.subList(0, ann.indexed()));
                params = params.subList(ann.indexed(), params.size());
            }
            builder.addStatement("$T.logEvent($T.asEventObjects($L), $T.asEventObjects($L))",
                    Context.class, TypeConverter.class, objectArray(indexed),
                    TypeConverter.class, objectArray(params));
            return builder.build();
        }
        return null;
    }

    private static List<ParameterSpec> getParameterSpecs(TypeElement typeElement, ExecutableElement ee) {
        boolean mustOptional = false;
        List<ParameterSpec> params = new ArrayList<>();
        for (VariableElement ve : ee.getParameters()) {
            ParameterSpec param;
            if (ve.getAnnotation(Optional.class) != null) {
                param = ParameterSpec.builder(TypeName.get(ve.asType()), ve.getSimpleName().toString())
                        .addModifiers(ve.getModifiers())
                        .addAnnotation(TOptional.class)
                        .build();
                mustOptional = true;
            } else {
                if (mustOptional) {
                    throw new RuntimeException(String.format("parameter should be optional, %s of %s.%s",
                            ve.getSimpleName(), typeElement.getSimpleName(), ee.getSimpleName()));
                }
                param = ParameterSpec.get(ve);
            }
            params.add(param);
        }
        return params;
    }

    private MethodSpec externalMethodSpec(ExecutableElement ee, TypeElement typeElement) {
        External ann = ee.getAnnotation(External.class);
        if (ann != null) {
            boolean readonly = ann.readonly();
            boolean payable = ee.getAnnotation(Payable.class) != null;
            if (readonly && payable) {
                throw new RuntimeException(String.format("readonly method cannot be payable, %s.%s",
                        typeElement.getSimpleName(), ee.getSimpleName()));
            }
            var params = GenerateTScoreProcessor.getParameterSpecs(typeElement, ee);
            TypeName returnType = TypeName.get(ee.getReturnType());
            MethodSpec.Builder builder = MethodSpec.methodBuilder(ee.getSimpleName().toString())
                    .addModifiers(ee.getModifiers())
                    .addParameters(params)
                    .returns(returnType)
                    .addAnnotation(Override.class)
                    .addAnnotation(AnnotationSpec.builder(TExternal.class)
                            .addMember("readonly", "$L", readonly)
                            .addMember("payable", "$L", payable)
                            .build());
            if (returnType.equals(TypeName.VOID)) {
                builder.addStatement("super.$L($L)",
                        ee.getSimpleName(), paramJoin(paramNames(ee)));
            } else {
                builder.addStatement("return super.$L($L)",
                        ee.getSimpleName(), paramJoin(paramNames(ee)));
            }
            return builder.build();
        }
        return null;
    }

    private void addMethods(List<MethodSpec> methods, List<MethodSpec> methodSpecs, TypeElement element) {
        for (MethodSpec methodSpec : methodSpecs) {
            addMethod(methods, methodSpec, element);
        }
    }

    private void addMethod(List<MethodSpec> methods, MethodSpec methodSpec, TypeElement typeElement) {
        if (methodSpec != null) {
            MethodSpec conflictMethod = ProcessorUtil.getConflictMethod(methods, methodSpec);
            if (conflictMethod != null) {
                methods.remove(conflictMethod);
                CodeBlock indexed = eventIndexed(methodSpec);
                if (indexed != null && !indexed.equals(eventIndexed(conflictMethod))) {
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
                .findAny()
                .map((ann) -> ann.members.get("indexed").get(0)).orElse(null);
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
                throw new RuntimeException(String.format("not allowed event parameter type, %s of %s.%s",
                        ve.getSimpleName(), ee.getEnclosingElement().getSimpleName(), ee.getSimpleName()));
            }
            stringJoiner.add(eventTypeString);
        }
        return String.format("\"%s(%s)\"",
                ee.getSimpleName(), stringJoiner);
    }

    private List<String> paramNames(ExecutableElement ee) {
        return ee.getParameters().stream()
                .map(v -> v.getSimpleName().toString())
                .collect(Collectors.toList());
    }

    private String paramJoin(List<String> names) {
        StringJoiner stringJoiner = new StringJoiner(", ");
        for (String name : names) {
            stringJoiner.add(name);
        }
        return stringJoiner.toString();
    }

    private String objectArray(List<String> names) {
        return "new Object[]{" + paramJoin(names) + "}";
    }

    static TypeName externalAnnName = TypeName.get(TExternal.class);
    static TypeName optionalAnnName = TypeName.get(TOptional.class);
    static TypeName eventAnnName = TypeName.get(EventLog.class);

    private static boolean hasOptional(ParameterSpec p) {
        return p.annotations.stream().anyMatch(
                a -> a.type.equals(optionalAnnName)
        );
    }

    private List<List<ParameterSpec>> explodeOptionalParameters(List<ParameterSpec> params) {
        List<List<ParameterSpec>> specs = new ArrayList<>();
        List<ParameterSpec> spec = new ArrayList<>();
        for (var p : params) {
            if (hasOptional(p)) {
                specs.add(List.copyOf(spec));
            }
            spec.add(ParameterSpec.builder(p.type, p.name, p.modifiers.toArray(Modifier[]::new)).build());
        }
        specs.add(List.copyOf(spec));
        return specs;
    }

    private TypeSpec clientTypeSpec(ClassName tScoreClassName, List<MethodSpec> methods) {
        ClassName className = tScoreClassName.nestedClass("Client");
        String FILED_SCORE = "score";
        String FILED_FROM = "from";
        TypeSpec.Builder builder = TypeSpec
                .classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addField(Score.class, FILED_SCORE, Modifier.PRIVATE)
                .addField(Account.class, FILED_FROM, Modifier.PRIVATE)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(Score.class, FILED_SCORE)
                        .addStatement("this($L, null)", FILED_SCORE)
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(Score.class, FILED_SCORE)
                        .addParameter(Account.class, FILED_FROM)
                        .addStatement("this.$L = $L", FILED_SCORE, FILED_SCORE)
                        .addStatement("this.$L = $L == null ? $L.getOwner() : $L",
                                FILED_FROM, FILED_FROM, FILED_SCORE, FILED_FROM)
                        .build());
        List<ParameterSpec> constructorParams = methods.stream()
                .filter(MethodSpec::isConstructor)
                .findFirst()
                .map(m -> m.parameters)
                .orElse(new ArrayList<>());
        MethodSpec.Builder deployMethodBuilder = MethodSpec.methodBuilder("deploy")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addException(Exception.class)
                .addParameter(ServiceManager.class, "sm")
                .addParameter(Account.class, "caller")
                .addParameters(constructorParams)
                .returns(className);
        if (constructorParams.size() == 0) {
            deployMethodBuilder.addStatement("return new $T(sm.deploy(caller, $T.class))", className, tScoreClassName);
        } else {
            deployMethodBuilder.addStatement("return new $T(sm.deploy(caller, $T.class, $L))", className, tScoreClassName,
                    paramJoin(constructorParams.stream().map(p -> p.name).collect(Collectors.toList())));
        }
        builder.addMethod(deployMethodBuilder.build());
        builder.addMethod(MethodSpec.methodBuilder(FILED_SCORE)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return $L", FILED_SCORE)
                .returns(Score.class)
                .build());
        builder.addMethod(MethodSpec.methodBuilder(FILED_FROM)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Account.class, FILED_FROM)
                .addStatement("return new $T($L, $L)", className, FILED_SCORE, FILED_FROM)
                .returns(className)
                .build());

        for (MethodSpec method : methods) {
            AnnotationSpec ann = method.annotations.stream().filter(a -> a.type.equals(externalAnnName)).findAny().orElse(null);
            if (ann != null) {
                boolean readonly = ann.members.get("readonly").get(0).toString().equals("true");
                boolean payable = ann.members.get("payable").get(0).toString().equals("true");

                List<List<ParameterSpec>> parameterSets =
                        explodeOptionalParameters(method.parameters);
                for (var parameters : parameterSets) {
                    MethodSpec.Builder externalMethodBuilder = MethodSpec.methodBuilder(method.name)
                            .addModifiers(method.modifiers);
                    List<String> paramNames = parameters.stream().map(p -> p.name).collect(Collectors.toList());
                    String params = paramJoin(paramNames);
                    if (paramNames.size() > 0) {
                        params = ", " + params;
                    }
                    if (readonly) {
                        externalMethodBuilder.addStatement("return this.$L.call($T.class, \"$L\"$L)",
                                FILED_SCORE, method.returnType, method.name, params).returns(method.returnType);
                    } else {
                        if (payable) {
                            String PARAM_VALUE = resolveName(paramNames, "valueForPayable");
                            externalMethodBuilder.addParameter(ParameterSpec.builder(BigInteger.class, PARAM_VALUE).build());
                            externalMethodBuilder.addStatement("this.$L.invoke(this.$L, $L, \"$L\"$L)",
                                    FILED_SCORE, FILED_FROM, PARAM_VALUE, method.name, params);
                        } else {
                            externalMethodBuilder.addStatement("this.$L.invoke(this.$L, \"$L\"$L)",
                                    FILED_SCORE, FILED_FROM, method.name, params);
                        }
                    }
                    builder.addMethod(externalMethodBuilder
                            .addParameters(parameters)
                            .build());
                }
            } else {
                boolean isEvent = method.annotations.stream().anyMatch(a -> a.type.equals(eventAnnName));
                if (isEvent) {
                    String code = method.code.toString().replace("score.Context.logEvent(",
                            String.format("return new Event(this.%s.getAddress(), ", FILED_SCORE));
                    code = code.replace(TypeConverter.class.getName(), TypeConverter.class.getSimpleName());
                    builder.addMethod(MethodSpec.methodBuilder(method.name)
                            .addModifiers(method.modifiers)
                            .addParameters(method.parameters)
                            .returns(Event.class)
                            .addCode(code)
                            .build());
                }
            }
        }
        return builder.build();
    }

    private String resolveName(List<String> names, String name) {
        if (names.contains(name)) {
            return resolveName(names, "_" + name);
        }
        return name;
    }
}
