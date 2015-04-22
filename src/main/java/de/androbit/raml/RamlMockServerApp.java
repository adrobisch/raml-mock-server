package de.androbit.raml;

import de.androbit.nibbler.RestHttpServerConfiguration;
import de.androbit.nibbler.netty.NettyHttpServer;
import org.raml.emitter.RamlEmitter;
import org.raml.model.Raml;
import org.raml.parser.visitor.RamlDocumentBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class RamlMockServerApp {
  public static NettyHttpServer mockServer;

  public static void main(String[] args) {
    File ramlFile = ramlFromWorkingDirOrAbsoluteFile(args[0]);

    if (!ramlFile.isFile()) {
      throw new IllegalArgumentException("first argument must be a file");
    }

    mockServer = startMockServer(ramlFile);
    waitForChangesAndRestart(ramlFile);
  }

  private static NettyHttpServer startMockServer(File ramlFile) {
    Raml raml = parseRaml(ramlFile);

    System.out.println("starting mock server for RAML:\n" + new RamlEmitter().dump(raml));

    RestHttpServerConfiguration config = new RestHttpServerConfiguration()
      .withService(new RamlMockService(raml));

    NettyHttpServer httpServer = new NettyHttpServer();
    httpServer.start(config);

    return httpServer;
  }

  private static void waitForChangesAndRestart(File ramlFile) {
    Path ramlFilePath = ramlFile.getParentFile().toPath();
    WatchService folderWatchService = watchService(ramlFilePath);

    while (true) {
      try {
        WatchResult watchedFolder = watchFolder(folderWatchService);

        if (watchedFolder.isModified && watchedFolder.getFileName().startsWith(ramlFile.getName())) {
          mockServer.stop();
          mockServer = startMockServer(ramlFile);
        } else if (!watchedFolder.reset) {
          break;
        }

        printAliveSign();
      } catch (Exception e) {
        e.printStackTrace();
        break;
      }
    }

    try {
      folderWatchService.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static WatchResult watchFolder(WatchService watchService) {
    try {
      WatchKey watchKey = watchService.poll(750, TimeUnit.MILLISECONDS);

      if (watchKey == null) {
        return new WatchResult(false, null, true);
      }

      for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
        System.out.printf("watch event:" + watchEvent);
        // Get the type of the event
        WatchEvent.Kind<?> kind = watchEvent.kind();
        if (StandardWatchEventKinds.OVERFLOW == kind) {
          continue; // loop
        } else if (StandardWatchEventKinds.ENTRY_MODIFY == kind) {
          // modified
          Path newPath = ((WatchEvent<Path>) watchEvent)
            .context();
          return new WatchResult(true, newPath.toString(), watchKey.reset());
        }
      }
      return new WatchResult(false, null, watchKey.reset());
    } catch (InterruptedException e) {
      Thread.interrupted();
      throw new RuntimeException(e);
    }
  }

  private static WatchService watchService(Path ramlFilePath) {
    try {
      WatchService watchService = ramlFilePath.getFileSystem().newWatchService();
      ramlFilePath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
      return watchService;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void printAliveSign() {
    System.out.printf(".");
  }

  private static File ramlFromWorkingDirOrAbsoluteFile(String path) {
    return new File(System.getProperty("user.dir") + File.separatorChar + path);
  }

  private static Raml parseRaml(File ramlFile) {
    try(FileInputStream fileInputStream = new FileInputStream(ramlFile)) {
      return new RamlDocumentBuilder()
        .build(fileInputStream);
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }
}
