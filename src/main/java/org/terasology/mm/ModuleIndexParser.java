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
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;

/**
 * Parses module infos from a all-knowing json map
 * (as created by a python script in Jenkins).
 * @author Martin Steiger
 */
public class ModuleIndexParser {

    private static final Logger logger = LoggerFactory.getLogger(ModuleIndexParser.class);

    private Map<String, ModuleInfo> modInfos = new HashMap<>();


    public ModuleIndexParser(URL remoteUrl) throws IOException {

        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        TypeAdapter<ModuleInfo> adapter = gson.getAdapter(ModuleInfo.class);

        try (InputStreamReader reader = new InputStreamReader(remoteUrl.openStream(), Charset.forName("UTF-8"))) {

            // reading everything at once is possible with the following two lines
            // but we don't want the entire process to fail if individual modules
            // cannot be read.

            //  TypeToken<?> typeToken = new TypeToken<Map<String, ModuleInfo>>() { /* trick type erasure */ };
            //  Map<String, ModuleInfo> infos = new Gson().fromJson(reader, typeToken.getType());

            JsonElement tree = parser.parse(reader);

            if (!tree.isJsonObject()) {
                throw new IllegalStateException("Root element must be a map");
            }

            JsonObject jsonObject = tree.getAsJsonObject();

            for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                try {
                    ModuleInfo info = adapter.fromJsonTree(entry.getValue());
                    registerEntry(info);
                } catch (Exception e) {
                    logger.warn("Encountered invalid entry \"{}\" - {}", entry.getKey(), e.getMessage());
                }
            }

        }
    }

    private void registerEntry(ModuleInfo info) {
        String id = info.getId();
        if (id != null) {
            ModuleInfo prev = modInfos.put(id, info);
            logger.info("Found module info {}", id);
            logger.debug("Module details: {}", info);
            if (prev != null) {
                logger.warn("ID {} already existing in database - overwriting", id);
            }
        } else {
            logger.warn("Encountered module info without id field");
        }
    }

    /**
     * @return an unmodifiable collection
     */
    public Collection<ModuleInfo> getAll() {
        return Collections.unmodifiableCollection(modInfos.values());
    }

    /**
     * @return the module info or <code>null</code>
     */
    public ModuleInfo getById(String id) {
        return modInfos.get(id);
    }
}
