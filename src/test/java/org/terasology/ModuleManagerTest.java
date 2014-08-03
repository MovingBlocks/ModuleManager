/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;

import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Tests the {@link ModuleManager} class.
 * TODO: replace web URL with local zip archive - the problem with that is that jGit does not support jar urls in fetch 
 * TODO: replace local tmp folder with in-memory FS. By definition in-mem FS do not support Path.toFile() which is necessary for jGit
 *       The InMemory repository of jGIt does not support fetching.
 * @author Martin Steiger
 */
public final class ModuleManagerTest {

    public static void main(String[] args) throws IOException, URISyntaxException, GitAPIException {
        URL url = new URL("https://github.com/Terasology/Index.git");
        File localPath = Files.createTempDirectory("module-manager-junit").toFile();

        try (ModuleManager mm = new ModuleManager(localPath, url)) {
            mm.updateRepo();
            for (ModuleInfo info : mm.getAll()) {
                System.out.println(info);
            }
        }
    }
}
