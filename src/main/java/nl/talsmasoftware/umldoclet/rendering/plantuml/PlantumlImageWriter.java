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
package nl.talsmasoftware.umldoclet.rendering.plantuml;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import nl.talsmasoftware.umldoclet.logging.Logger;
import nl.talsmasoftware.umldoclet.rendering.writers.StringBufferingWriter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static nl.talsmasoftware.umldoclet.logging.Message.*;

/**
 * Writer that delegates to a regular writer for the UML itself, but when finished (i.e. when close is called), also
 * attempt to generate image files using the PlantUML library.
 *
 * @author Sjoerd Talsma
 */
public class PlantumlImageWriter extends StringBufferingWriter {

    private final Logger logger;
    private final File directory;
    private final String baseName;
    private final Collection<FileFormat> imageFormats;

    /**
     * Constructor. Creates a new writer that delegates all writes to the specified writer,
     * while maintaining a {@link #getBuffer() buffer} of all written characters.
     * This buffer is used when the writing is done (i.e., this writer is being closed) to create image files;
     * one for each specified <code>imageFormats</code>.
     * The name of the file(s) to create is based on the specified <code>directory</code>, <code>baseName</code> and
     * {@code default file extension} of the particular {@code FileFormat}.
     *
     * @param delegate     The delegate writer to perform the pass-through writing.
     * @param logger       The logger.
     * @param directory    The directory to create the image file(s) in.
     * @param baseName     The base name of the image file(s) to create, without extension.
     * @param imageFormats The name(s) of the image format(s) to generate.
     */
    public PlantumlImageWriter(Writer delegate, Logger logger, File directory, String baseName, String... imageFormats) {
        super(delegate);
        this.logger = requireNonNull(logger, "Logger is <null>.");
        this.directory = directory;
        this.baseName = baseName;
        this.imageFormats = parseFileFormats(imageFormats);
    }

    /**
     * Closes the delegate writer and tries to generate an image file for each configured image format.
     * The default file extension from the image format is used, together with the specified <code>directory</code>
     * and <code>baseName</code>.
     *
     * @throws IOException In case of I/O errors while closing the delegate writer or writing to an image file.
     */
    @Override
    public void close() throws IOException {
        super.close();
        for (FileFormat imageFormat : imageFormats) {
            final File imageFile = new File(directory, baseName + imageFormat.getFileSuffix());
            logger.info(INFO_GENERATING_FILE, imageFile);
            try (OutputStream imageOutput = new BufferedOutputStream(new FileOutputStream(imageFile))) {
                new SourceStringReader(getBuffer().toString()).outputImage(imageOutput, new FileFormatOption(imageFormat));
            }
        }
    }

    /**
     * Converts image format names to a set of {@link FileFormat} instances.
     * Names are used to avoid any runtime dependency from calling code on the implementation to any
     * <code>plantuml</code> packages.
     *
     * @param imageFormatNames The names of the image formats to be generated
     *                         (e.g. <code>"PNG"</code>, <code>"SVG"</code>, etc).
     * @return The parsed <code>FileFormat</code> instances.
     */
    private Set<FileFormat> parseFileFormats(String... imageFormatNames) {
        Set<FileFormat> fileFormats = EnumSet.noneOf(FileFormat.class);
        if (imageFormatNames != null) for (String fileFormatName : imageFormatNames) {
            FileFormat fileFormat = fileFormatFromName(fileFormatName);
            if (fileFormat != null) fileFormats.add(fileFormat);
        }
        logger.debug(DEBUG_CONFIGURED_IMAGE_FORMATS, fileFormats);
        return fileFormats;
    }

    /**
     * Converts the name of the fileformat into a {@link FileFormat} object.
     * Returns <code>null</code> if it cannot find the corresponding file format.
     *
     * @param fileFormatName The name of the fileformat.
     * @return The found <code>FileFormat</code> instance or <code>null</code> if the name was not recognized.
     */
    private FileFormat fileFormatFromName(String fileFormatName) {
        fileFormatName = trimToEmpty(fileFormatName);
        if (fileFormatName.startsWith(".")) fileFormatName = fileFormatName.substring(1);
        for (FileFormat fileFormat : FileFormat.values()) {
            if (fileFormatName.equalsIgnoreCase(fileFormat.name())) return fileFormat;
        }
        logger.warn(WARNING_UNRECOGNIZED_IMAGE_FORMAT, fileFormatName);
        return null;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * @return String representation providing information about which image file(s) will be generated.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(getClass().getSimpleName()).append('{');
        if (directory != null) {
            result.append(directory.getPath()).append(File.separator);
        }
        result.append(baseName);
        if (imageFormats.size() == 1) {
            result.append(imageFormats.iterator().next().getFileSuffix());
        } else {
            result.append('.').append(imageFormats);
        }
        return result.append('}').toString();
    }

}
