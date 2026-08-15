package org.fsv.instagramuploader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class FontRegistry {
 private static final Logger logger = LoggerFactory.getLogger(FontRegistry.class);
 private static final Path PROJECT_FONTS = Paths.get("src/main/resources/fonts");
 private static final Path DEPLOYMENT_FONTS = Paths.get("fonts");
 private static final Set<Path> registeredFiles = new LinkedHashSet<>();
 private static volatile List<String> availableFamilies;
 private static volatile Map<String, String> familiesByLowercase;

 private FontRegistry() {
 }

 public static List<String> getAvailableFamilies() {
	List<String> result = availableFamilies;
	if (result == null) {
	 result = refreshAvailableFamilies();
	}
	return result;
 }

 public static synchronized List<String> refreshAvailableFamilies() {
	registerCustomFonts();
	List<String> families = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT))
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.toList();
	Map<String, String> lookup = new HashMap<>();
	families.forEach(family -> lookup.put(family.toLowerCase(Locale.ROOT), family));
	familiesByLowercase = Map.copyOf(lookup);
	availableFamilies = families;
	return families;
 }

 public static Font createFont(String requestedFamily, int style, int size, Font fallback) {
	getAvailableFamilies();
	String family = familiesByLowercase.get(requestedFamily.toLowerCase(Locale.ROOT));
	if (family == null) {
	 logger.warn("Font '{}' is unavailable, using '{}'", requestedFamily, fallback.getFamily());
	 family = fallback.getFamily();
	}
	return new Font(family, style, size);
 }

 public static byte[] createPreview(String family, String text, int style) throws IOException {
	Font font = createFont(family, style, 36, new Font(Font.SANS_SERIF, style, 36));
	BufferedImage image = new BufferedImage(720, 90, BufferedImage.TYPE_INT_RGB);
	Graphics2D graphics = image.createGraphics();
	graphics.setColor(Color.WHITE);
	graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
	graphics.setColor(Color.BLACK);
	graphics.setFont(font);
	graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	FontMetrics metrics = graphics.getFontMetrics();
	String value = text == null || text.isBlank() ? "Aa Bb Cc 123 - Beispieltext" : text;
	graphics.drawString(value, 16, (image.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
	graphics.dispose();
	try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
	 ImageIO.write(image, "png", output);
	 return output.toByteArray();
	}
 }

 private static void registerCustomFonts() {
	for (Path directory : fontDirectories()) {
	 if (!Files.isDirectory(directory)) {
		continue;
	 }
	 try (Stream<Path> paths = Files.walk(directory)) {
		paths.filter(Files::isRegularFile)
						.filter(FontRegistry::isFontFile)
						.sorted(Comparator.comparing(Path::toString))
						.forEach(FontRegistry::registerFont);
	 } catch (IOException e) {
		logger.warn("Could not scan font directory '{}'", directory, e);
	 }
	}
 }

 private static List<Path> fontDirectories() {
	List<Path> result = new ArrayList<>();
	result.add(PROJECT_FONTS);
	result.add(DEPLOYMENT_FONTS);
	String externalDirectory = System.getenv("INSTAGRAM_FONTS_DIR");
	if (externalDirectory != null && !externalDirectory.isBlank()) {
	 result.add(Paths.get(externalDirectory));
	}
	return result;
 }

 private static boolean isFontFile(Path path) {
	Path fileName = path.getFileName();
	if (fileName == null) {
	 return false;
	}
	String name = fileName.toString().toLowerCase(Locale.ROOT);
	return name.endsWith(".ttf") || name.endsWith(".otf");
 }

 private static void registerFont(Path path) {
	Path absolutePath = path.toAbsolutePath().normalize();
	if (!registeredFiles.add(absolutePath)) {
	 return;
	}
	try {
	 Font font = Font.createFont(Font.TRUETYPE_FONT, absolutePath.toFile());
	 if (GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)) {
		logger.debug("Registered font '{}' from '{}'", font.getFamily(), absolutePath);
	 }
	} catch (FontFormatException | IOException e) {
	 logger.warn("Could not register font file '{}'", absolutePath, e);
	}
 }
}
