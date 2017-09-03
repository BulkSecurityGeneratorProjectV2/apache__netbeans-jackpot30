/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.netbeans.modules.parsing.api;

import org.netbeans.api.editor.mimelookup.MimePath;

/**
 *
 * @author lahvac
 */
public class SnapshotHack {

    public static Snapshot create(String text) {
        return Snapshot.create(text, new int[0], null, MimePath.get("text/x-java"), new int[][] {new int[] {0, 0}}, new int[][] {new int[] {0, 0}});
    }

}