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

import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public class LoggingProgressMonitor implements ProgressMonitor {

    private final Logger logger;
    private String curTask;
    private int maxWork;
    private int curWork;

    /**
     * @param logger
     */
    public LoggingProgressMonitor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void beginTask(String title, int totalWork) {
        this.curTask = title;
        this.curWork = 0;
        this.maxWork = totalWork;
        logger.info("Starting: " + title);
    }

    @Override
    public void start(int totalTasks) {
        // ignore
    }

    @Override
    public void update(int completed) {
        curWork += completed;
        logger.info("{} ({}/{})", curTask, curWork, maxWork);
    }

    @Override
    public void endTask() {
        logger.info(curTask + " finished");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

}
