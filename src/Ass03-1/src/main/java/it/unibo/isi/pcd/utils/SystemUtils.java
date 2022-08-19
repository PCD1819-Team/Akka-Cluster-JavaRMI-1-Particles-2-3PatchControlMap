package it.unibo.isi.pcd.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class SystemUtils {

  public static int getCores() {
    return Runtime.getRuntime().availableProcessors();
  }

  public static String getHomePath() {
    return System.getProperty("user.home");
  }

  public static boolean isTxtFile(final File file) {
    return SystemUtils.isTxtFile(file.toString());
  }

  public static boolean isTxtFile(final Path file) {
    return SystemUtils.isTxtFile(file.toString());
  }

  public static boolean isTxtFile(final String file) {
    final boolean result = file.toLowerCase().endsWith(".txt")
        || file.toLowerCase().endsWith(".TXT");
    return result;
  }

  public static boolean isValidFile(final Path file) {
    return Files.isReadable(file) && !Files.isSymbolicLink(file) && !file.toFile().isHidden();
  }

}
