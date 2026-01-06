package com.houarizegai.prayertimes.util;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtils {

  public static final Path RESOURCES_PATH = Paths.get("").toAbsolutePath().resolve("src").resolve("main").resolve("resources");

  public static List<String> getFilesNameFromFolder(String path) {
    File[] listOfFiles = new File(path).listFiles();

    if (listOfFiles == null || listOfFiles.length < 1) {
      return Collections.emptyList();
    } else {
      return Arrays.stream(listOfFiles)
        .filter(File::isFile)
        .map(File::getName)
        .collect(Collectors.toList());
    }
  }
}
