import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import javax.tools.ToolProvider;

// Run with a full JDK 11+ source launcher. Compiled test and product classes target Java 8.
class RunTests {
    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath();
        Path source = args.length > 1 ? Paths.get(args[1]).toAbsolutePath()
                : root.resolve("libuvccamera/src/main/java/com/serenegiant/opengl/renderer/RendererSurface.java");
        Path output = Files.createTempDirectory("uvc-renderer-test-");
        try {
            List<String> options = new ArrayList<>();
            options.add("-source"); options.add("8");
            options.add("-target"); options.add("8");
            options.add("-d"); options.add(output.toString());
            options.add(source.toString());
            try (var paths = Files.walk(root.resolve("tests/renderer/src"))) {
                paths.filter(p -> p.toString().endsWith(".java")).sorted()
                        .forEach(p -> options.add(p.toString()));
            }
            var compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) throw new IllegalStateException("A full JDK is required");
            if (compiler.run(null, null, null, options.toArray(new String[0])) != 0)
                throw new AssertionError("Compilation failed");
            try (URLClassLoader loader = new URLClassLoader(new URL[] {output.toUri().toURL()})) {
                loader.loadClass("com.serenegiant.opengl.renderer.RendererSurfaceTest")
                        .getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            }
        } finally {
            try (var paths = Files.walk(output)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator)
                    Files.delete(path);
            }
        }
    }
}
