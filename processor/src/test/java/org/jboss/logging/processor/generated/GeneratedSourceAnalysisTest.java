/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.logging.processor.generated;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Method;
import org.jboss.forge.roaster.model.Named;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.ParameterSource;
import org.jboss.logging.DelegatingBasicLogger;
import org.jboss.logging.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class GeneratedSourceAnalysisTest {

    private static String TEST_SRC_PATH = null;
    private static String TEST_GENERATED_SRC_PATH = null;

    private static final Map<Locale, String> LOCALE_CONSTANTS = new LinkedHashMap<>();

    static {
        LOCALE_CONSTANTS.put(Locale.CANADA, "Locale.CANADA");
        LOCALE_CONSTANTS.put(Locale.CANADA_FRENCH, "Locale.CANADA_FRENCH");
        LOCALE_CONSTANTS.put(Locale.CHINESE, "Locale.CHINESE");
        LOCALE_CONSTANTS.put(Locale.ENGLISH, "Locale.ENGLISH");
        LOCALE_CONSTANTS.put(Locale.FRANCE, "Locale.FRANCE");
        LOCALE_CONSTANTS.put(Locale.FRENCH, "Locale.FRENCH");
        LOCALE_CONSTANTS.put(Locale.GERMAN, "Locale.GERMAN");
        LOCALE_CONSTANTS.put(Locale.GERMANY, "Locale.GERMANY");
        LOCALE_CONSTANTS.put(Locale.ITALIAN, "Locale.ITALIAN");
        LOCALE_CONSTANTS.put(Locale.ITALY, "Locale.ITALY");
        LOCALE_CONSTANTS.put(Locale.JAPAN, "Locale.JAPAN");
        LOCALE_CONSTANTS.put(Locale.JAPANESE, "Locale.JAPANESE");
        LOCALE_CONSTANTS.put(Locale.KOREA, "Locale.KOREA");
        LOCALE_CONSTANTS.put(Locale.KOREAN, "Locale.KOREAN");
        LOCALE_CONSTANTS.put(Locale.SIMPLIFIED_CHINESE, "Locale.SIMPLIFIED_CHINESE");
        LOCALE_CONSTANTS.put(Locale.TRADITIONAL_CHINESE, "Locale.TRADITIONAL_CHINESE");
        LOCALE_CONSTANTS.put(Locale.UK, "Locale.UK");
        LOCALE_CONSTANTS.put(Locale.US, "Locale.US");
    }

    @BeforeClass
    public static void setUp() {
        TEST_SRC_PATH = System.getProperty("test.src.path");
        TEST_GENERATED_SRC_PATH = System.getProperty("test.generated.src.path");
    }

    @Test
    public void testBundles() throws Exception {
        compareBundle(DefaultMessages.class);
        compareBundle(ValidMessages.class);
    }

    @Test
    public void testLoggers() throws Exception {
        compareLogger(DefaultLogger.class);
        compareLogger(ExtendedLogger.class);
        compareLogger(ValidLogger.class);
        compareLogger(RootLocaleLogger.class);
    }

    @Test
    public void testGeneratedTranslations() throws Exception {
        compareTranslations(DefaultLogger.class);
        compareTranslations(DefaultMessages.class);
        compareTranslations(RootLocaleLogger.class);
    }

    @Test
    public void testRootLocale() throws Exception {
        JavaClassSource implementationSource = parseGenerated(RootLocaleLogger.class);
        FieldSource<JavaClassSource> locale = implementationSource.getField("LOCALE");
        Assert.assertNotNull(locale, "Expected a LOCALE field for " + implementationSource.getName());
        Assert.assertEquals(locale.getLiteralInitializer(), "Locale.forLanguageTag(\"en-UK\")");

        implementationSource = parseGenerated(DefaultLogger.class);
        locale = implementationSource.getField("LOCALE");
        Assert.assertNotNull(locale, "Expected a LOCALE field for " + implementationSource.getName());
        Assert.assertEquals(locale.getLiteralInitializer(), "Locale.ROOT");
    }

    private void compareLogger(final Class<?> intf) throws IOException {
        final JavaInterfaceSource interfaceSource = parseInterface(intf);
        final JavaClassSource implementationSource = parseGenerated(intf);
        compareCommon(interfaceSource, implementationSource);

        // Logger implementations should have a single constructor which accepts a org.jboss.logging.Logger
        final List<MethodSource<JavaClassSource>> implementationMethods = implementationSource.getMethods();
        final Optional<MethodSource<JavaClassSource>> constructor = findConstructor(implementationMethods);
        Assert.assertTrue(constructor.isPresent(), "No constructor found for " + implementationSource.getName());
        final List<ParameterSource<JavaClassSource>> parameters = constructor.get().getParameters();
        Assert.assertEquals(parameters.size(), 1, "Found more than one parameter for " + implementationSource.getName() + ": " + parameters);
        final ParameterSource<JavaClassSource> parameter = parameters.get(0);
        final Type<JavaClassSource> type = parameter.getType();
        Assert.assertEquals(type.getQualifiedName(), Logger.class.getName());

        // If the logger is not extending the DelegatingBasicLogger there should be a protected final org.jboss.logging.Logger field
        if (!DelegatingBasicLogger.class.getName().equals(implementationSource.getSuperType())) {
            final FieldSource<JavaClassSource> log = implementationSource.getField("log");
            Assert.assertNotNull(log, "Expected a log field in " + implementationSource.getName());
            Assert.assertTrue(log.isProtected() && log.isFinal(),
                    "Expected the log field to be protected and final in " + implementationSource.getName());
        }
    }

    private void compareBundle(final Class<?> intf) throws IOException {
        final JavaInterfaceSource interfaceSource = parseInterface(intf);
        final JavaClassSource implementationSource = parseGenerated(intf);
        compareCommon(interfaceSource, implementationSource);

        // Message bundles should have an INSTANCE field
        final FieldSource<JavaClassSource> instance = implementationSource.getField("INSTANCE");
        Assert.assertNotNull(instance, "Expected an INSTANCE field in " + implementationSource.getName());
        Assert.assertTrue(instance.isStatic() && instance.isFinal() && instance.isPublic(),
                "Expected the instance field to be public, static and final in " + implementationSource.getName());

        // Expect a protected constructor with no parameters
        final Optional<MethodSource<JavaClassSource>> constructor = findConstructor(implementationSource.getMethods());
        Assert.assertTrue(constructor.isPresent(), "No constructor found for " + implementationSource.getName());
        final MethodSource<JavaClassSource> c = constructor.get();
        Assert.assertTrue(c.getParameters().isEmpty(), "Expected the constructor parameters to be empty for " + implementationSource.getName());
        Assert.assertTrue(c.isProtected(), "Expected the constructor to be protected for " + implementationSource.getName());
    }

    private void compareCommon(final JavaInterfaceSource interfaceSource, final JavaClassSource implementationSource) {
        final List<MethodSource<JavaInterfaceSource>> interfaceMethods = interfaceSource.getMethods();
        final List<MethodSource<JavaClassSource>> implementationMethods = implementationSource.getMethods();

        // Validate the implementation has all the interface methods, note this should be the cause
        final Collection<String> interfaceMethodNames = toNames(interfaceMethods);
        final Collection<String> implementationMethodNames = toNames(implementationMethods);
        Assert.assertTrue(implementationMethodNames.containsAll(interfaceMethodNames),
                String.format("Implementation is missing methods from the interface:%n\timplementation: %s%n\tinterface:%s", implementationMethodNames, interfaceMethodNames));

        // The generates source files should have a serialVersionUID with a value of one
        Assert.assertTrue(implementationSource.hasField("serialVersionUID"), "Expected a serialVersionUID field in " + implementationSource.getName());
        final FieldSource<JavaClassSource> serialVersionUID = implementationSource.getField("serialVersionUID");
        Assert.assertEquals(serialVersionUID.getLiteralInitializer(), "1L", "Expected serialVersionUID  to be set to 1L in " + implementationSource.getName());

        // All bundles should have a getLoggingLocale()
        final MethodSource<JavaClassSource> getLoggingLocale = implementationSource.getMethod("getLoggingLocale");
        Assert.assertNotNull(getLoggingLocale, "Expected a getLoggingLocale() method in " + implementationSource.getName());
        Assert.assertTrue(getLoggingLocale.isProtected(), "Expected the getLoggingLocale() to be protected in " + implementationSource.getName());
    }

    private void compareTranslations(final Class<?> intf) throws IOException {
        final JavaInterfaceSource interfaceSource = parseInterface(intf);
        final Collection<JavaClassSource> implementations = parseGeneratedTranslations(intf);
        // Find the default source file
        final JavaClassSource superImplementationSource = parseGenerated(intf);
        for (JavaClassSource implementationSource : implementations) {
            compareTranslations(interfaceSource, superImplementationSource, implementationSource);
        }
    }

    private void compareTranslations(final JavaInterfaceSource interfaceSource, final JavaClassSource superImplementationSource,
                                     final JavaClassSource implementationSource) {
        // The implementations should not contain any methods from the interface
        final List<String> interfaceMethods = interfaceSource.getMethods()
                .stream()
                .map(Named::getName)
                .collect(Collectors.toList());
        final Collection<String> found = new ArrayList<>();
        for (MethodSource<JavaClassSource> method : implementationSource.getMethods()) {
            if (interfaceMethods.contains(method.getName())) {
                found.add(method.getName());
            }
        }
        Assert.assertTrue(found.isEmpty(), "Found methods in implementation that were in the interface " + implementationSource.getName() + " : " + found);

        // The getLoggerLocale() should be overridden
        final MethodSource<JavaClassSource> getLoggerLocale = implementationSource.getMethod("getLoggingLocale");
        Assert.assertNotNull(getLoggerLocale, "Missing overridden getLoggingLocale() method " + implementationSource.getName());

        // If the file should have a locale constant, validate the constant is one of the Locale constants
        LOCALE_CONSTANTS.forEach((locale, constant) -> {
            if (implementationSource.getName().endsWith(locale.toString())) {
                // Get the LOCALE field
                final FieldSource<JavaClassSource> localeField = implementationSource.getField("LOCALE");
                Assert.assertNotNull(localeField, "Expected a LOCALE field " + implementationSource.getName());
                Assert.assertEquals(localeField.getLiteralInitializer(), constant,
                        "Expected the LOCALE to be set to " + constant + " in " + implementationSource.getName());
            }
        });

        // Get all the method names from the super class
        final List<String> superMethods = superImplementationSource.getMethods()
                .stream()
                .filter(method -> !method.isConstructor())
                .map(Named::getName)
                .collect(Collectors.toList());

        // All methods in the translation implementation should be overrides of methods in the super class
        implementationSource.getMethods().forEach(method -> {
            if (!method.isConstructor()) {
                Assert.assertTrue(method.hasAnnotation(Override.class), String.format("Expected method %s to be overridden in %s.",
                        method.getName(), implementationSource.getName()));
                Assert.assertTrue(superMethods.contains(method.getName()), String.format("Expected method %s to override the super (%s) method in %s.",
                        method.getName(), superImplementationSource.getName(), implementationSource.getName()));
            }
        });
    }

    private void compareRootLocale(final Class<?> intf, final String expectedLocaleString) throws IOException {
        final JavaClassSource implementationSource = parseGenerated(intf);
    }

    private Optional<MethodSource<JavaClassSource>> findConstructor(final List<MethodSource<JavaClassSource>> implementationMethods) {
        return implementationMethods.stream()
                .filter(Method::isConstructor)
                .findFirst();
    }

    private Collection<String> toNames(final List<? extends MethodSource<?>> methods) {
        return methods.stream()
                // Skip default methods, static methods and constructors
                .filter(m -> !m.isDefault() && !m.isStatic() && !m.isConstructor())
                .map(Named::getName)
                .collect(Collectors.toList());

    }

    private String packageToPath(final Package pkg) {
        String result = pkg.getName().replace('.', File.separatorChar);
        return result.endsWith(File.separator) ? result : result + File.separator;
    }

    private JavaClassSource parseGenerated(final Class<?> intf) throws IOException {
        final Pattern pattern = Pattern.compile(Pattern.quote(intf.getSimpleName()) + "_\\$(logger|bundle)\\.java$");
        // Find all the files that match
        final FileFilter filter = pathname -> pattern.matcher(pathname.getName()).find();
        final File dir = new File(TEST_GENERATED_SRC_PATH, packageToPath(intf.getPackage()));
        final File[] files = dir.listFiles(filter);
        // There should only be one file
        Assert.assertNotNull(files, "Did not find any implementation files for interface " + intf.getName());
        Assert.assertEquals(1, files.length, "Found more than one implementation for interface " + intf.getName() + " " + Arrays.asList(files));

        return Roaster.parse(JavaClassSource.class, files[0]);
    }

    private Collection<JavaClassSource> parseGeneratedTranslations(final Class<?> intf) throws IOException {
        final Pattern pattern = Pattern.compile(Pattern.quote(intf.getSimpleName()) + "_\\$(logger|bundle)_.*\\.java$");
        // Find all the files that match
        final FileFilter filter = pathname -> pattern.matcher(pathname.getName()).matches();
        final File dir = new File(TEST_GENERATED_SRC_PATH, packageToPath(intf.getPackage()));
        final File[] files = dir.listFiles(filter);
        // There should only be one file
        Assert.assertNotNull(files, "Did not find any implementation files for interface " + intf.getName());
        Assert.assertTrue(files.length > 0, "Did not find any translation implementations for interface " + intf.getName());
        final Collection<JavaClassSource> result = new ArrayList<>();
        for (final File file : files) {
            result.add(Roaster.parse(JavaClassSource.class, file));
        }
        return result;
    }

    private JavaInterfaceSource parseInterface(final Class<?> intf) throws IOException {
        final File srcFile = new File(TEST_SRC_PATH, packageToPath(intf.getPackage()) +
                intf.getSimpleName() + ".java");
        return Roaster.parse(JavaInterfaceSource.class, srcFile);
    }
}
