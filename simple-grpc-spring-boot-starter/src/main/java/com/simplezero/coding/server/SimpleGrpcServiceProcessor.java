package com.simplezero.coding.server;

import com.google.auto.service.AutoService;
import com.simplezero.coding.annotations.SimpleGrpcService;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

@SupportedAnnotationTypes("com.simplezero.coding.annotations.SimpleGrpcService")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class SimpleGrpcServiceProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(SimpleGrpcService.class);
        Set<TypeElement> classes = ElementFilter.typesIn(elements);
        Set<String> processedClasses = new HashSet<>();
        for (TypeElement field : classes) {
            processElement(field, processedClasses);
        }
        return true;
    }

    private void processElement(TypeElement classElement, Set<String> processedClass) {
        String serviceFullClassName = classElement.asType().toString();
        if (processedClass.contains(serviceFullClassName)) {
            return;
        }
        try {
            Optional<? extends AnnotationMirror> annotationMirrorResult = classElement
                    .getAnnotationMirrors()
                    .stream()
                    .filter(mirror -> mirror.getAnnotationType().toString()
                            .equals("com.simplezero.coding.annotations.SimpleGrpcService"))
                    .findFirst();
            if (!annotationMirrorResult.isPresent()) {
                return;
            }

            String grpcImplBaseClassName = null;
            AnnotationMirror annotationMirror = annotationMirrorResult.get();
            Map<? extends ExecutableElement, ? extends AnnotationValue> elementValuesMap =
                    annotationMirror.getElementValues();
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValuesMap
                    .entrySet()) {
                String key = entry.getKey().getSimpleName().toString();
                if ("value".equals(key)) {
                    grpcImplBaseClassName = entry.getValue().getValue().toString();
                }
            }
            if (grpcImplBaseClassName == null) {
                return;
            }
            String servicePojoClassSimpleName = classElement.getSimpleName().toString();
            String servicePojoClassName = classElement.getQualifiedName().toString();
            String servicePackageName = ClassUtils.getPackageName(serviceFullClassName);
            String serviceClassName = servicePojoClassSimpleName + "GrpcImpl";
            String serviceVarName = StringUtils.uncapitalize(servicePojoClassSimpleName);
            JavaFileObject builderFile = processingEnv.getFiler()
                    .createSourceFile(servicePojoClassName + "GrpcImpl");
            try (OutputStreamWriter writer = new OutputStreamWriter(builderFile.openOutputStream(), UTF_8)) {
                writer.write("package " + servicePackageName + ";\n" +
                        "\n" +
                        "import io.grpc.Status;\n" +
                        "import io.grpc.StatusRuntimeException;\n" +
                        "import io.grpc.stub.StreamObserver;\n" +
                        "import org.slf4j.Logger;\n" +
                        "import org.slf4j.LoggerFactory;\n" +
                        "import org.springframework.beans.factory.annotation.Autowired;\n" +
                        "import com.simplezero.coding.annotations.GrpcService;\n" +
                        "\n" +
                        "@GrpcService\n" +
                        "public class " + serviceClassName + " extends " + grpcImplBaseClassName
                        + " {\n" +
                        "    private static final Logger log = LoggerFactory.getLogger("
                        + serviceClassName + ".class);\n" +
                        "\n" +
                        "    @Autowired\n" +
                        "    private " + servicePojoClassName + " " + serviceVarName + ";\n" +
                        "\n" + generateMethods(classElement.getEnclosedElements(), serviceVarName) +
                        "}");

                processedClass.add(serviceFullClassName);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Process grpc service [" + serviceFullClassName + "] error:\n" + ExceptionUtils
                            .getStackTrace(e));
        }
    }

    private String generateMethods(List<? extends Element> enclosedElements,
                                   String serviceVarName) {
        StringBuilder methodsDefine = new StringBuilder();
        for (ExecutableElement element : ElementFilter.methodsIn(enclosedElements)) {
            if (element.getModifiers().contains(Modifier.PUBLIC)) {
                String methodName = element.getSimpleName().toString();
                String requestClassName = element.getParameters().get(0).asType().toString();
                String responseClassName = element.getReturnType().toString();
                methodsDefine.append("    @Override\n" +
                        "    public void " + methodName + "(" + requestClassName
                        + " request, StreamObserver<" + responseClassName
                        + "> responseObserver) {\n" +
                        "        Exception cause = null;\n" +
                        "        try {\n" +
                        "            responseObserver.onNext(" + serviceVarName + "."
                        + methodName + "(request));\n" +
                        "            responseObserver.onCompleted();\n" +
                        "        } catch (StatusRuntimeException sre) {\n" +
                        "            cause = sre;\n" +
                        "        } catch (Exception e) {\n" +
                        "            cause = e;\n" +
                        "        }\n\n" +
                        "        if (cause != null) {\n" +
                        "            if (log.isErrorEnabled()) {\n" +
                        "                log.error(\"Invoke [" + methodName
                        + "] error\", cause);\n" +
                        "            }\n\n" +
                        "            if (cause instanceof StatusRuntimeException) {\n" +
                        "                responseObserver.onError(cause);\n" +
                        "            } else {\n" +
                        "                responseObserver.onError(Status.INTERNAL.withCause(cause).asRuntimeException" +
                        "());\n"
                        +
                        "            }\n" +
                        "        }\n" +
                        "    }");
                methodsDefine.append("\n\n");
            }
        }
        return methodsDefine.toString();
    }
}
