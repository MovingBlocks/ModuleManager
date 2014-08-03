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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A set of convenience methods that I would have put into jGit
 * @author Martin Steiger
 */
public class JGitWrapper implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(JGitWrapper.class);

    private final Git git;

    private Repository repository;

    /**
     * @param localPath
     * @throws IOException 
     */
    public JGitWrapper(File localPath) throws IOException {

        File gitDir = new File(localPath, ".git");
        repository = FileRepositoryBuilder.create(gitDir);
        git = new Git(repository);
    }

    public boolean repoExists() {
        return repository.getObjectDatabase().exists();
    }

    public void createRepo() throws IOException {
        repository.create();
    }

    public void setupRemote(String remote, URL url) throws IOException, URISyntaxException {

        StoredConfig config = git.getRepository().getConfig();
        RemoteConfig rc = new RemoteConfig(config, remote);
        rc.addURI(new URIish(url));
        rc.addFetchRefSpec(new RefSpec("+refs/heads/*:refs/remotes/" + remote + "/*"));
        rc.update(config);
        config.save();
    }

    public PullResult pullFromRemote(String remote) throws GitAPIException {
        return git.pull().setRemote(remote).call();
    }

    /**
     * @return a map id -> URL
     */
    public Map<String, String> listRemotes() {

        final String section = "remote";
        Config storedConfig = git.getRepository().getConfig();
        Set<String> remotes = storedConfig.getSubsections(section);
        Map<String, String> map = Maps.newHashMap();

        for (String remoteName : remotes) {
            String url = storedConfig.getString(section, remoteName, "url");
            map.put(remoteName, url);
        }

        return map;
    }

    public Iterable<RevCommit> listCommits() throws IOException, GitAPIException {

        ObjectId headId = git.getRepository().resolve(Constants.HEAD);
        if (headId == null) {
            logger.debug("Repository is empty");
            return Collections.emptyList();
        } else {
            return git.log().all().call();
        }
    }


    public List<String> findFiles(String regex) throws IOException {
        List<String> result = Lists.newArrayList();

        ObjectId lastCommitId = repository.resolve(Constants.HEAD);
        if (lastCommitId == null) {
            return result;
        }

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
    public InputStream openFile(String file) throws IOException {

        ObjectId lastCommitId = repository.resolve(Constants.HEAD);

        if (lastCommitId == null) {
            throw new IOException("Repository does not have a HEAD commit");
        }

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

    private static RevTree getRevTree(Repository repository) throws IOException {
        ObjectId lastCommitId = repository.resolve(Constants.HEAD);

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
