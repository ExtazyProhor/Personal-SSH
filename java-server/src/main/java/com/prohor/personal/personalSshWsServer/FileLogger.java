package com.prohor.personal.personalSshWsServer;

import java.io.*;
import java.util.function.Consumer;

public class FileLogger implements Consumer<Throwable> {
    private final File file;

    public FileLogger(File dir) {
        file = new File(dir, "log.txt");
    }

    @Override
    public void accept(Throwable throwable) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(throwable.toString());
            writer.newLine();
            for (StackTraceElement element : throwable.getStackTrace()) {
                writer.write("\tat " + element);
                writer.newLine();
            }
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
