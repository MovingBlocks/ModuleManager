ModuleManager
=============

Package manager style library project for organizing Terasology modules

This library is functional, but still **incubating** and the API can change without prior notice.

Content
--------

This library uses [eclipse Aether](http://eclipse.org/aether/) to access the Terasology module repository (Maven2) and read out its contents.

Usage
--------

To use ModuleManager, you can write something like this:

```java
RepositoryConnector connector = new RepositoryConnector(root);
Collection<String> versions = connector.findAvailableVersions("Sample");
File file = connector.downloadArtifact("Sample", "0.1.0-SNAPSHOT");
Collection<String> deps = connector.getDependencies("Sample");
```

Easy as pie!

License
--------

Terasology is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html) and available in source code form at [GitHub](https://github.com/MovingBlocks/Terasology).
