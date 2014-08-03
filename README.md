ModuleManager
=============

Package manager style library project for organizing Terasology modules

This library is functional, but still **incubating** and the API can change without prior notice.

Content
--------

This library uses jGit to clone the Terasology index repository and parse its contents.
Unfortunately, the InMemory-version of `Repository` doesn't support fetching from a remote repository (it's a bare repository only).

Usage
--------

To use ModuleManager, you can write something like this:

	URL url = new URL("https://github.com/Terasology/Index.git");
	try (ModuleManager mm = new ModuleManager(localPath, url)) {
		mm.updateRepo();
		for (ModuleInfo info : mm.getAll()) {
			System.out.println(info);
		}
	}


License
--------

Terasology is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html) and available in source code form at [GitHub](https://github.com/MovingBlocks/Terasology).
