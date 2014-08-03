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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.FetchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

/**
 * The main class that gives access to the module infos
 * @author Martin Steiger
 */
public class ModuleManager implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ModuleManager.class);

    private final String remoteName = Constants.DEFAULT_REMOTE_NAME;

    private final Gson gson = new Gson();

    private Map<String, ModuleInfo> modInfos = Maps.newHashMap();

    private JGitWrapper wrapper;

    public ModuleManager(File localPath, URL remoteUrl) throws IOException, URISyntaxException {

        wrapper = new JGitWrapper(localPath);

        // create if not existing
        if (!wrapper.repoExists()) {
            logger.info("Creating repository in {}", localPath);
            wrapper.createRepo();
            wrapper.setupRemote(remoteName, remoteUrl);
        } else {
            logger.info("Found repository in {}", localPath);
            parseFiles();
        }
    }

    public void updateRepo() throws GitAPIException, IOException {

        PullResult pr = wrapper.pullFromRemote(remoteName);

        FetchResult fr = pr.getFetchResult();
        logger.info("Fetching from uri: " + fr.getURI().toString());

        MergeResult mr = pr.getMergeResult(); 
        logger.info("Merge Status: " + mr.getMergeStatus());

        if (!mr.getMergeStatus().isSuccessful()) {
            logger.info("Conflicting files: " + mr.getConflicts().keySet());

            // TODO: resolve using "theirs"
            return;
        }

        parseFiles();
    }

    /**
     * @return an unmodifiable collection
     */
    public Collection<ModuleInfo> getAll() {
        return Collections.unmodifiableCollection(modInfos.values());
    }

    public Optional<ModuleInfo> getById(String id) {
        return Optional.fromNullable(modInfos.get(id));
    }

    private void parseFiles() throws IOException {

        modInfos.clear();

        List<String> moduleFiles = wrapper.findFiles("modules/.*/module.txt");

        logger.debug("Found module infos: {}", moduleFiles);

        for (String fname : moduleFiles) {
            try (InputStreamReader reader = new InputStreamReader(wrapper.openFile(fname), Charsets.UTF_8)) {
                ModuleInfo info = gson.fromJson(reader, ModuleInfo.class);
                String id = info.getId();
                if (id != null) {
                    ModuleInfo prev = modInfos.put(id, info);
                    if (prev != null) {
                        logger.warn("ID {} already existing in database - overwriting", id);
                    }
                } else {
                    logger.warn("Encountered module info {} without id field", fname);
                }
            } catch (IOException e) {
                logger.warn("Could not open {}", fname);
            }
        }
    }

    @Override
    public void close() throws IOException {
        wrapper.close();
    }

}
