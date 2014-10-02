/**
 * Copyright 2014 Felipe Estrada-Solano <festradasolano at gmail>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.floodlightcontroller.flatfilerecord.util;

import java.io.File;

/**
 * Provides methods to build path to files.
 * 
 * Copyright 2014 Felipe Estrada-Solano <festradasolano at gmail>
 * 
 * Distributed under the Apache License, Version 2.0
 * 
 * @author festradasolano
 */
public class FilePath {

	/**
	 * Returns file system separator string.
	 * 
	 * @return File system separator ("/" on Unix, "\" on Windows)
	 */
	public static String getFileSeparator() {
		return System.getProperty("file.separator");
	}

	/**
	 * Returns path to requested directory and creates it if necessary.
	 * 
	 * @param dirs
	 *            Array of directories to request path
	 * @return Path to directory if exists or was successfully created. Null if
	 *         such directory could not be created.
	 */
	public static String getDirectoryPath(String[] dirs) {
		String dirPath = getUserHomeDirectory();
		for (String dir : dirs) {
			dirPath += dir + getFileSeparator();
		}
		File dirFile = new File(dirPath);
		return (dirFile.exists() || dirFile.mkdirs()) ? dirPath : null;
	}

	/**
	 * Returns full path to the requested file.
	 * 
	 * @param dirs
	 *            Array of directories to reach file
	 * @param filename
	 *            Name of the file
	 * @return Full path to the file
	 */
	public static String getFilePath(String[] dirs, String filename) {
		String logDir = getDirectoryPath(dirs);
		if (logDir != null) {
			return logDir + filename;
		} else {
			return null;
		}
	}

	/**
	 * Returns path to user's home directory.
	 * 
	 * @return Path to users home directory, with file separator appended.
	 */
	public static String getUserHomeDirectory() {
		return System.getProperty("user.home") + getFileSeparator();
	}
	
}
