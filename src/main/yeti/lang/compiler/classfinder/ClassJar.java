package yeti.lang.compiler.classfinder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

class ClassJar extends ClassPathItem {
    JarFile jar;
    Map entries = Collections.EMPTY_MAP;

    ClassJar(String path) {
        try {
            jar = new JarFile(path);
            Enumeration e = jar.entries();
            entries = new HashMap();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class"))
                    entries.put(name, entry);
            }
        } catch (IOException ex) {
        }
    }

    InputStream getStream(String name) throws IOException {
        ZipEntry entry = (ZipEntry) entries.get(name);
        return entry == null ? null : jar.getInputStream(entry);
    }

    boolean exists(String name) {
        return entries.containsKey(name);
    }
}
