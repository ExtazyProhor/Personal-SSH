package com.prohor.personal.personalSshWsServer;

import org.json.JSONObject;

import java.io.*;
import java.nio.file.*;

public final class Variables {
    private Variables() {
    }

    private static final Path path;

    static {
        String strPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = strPath.endsWith(".jar")
                ? Path.of(new File(new File(strPath).getParentFile(), "variables.json").getPath())
                : Path.of("../variables.json");
    }

    private static JSONObject variables;

    public static JSONObject getVariables() throws IOException {
        if (variables == null)
            updateVariables();
        return variables;
    }

    public static JSONObject updateVariables() throws IOException {
        variables = new JSONObject(Files.readString(path));
        return variables;
    }
}
