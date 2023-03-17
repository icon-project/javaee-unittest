/*
 * Copyright 2023 PARAMETA Inc.
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

package score;

import com.iconloop.score.test.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class EventLogger {

    private List<Event> logs;

    private Stack<List<Event>> frames;

    public EventLogger() {
        frames = new Stack<List<Event>>();
        logs = new ArrayList<Event>();
    }

    void addLog(Event log) {
        logs.add(log);
    }

    void push() {
        frames.push(logs);
        logs = new ArrayList<Event>();
    }
    void pop() {
        logs = frames.pop();
    }

    void apply() {
        frames.peek().addAll(logs);
    }

    public List<Event> getLogs() {
        return List.copyOf(logs);
    }
}
