package mekanism.common.integration.crafttweaker.example.component;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nonnull;

public class CrTImportsComponent implements ICrTExampleComponent {

    private final Set<String> paths = new LinkedHashSet<>();

    public String addImport(String path) {
        paths.add(path);
        int last = path.lastIndexOf('.');
        if (last == -1) {
            throw new IllegalArgumentException("Path being imported has no packages and may as well just be directly used.");
        } else if (path.length() <= last + 1) {
            throw new IllegalArgumentException("Path being imported ends has no class declared.");
        }
        return path.substring(last + 1);
    }

    public boolean hasImports() {
        return !paths.isEmpty();
    }

    @Nonnull
    @Override
    public String asString() {
        if (paths.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String path : paths) {
            stringBuilder.append("import ").append(path).append(";\n");
        }
        stringBuilder.append('\n');
        return stringBuilder.toString();
    }
}