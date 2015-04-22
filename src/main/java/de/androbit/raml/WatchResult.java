package de.androbit.raml;

public class WatchResult {
  boolean isModified;
  String fileName;
  boolean reset;

  public WatchResult(boolean isModified, String fileName, boolean reset) {
    this.isModified = isModified;
    this.fileName = fileName;
    this.reset = reset;
  }

  public boolean isModified() {
    return isModified;
  }

  public String getFileName() {
    return fileName;
  }

  public boolean isReset() {
    return reset;
  }
}
