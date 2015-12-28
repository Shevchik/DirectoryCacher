package directorycacher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirectoryList {

	private final ArrayList<File> files = new ArrayList<File>();

	public List<File> getList() {
		return Collections.unmodifiableList(files);
	}

	public void load(String listfilepath) throws IOException {
		File listfile = new File(listfilepath);
		if (!listfile.exists()) {
			listfile.createNewFile();
		}
		files.clear();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(listfilepath)), StandardCharsets.UTF_8))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				files.add(new File(line));
			}
		}
	}

}
