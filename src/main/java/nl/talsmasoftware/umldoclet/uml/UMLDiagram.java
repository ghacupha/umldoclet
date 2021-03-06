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
package nl.talsmasoftware.umldoclet.uml;

import nl.talsmasoftware.umldoclet.logging.Logger;
import nl.talsmasoftware.umldoclet.rendering.indent.Indentation;
import nl.talsmasoftware.umldoclet.rendering.indent.IndentingPrintWriter;
import nl.talsmasoftware.umldoclet.rendering.plantuml.PlantumlImageWriter;
import nl.talsmasoftware.umldoclet.uml.configuration.Configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static nl.talsmasoftware.umldoclet.logging.Message.ERROR_COULDNT_RENDER_UML;
import static nl.talsmasoftware.umldoclet.logging.Message.INFO_GENERATING_FILE;

/**
 * Renders a new UML diagram.
 * <p>
 * Responsible for rendering the UML diagram itself:
 * The <code>{@literal @}startuml</code> and <code>{@literal @}enduml</code> lines with the children within.
 * Subclasses of {@code UMLDiagram} are responsible for adding appropriate child renderers.
 * <p>
 * The diagram is rendered to a {@code .puml} output file.
 * Writing happens to the {@link PlantumlImageWriter} which caches the written plantuml file and
 * will generate one or more corresponding images from the diagram when the writer is closed.
 *
 * @author Sjoerd Talsma
 */
public abstract class UMLDiagram extends UMLPart {

    final Configuration config;
    protected final List<UMLPart> children = new ArrayList<>();

    protected UMLDiagram(Configuration config) {
        super(null);
        this.config = requireNonNull(config, "Configuration is <null>.");
    }

    @Override
    protected UMLDiagram getDiagram() {
        return this;
    }

    /**
     * This method determines the physical file where the plantuml diagram should be rendered.
     *
     * @return The physical file where this plantuml diagram in question should be rendered.
     */
    protected abstract File pumlFile();

    @Override
    public Collection<? extends UMLPart> getChildren() {
        return children;
    }

    @Override
    public Configuration getConfiguration() {
        return config;
    }

    @Override
    public <IPW extends IndentingPrintWriter> IPW writeTo(IPW output) {
        output.append("@startuml").newline().newline();
        writeChildrenTo(output);
        output.newline().append("@enduml").newline();
        return output;
    }

    /**
     * Renders this diagram to a designated {@link #pumlFile() .puml file}.
     *
     * @return Whether the rendering succeeded.
     */
    public boolean render() {
        final File pumlFile = pumlFile();
        final Logger logger = getConfiguration().getLogger();
        try (IndentingPrintWriter writer = createPlantumlWriter(pumlFile)) {
            logger.info(INFO_GENERATING_FILE, pumlFile);
            this.writeTo(IndentingPrintWriter.wrap(writer, getConfiguration().getIndentation()));
            return true;
        } catch (IOException | RuntimeException e) {
            logger.error(ERROR_COULDNT_RENDER_UML, pumlFile, e);
            return false;
        }
    }

    /**
     * Ensure the parent directory exists by attempting to create it if it doensn't yet exist.
     *
     * @param file The file verify directory existence for.
     * @return The specified file.
     */
    protected static File ensureParentDir(File file) {
        if (file != null && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IllegalStateException("Can't create directory \"" + file.getParent() + "\".");
        }
        return file;
    }

    private static String baseName(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : name;
    }

    private IndentingPrintWriter createPlantumlWriter(File pumlFile) throws IOException {
        Configuration config = getConfiguration();
        Logger logger = config.getLogger();
        // TODO Make these configurable:
        File imgdir = ensureParentDir(pumlFile).getParentFile();
        String baseName = baseName(pumlFile);
        String[] imgFormats = new String[]{"svg", "png"};
        Indentation indentation = config.getIndentation();

        return IndentingPrintWriter.wrap(
                new PlantumlImageWriter(
                        new OutputStreamWriter(new FileOutputStream(pumlFile)),
                        logger, imgdir, baseName, imgFormats),
                indentation);
    }

}
