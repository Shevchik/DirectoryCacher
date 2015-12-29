package directorycacher;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;

public class DirectoryWatchService {

	private final File directory;
	public DirectoryWatchService(File directory) {
		this.directory = directory;
	}

	public void start() throws IOException {
		ExceptionShutdown.log("Starting watching of directory "+directory.getAbsolutePath());
		Path path = directory.toPath();
		WatchService service = path.getFileSystem().newWatchService();
		new DirectoryWatchServicePoller(directory, service).start();
	}

	private static class DirectoryWatchServicePoller extends Thread {

		private final Path root;
		private final WatchService wservice;
		private final HashMap<WatchKey, Path> wKeyToDir = new HashMap<>();
		private final HashMap<Path, WatchKey> dirToWKey = new HashMap<>();
		private final FileLocker locker = new FileLocker();
		public DirectoryWatchServicePoller(File directory, WatchService wservice) throws IOException {
			this.root = directory.toPath().toAbsolutePath();
			this.wservice = wservice;
			//lock already existing files
			ArrayList<Path> dirsToWatch = new ArrayList<Path>();
			Files.walkFileTree(this.root, new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					dirsToWatch.add(dir);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					locker.lockFile(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
			//watch detected directories
			for (Path dirToWatch : dirsToWatch) {
				watchDir(dirToWatch);
			}
		}

		@Override
		public void run() {
			try {
				for (;;) {
					WatchKey wkey = wservice.take();
					for (WatchEvent<?> event : wkey.pollEvents()) {
						//get root directory for this key
						Path parent = wKeyToDir.get(wkey);
						//handle overflow
						if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
							ExceptionShutdown.err("Overflow while watching directory "+parent);
							continue;
						}
						//resolve full path
						Path full = parent.resolve((Path) event.context());
						//handle create/delete
						if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
							//if the path is directory - watch, if file - lock
							if (full.toFile().isDirectory()) {
								watchDir(full);
							} else {
								locker.lockFile(full);
							}
						} else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
							//if the path is directory - unwatch it, if file - unlock
							WatchKey directoryWKey = dirToWKey.remove(full);
							if (directoryWKey != null) {
								wKeyToDir.remove(directoryWKey);
								directoryWKey.cancel();
							} else {
								locker.unlockFile(full);
							}
						} else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
							if (!full.toFile().isDirectory()) {
								locker.relockFile(full);
							}
						}
					}
					wkey.reset();
				}
			} catch (ClosedWatchServiceException e) {
			} catch (Throwable e) {
				ExceptionShutdown.shutdown("Error occured while watching directory "+ root, e);
			}
		}

		private void watchDir(Path dir) throws IOException {
			WatchKey wkey = dir.register(wservice, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
			wKeyToDir.put(wkey, dir);
			dirToWKey.put(dir, wkey);
		}
		
	}

}
