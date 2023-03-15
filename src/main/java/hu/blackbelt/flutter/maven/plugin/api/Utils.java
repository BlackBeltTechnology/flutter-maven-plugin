package hu.blackbelt.flutter.maven.plugin.api;

/*-
 * #%L
 * flutter-maven-plugin
 * %%
 * Copyright (C) 2018 - 2023 BlackBelt Technology
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Utils {
    public static List<String> merge(List<String> first, List<String> second) {
        ArrayList<String> result = new ArrayList<String>(first);
        result.addAll(second);
        return result;
    }

    public static List<String> prepend(String first, List<String> list){
        return merge(Arrays.asList(first), list);
    }

    public static String normalize(String path){
        return path.replace("/", File.separator);
    }

    public static String implode(String separator, List<String> elements){
        StringBuffer s = new StringBuffer();
        for(int i = 0; i < elements.size(); i++){
            if(i > 0){
                s.append(" ");
            }
            s.append(elements.get(i));
        }
        return s.toString();
    }

    public static boolean isRelative(String path) {
        return !path.startsWith("/") && !path.startsWith("file:") && !path.matches("^[a-zA-Z]:\\\\.*");
    }
}
