/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api;

import org.gradle.apiImpl.Impl;

import java.util.ArrayList;


public class PersonList {
    private ArrayList<String> persons = new ArrayList<String>();

    public void doSomethingWithImpl() {
        org.apache.commons.lang.builder.ToStringBuilder stringBuilder;
        try {
            Class.forName("org.apache.commons.io.FileUtils");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        new Impl().implMethod();
    }

}
