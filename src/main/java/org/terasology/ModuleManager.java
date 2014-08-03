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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

/**
 * The main class that gives access to the module infos
 * @author Martin Steiger
 */
public class ModuleManager implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ModuleManager.class);

    private final Git git;
    private final Gson gson = new Gson();

    private Map<String, ModuleInfo> modInfos = Maps.newHashMap();

    private final String remoteName = Constants.DEFAULT_REMOTE_NAME;

    public ModuleManager(File localPath, URL remoteUrl) throws IOException, URISyntaxException {

        File gitDir = new File(localPath, ".git");
        Repository repository = FileRepositoryBuilder.create(gitDir);

        git = new Git(repository);

        // create if not existing
        if (!repository.getObjectDatabase().exists()) {
            repository.create();

            setupRemote(remoteName, remoteUrl);
        } else {
            logger.info("Found repository in {}", gitDir);
            parseFiles();
        }
    }

    public void updateRepo() throws GitAPIException, IOException {

        PullResult pr = git.pull().setRemote(remoteName).call();

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

        List<String> moduleFiles = findFiles("modules/.*/module.txt");

        logger.debug("Found module infos: {}", moduleFiles);

        for (String fname : moduleFiles) {
            try (InputStreamReader reader = new InputStreamReader(openFile(fname), Charsets.UTF_8)) {
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

    private void setupRemote(String remote, URL url) throws IOException, URISyntaxException {

        StoredConfig config = git.getRepository().getConfig();
        RemoteConfig rc = new RemoteConfig(config, remote);
        rc.addURI(new URIish(url));
        rc.addFetchRefSpec(new RefSpec("+refs/heads/*:refs/remotes/" + remote + "/*"));
        rc.update(config);
        config.save();
    }

    private List<String> findFiles(String regex) throws IOException {
        List<String> result = Lists.newArrayList();

        Repository repository = git.getRepository();
        RevTree tree = getRevTree(repository);

        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);

        while (treeWalk.next()) {
            String path = treeWalk.getPathString();
            if (path.matches(regex)) {
                result.add(path);
            }
        }

        return result;
    }

    /**
     * @param file relative to the root of the repository. Use '/' to delimit directories on all platforms.
     * @return
     * @throws IOException
     */
    private InputStream openFile(String file) throws IOException {

        Repository repository = git.getRepository();
        RevTree tree = getRevTree(repository);

        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        treeWalk.setFilter(PathFilter.create(file));
        if (!treeWalk.next()) {
            throw new FileNotFoundException(file);
        }
        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = repository.open(objectId);

        return loader.openStream();
    }

    private RevTree getRevTree(Repository repository) throws IOException {
        ObjectId lastCommitId = repository.resolve(Constants.HEAD);

        // TODO: move it some levels upwards to avoid the exception where possible
        if (lastCommitId == null) {
            throw new IOException("Repository does not have a HEAD commit");
        }

        // now we have to get the commit
        RevWalk revWalk = new RevWalk(repository);
        RevCommit commit = revWalk.parseCommit(lastCommitId);
        // and using commit's tree find the path
        RevTree tree = commit.getTree();
        return tree;
    }

    @Override
    public void close() throws IOException {
        git.close();
    }

}
