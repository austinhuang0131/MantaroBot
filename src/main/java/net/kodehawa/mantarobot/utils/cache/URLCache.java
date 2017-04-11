package net.kodehawa.mantarobot.utils.cache;

import com.google.common.base.Preconditions;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class URLCache {
    public static final File DEFAULT_CACHE_DIR = new File("urlcache_files");
    public static final URLCache MISC_CACHE = new URLCache(40);
	private static final Logger LOGGER = LoggerFactory.getLogger("URLCache");
	private static final Map<String, File> saved = new ConcurrentHashMap<>();
    private final FileCache cache;
	private File cacheDir;

	public URLCache(File cacheDir, int cacheSize) {
	    this.cacheDir = cacheDir;
	    cache = new FileCache(cacheSize);
		if (cacheDir.isFile())
			cacheDir.delete();
		cacheDir.mkdirs();
	}

	public URLCache(int cacheSize) {
	    this(DEFAULT_CACHE_DIR, cacheSize);
    }

	public void changeCacheDir(File newDir) {
		if (newDir == null) throw new NullPointerException("newDir");
		if (!newDir.isDirectory()) throw new IllegalArgumentException("Not a directory: " + newDir);
		cacheDir = newDir;
	}

	public File getFile(String url) {
		File cachedFile = saved.get(Preconditions.checkNotNull(url, "url"));
		if (cachedFile != null) return cachedFile;
		File file = null;
		try {
			file = File.createTempFile(url.replace('/', '_').replace(':', '_'), "cache", cacheDir);
			try (InputStream is = Unirest.get(url).asBinary().getRawBody();
				 FileOutputStream fos = new FileOutputStream(file)) {
				byte[] buffer = new byte[1024];
				int read;
				while ((read = is.read(buffer)) != -1)
					fos.write(buffer, 0, read);
				saved.put(url, file);
				return file;
			}
		} catch (Exception e) {
			if (file != null) file.delete();
			LOGGER.error("Error caching", e);
			throw new InternalError();
		}
	}

	public InputStream getInput(String url) {
		return cache.input(getFile(url));
	}
}