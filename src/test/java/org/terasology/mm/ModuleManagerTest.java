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

package org.terasology.mm;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public class ModuleManagerTest {

    @Test
    public void testJsonParser() throws URISyntaxException, IOException {
        URI jenkins = new URI("http://jenkins.terasology.org/");
        URL index = jenkins.resolve("job/UpdateModuleIndex/lastSuccessfulBuild/artifact/index.json").toURL();

        URL mini = ModuleManagerTest.class.getResource("/index_mini.json");

        ModuleManager mm = new ModuleManager(mini);
        System.out.println(mm.getAll());
    }
}

