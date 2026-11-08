/*
 * This file is part of Limbo.
 *
 * Copyright (C) 2026. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2026. Contributors
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

package com.loohp.limbo.utils;

import net.querz.nbt.tag.ByteTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.DoubleTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongTag;
import net.querz.nbt.tag.StringTag;
import net.querz.nbt.tag.Tag;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CustomNBTUtils {

	public static Tag<?> getTagFromJson(Object json) {
		if (json instanceof JSONObject) {
			return getCompoundTagFromJson((JSONObject) json);
		} else if (json instanceof JSONArray) {
			return getListTagFromJson((JSONArray) json);
		} else if (json instanceof Boolean) {
			return new ByteTag((byte) ((boolean) json ? 1 : 0));
		} else if (json instanceof Long) {
			return new LongTag((long) json);
		} else if (json instanceof Double) {
			return new DoubleTag((double) json);
		} else if (json instanceof String) {
			return new StringTag((String) json);
		}
		throw new IllegalArgumentException("Unsupported JSON value: " + json);
	}
	
	public static CompoundTag getCompoundTagFromJson(JSONObject json) {
		CompoundTag tag = new CompoundTag();
		
		for (Object obj : json.keySet()) {
			String key = (String) obj;
			Object rawValue = json.get(key);

			tag.put(key, getTagFromJson(rawValue));
		}
		
		return tag;
	}
	
	public static ListTag<?> getListTagFromJson(JSONArray json) {
		if (json.isEmpty()) {
			return ListTag.createUnchecked(null);
		}
		ListTag<?> listTag = ListTag.createUnchecked(null);
		for (Object rawValue : json) {
			listTag.addUnchecked(getTagFromJson(rawValue));
		}
		return listTag;
	}

}
