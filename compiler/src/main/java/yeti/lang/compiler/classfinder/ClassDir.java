package yeti.lang.compiler.classfinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class ClassDir extends ClassPathItem {
    String path;

    ClassDir(String path) {
        this.path = path.length() > 0 ? path.concat(File.separator) : "";
    }

    InputStream getStream(String name) throws IOException {
        return new FileInputStream(path.concat(name));
    }

    boolean exists(String name) {
        return new File(path.concat(name)).isFile();
    }
}
