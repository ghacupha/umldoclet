/*
 * Copyright 2016-2018 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.talsmasoftware.umldoclet.javadoc;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.Reporter;
import nl.talsmasoftware.umldoclet.UMLDoclet;
import nl.talsmasoftware.umldoclet.logging.Logger;
import nl.talsmasoftware.umldoclet.rendering.indent.Indentation;
import nl.talsmasoftware.umldoclet.uml.Visibility;
import nl.talsmasoftware.umldoclet.uml.configuration.Configuration;
import nl.talsmasoftware.umldoclet.uml.configuration.FieldConfig;
import nl.talsmasoftware.umldoclet.uml.configuration.MethodConfig;
import nl.talsmasoftware.umldoclet.uml.configuration.TypeDisplay;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class DocletConfig implements Configuration {

    private final Doclet doclet;
    private final UMLOptions options;
    private volatile LocalizedReporter reporter;

    /**
     * Destination directory where documentation is generated.
     * <p>
     * Set by (Standard) doclet option {@code -d}, default is {@code ""} meaning the current directory.
     */
    String destDirName = "";

    /**
     * Whether the doclet should run more quite (errors must still be displayed).
     * <p>
     * Set by (Standard) doclet option {@code -quiet}, default is {@code false}.
     */
    boolean quiet = false;

    /**
     * When not quiet, should the doclet be extra verbose?
     * <p>
     * Set by (our own) doclet option {@code -verbose}, default is {@code false}.
     */
    boolean verbose = false;

    FieldCfg fieldConfig = new FieldCfg();
    MethodCfg methodConfig = new MethodCfg();

    List<String> excludedReferences = new ArrayList<>(asList(
            "java.lang.Object", "java.lang.Enum", "java.lang.annotation.Annotation"));

    // TODO decide whether we want to make this configurable at all.
    private Indentation indentation = Indentation.DEFAULT;

    public DocletConfig(UMLDoclet doclet) {
        this.doclet = requireNonNull(doclet, "UML Doclet is <null>.");
        this.options = new UMLOptions(this);
        this.reporter = new LocalizedReporter(this, null, null);
    }

    public void init(Locale locale, Reporter reporter) {
        this.reporter = new LocalizedReporter(this, reporter, locale);
    }

    public Set<Doclet.Option> mergeOptionsWith(Set<Doclet.Option> standardOptions) {
        return options.mergeWith(standardOptions);
    }

    public Logger getLogger() {
        return reporter;
    }

    public Indentation getIndentation() {
        return indentation;
    }

    public String getDestinationDirectory() {
        return destDirName;
    }

    @Override
    public FieldConfig getFieldConfig() {
        return fieldConfig;
    }

    @Override
    public MethodConfig getMethodConfig() {
        return methodConfig;
    }

    @Override
    public List<String> getExcludedTypeReferences() {
        return excludedReferences;
    }

    class FieldCfg implements FieldConfig {

        TypeDisplay typeDisplay = TypeDisplay.SIMPLE;
        Set<Visibility> visibilities = EnumSet.of(Visibility.PROTECTED, Visibility.PUBLIC);

        @Override
        public TypeDisplay typeDisplay() {
            return typeDisplay;
        }

        @Override
        public boolean include(Visibility visibility) {
            return visibilities.contains(visibility);
        }
    }

    class MethodCfg implements MethodConfig {
        // ParamNames paramNames = ParamNames.BEFORE_TYPE;
        ParamNames paramNames = ParamNames.NONE;
        TypeDisplay paramTypes = TypeDisplay.SIMPLE;
        TypeDisplay returnType = TypeDisplay.SIMPLE;
        Set<Visibility> visibilities = EnumSet.of(Visibility.PROTECTED, Visibility.PUBLIC);

        @Override
        public ParamNames paramNames() {
            return paramNames;
        }

        @Override
        public TypeDisplay paramTypes() {
            return paramTypes;
        }

        @Override
        public TypeDisplay returnType() {
            return returnType;
        }

        @Override
        public boolean include(Visibility visibility) {
            return visibilities.contains(visibility);
        }
    }
}
