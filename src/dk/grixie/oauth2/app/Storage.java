package dk.grixie.oauth2.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Storage {
    private class OutputStreamWriterWrapper extends OutputStreamWriter {
        private final String fileName;

        public OutputStreamWriterWrapper(String fileName) throws FileNotFoundException {
            super(new FileOutputStream(new File(directory, fileName)));
            this.fileName = fileName;
        }

        @Override
        public void close() throws IOException {
            super.close();

            for (File file : directory.listFiles()) {
                if (file.getName().substring(0, file.getName().lastIndexOf('.')).
                        equals(fileName.substring(0, fileName.lastIndexOf('.'))) &&
                        !file.getName().equals(fileName)) {
                    file.delete();
                }
            }
        }
    }

    private final File directory;

    public Storage(File directory) {
        this.directory = directory;
    }

    public Set<String> getFileNames() {

        Set<String> result = new HashSet<String>();

        for (String name : directory.list()) {
            result.add(name.substring(0, name.lastIndexOf('.')));
        }

        return result;
    }

    public OutputStreamWriter getOutputStreamWriter(String fileName) throws IOException {
        return new OutputStreamWriterWrapper(fileName + "." + new Date().getTime());
    }

    public InputStreamReader getInputStreamReader(String fileName) throws IOException {
        long timestamp = Long.MAX_VALUE;

        for (String n : directory.list()) {
            if (n.startsWith(fileName + ".")) {
                long candidate = Long.parseLong(n.substring(fileName.length() + ".".length()));

                if (candidate < timestamp) {
                    timestamp = candidate;
                }
            }
        }

        return new InputStreamReader(new FileInputStream(new File(directory, fileName + "." + timestamp)));
    }

    public void delete(String fileName) throws IOException {
        for (File file : directory.listFiles()) {
            if (file.getName().substring(0, file.getName().lastIndexOf('.')).equals(fileName)) {
                file.delete();
            }
        }
    }
}
