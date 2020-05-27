/*
 * Copyright 2020 NAFU_at.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package page.nafuchoco.neojukepro.core.playlist;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlaylistItem {
    @JsonProperty("name")
    private final String name;
    @JsonProperty("sourceName")
    private final String sourceName;
    @JsonProperty("url")
    private final String url;

    public PlaylistItem(String name, String sorceName, String url) {
        this.name = name;
        this.sourceName = sorceName;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getUrl() {
        return url;
    }
}
