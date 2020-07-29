/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.kogito.codegen.decision;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.junit.jupiter.api.Test;
import org.kie.kogito.codegen.AddonsConfig;
import org.kie.kogito.codegen.GeneratedFile;
import org.kie.kogito.codegen.GeneratorContext;

import static com.github.javaparser.StaticJavaParser.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DecisionModelResourcesProviderCodegenTest {

    @Test
    public void generateDecisionModelResourcesProvider() throws Exception {

        final GeneratorContext context = GeneratorContext.ofProperties(new Properties());

        final DecisionCodegen codeGenerator = DecisionCodegen
                .ofPath(Paths.get("src/test/resources/decision/models/vacationDays").toAbsolutePath())
                .withAddons(new AddonsConfig().withTracing(true));
        codeGenerator.setContext(context);

        final List<GeneratedFile> generatedFiles = codeGenerator.generate();
        assertEquals(2, generatedFiles.size());

        //A Rest endpoint is always generated per model.
        assertEquals(GeneratedFile.Type.REST, generatedFiles.get(0).getType());
        assertEquals("decision/VacationsResource.java", generatedFiles.get(0).relativePath());

        assertEquals(GeneratedFile.Type.CLASS, generatedFiles.get(1).getType());
        assertEquals("org/kie/kogito/app/DecisionModelResourcesProvider.java", generatedFiles.get(1).relativePath());

        final CompilationUnit compilationUnit = parse(new ByteArrayInputStream(generatedFiles.get(1).contents()));

        final ClassOrInterfaceDeclaration classDeclaration = compilationUnit
                .findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new NoSuchElementException("Compilation unit doesn't contain a class or interface declaration!"));

        assertNotNull(classDeclaration);

        final MethodDeclaration methodDeclaration = classDeclaration
                .findFirst(MethodDeclaration.class,
                           d -> d.getName().getIdentifier().equals("getResources"))
                .orElseThrow(() -> new NoSuchElementException("Class declaration doesn't contain a method named \"getResources\"!"));
        assertNotNull(methodDeclaration);
        assertTrue(methodDeclaration.getBody().isPresent());

        final BlockStmt body = methodDeclaration.getBody().get();
        assertTrue(body.getStatements().size() > 2);
        assertTrue(body.getStatements().get(1).isExpressionStmt());

        final ExpressionStmt expression = (ExpressionStmt) body.getStatements().get(1);
        assertTrue(expression.getExpression() instanceof MethodCallExpr);

        final MethodCallExpr call = (MethodCallExpr) expression.getExpression();
        assertEquals(call.getName().getIdentifier(), "add");
        assertTrue(call.getScope().isPresent());
        assertTrue(call.getScope().get().isNameExpr());

        final NameExpr nameExpr = call.getScope().get().asNameExpr();
        assertEquals(nameExpr.getName().getIdentifier(), "resourcePaths");
    }
}
