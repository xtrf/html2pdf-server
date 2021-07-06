package eu.xtrf.html2pdf.server.converter.service;

import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class FontServiceImpl implements FontService {
    private static final Map<String, String> fontFamilies = ImmutableMap.<String, String>builder()
            .put("Lato", "Lato")
            .put("Noto_Sans_JP", "Noto Sans JP")
            .put("Noto_Sans_SC", "Noto Sans SC")
            .put("Open_Sans", "Open Sans")
            .put("Poppins", "Poppins")
            .put("Roboto", "Roboto")
            .put("Source_Sans_Pro", "Source Sans Pro")
            .put("Source_Serif_Pro", "Source Serif Pro")
            .build();

    @Override
    public void loadFontsToRenderer(ITextRenderer renderer) throws IOException {
        // this path to fonts directory works only inside docker, for local execution change to: ./src/main/resources/fonts
        for (File ttfFile : getTTFFiles("./src/main/resources/fonts")) { // TODO: move path to configuration file
            String fontFamilyToOverride = findFamilyFont(ttfFile.getAbsolutePath());
            if (fontFamilyToOverride != null) {
                renderer.getFontResolver().addFont(ttfFile.getAbsolutePath(), fontFamilyToOverride, "Identity-H", true, null);
            } else {
                renderer.getFontResolver().addFont(ttfFile.getAbsolutePath(), "Identity-H", true);
            }
        }
    }

    @Override
    public void loadFontsToRenderer(String dir, ITextRenderer renderer) throws IOException {
        for (File ttfFile : getTTFFiles(dir)) {
            renderer.getFontResolver().addFont(ttfFile.getAbsolutePath(), "Identity-H", true);
        }
    }

    private static String findFamilyFont(String ttfPath) {
        for (Map.Entry<String, String> entry : fontFamilies.entrySet()) {
            if (ttfPath.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static List<File> getTTFFiles(String dir) {
        File fontsDir = new File(dir);
        List<File> ttfFiles = new LinkedList<>();

        for (File file : fontsDir.listFiles()) {
            if (file.getName().endsWith(".ttf")) {
                ttfFiles.add(file);
            } else if (file.isDirectory()) {
                ttfFiles.addAll(getTTFFiles(file.getAbsolutePath()));
            }
        }
        return ttfFiles;
    }
}
