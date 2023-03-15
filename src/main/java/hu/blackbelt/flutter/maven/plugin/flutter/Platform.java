package hu.blackbelt.flutter.maven.plugin.flutter;

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

enum Architecture { x86, x64, ppc64le, s390x, arm64, armv7l;
    public static Architecture guess(){
        String arch = System.getProperty("os.arch");
        String version = System.getProperty("os.version");

        if (arch.equals("ppc64le")) {
            return ppc64le;
        } else if (arch.equals("aarch64")) {
            return arm64;
        } else if (arch.equals("s390x")) {
                return s390x;
        } else if (arch.equals("arm") && version.contains("v7")) {
                return armv7l;
        } else {
            return arch.contains("64") ? x64 : x86;
        }
    }
}

enum OS { Windows, Mac, Linux, SunOS;

    public static OS guess() {
        final String osName = System.getProperty("os.name");
        return  osName.contains("Windows") ? OS.Windows :
                osName.contains("Mac") ? OS.Mac :
                        osName.contains("SunOS") ? OS.SunOS :
                                OS.Linux;
    }

    public String getArchiveExtension(){
        if(this == OS.Windows || this == OS.Mac){
          return "zip";
        } else {
          return "tar.xz";
        }
    }

    public String getCodename(){
        /*
        if(this == OS.Mac){
            return "darwin";
        } else if(this == OS.Windows){
            return "win";
        } else if(this == OS.SunOS){
            return "sunos";
        } else {
            return "linux";
        } */
        if(this == OS.Mac){
            return "macos";
        } else if(this == OS.Windows){
            return "windows";
        } else {
            return "linux";
        }
    }

    /*
    public String getOsName(){
        if(this == OS.Mac){
            return "macos";
        } else if(this == OS.Windows){
            return "windows";
        } else {
            return "linux";
        }
    } */

}

enum Channel { stable, beta, dev}

class Platform {
    private final String flutterDownloadRoot;
    private final String flutterGitUrl;
    private final OS os;
    private final Architecture architecture;
    private final String classifier;
    private final Channel channel;

    public Platform(OS os, Architecture architecture) {
        this("https://storage.googleapis.com/flutter_infra/releases/",
                "https://github.com/flutter/flutter.git",
                os, architecture, Channel.stable, null);
    }

    public Platform(String flutterDownloadRoot, String flutterGitUrl, OS os, Architecture architecture, Channel channel, String classifier) {
        this.channel = channel;
        this.flutterDownloadRoot = flutterDownloadRoot;
        this.flutterGitUrl = flutterGitUrl;
        this.os = os;
        this.architecture = architecture;
        this.classifier = classifier;
    }

    public static Platform guess(){
        OS os = OS.guess();
        Architecture architecture = Architecture.guess();
        return new Platform(os, architecture);
    }

    public String getFlutterDownloadRoot(){
        return flutterDownloadRoot;
    }

    public String getFlutterGitUrl(){
        return flutterGitUrl;
    }

    public String getArchiveExtension(){
        return os.getArchiveExtension();
    }

    public String getCodename(){
        return os.getCodename();
    }

    public boolean isWindows(){
        return os == OS.Windows;
    }

    public boolean isMac(){
        return os == OS.Mac;
    }

    public boolean isLinux(){
        return os == OS.Linux;
    }

    public boolean isSunOS(){
        return os == OS.SunOS;
    }

    public String getLongFlutterFilename(String flutterVersion) {
        return "flutter_" + os.getCodename() + "_" + flutterVersion + "-" + channel.name();
    }

    public String getFlutterDownloadFilename(String flutterVersion) {
        return channel.name() + "/" + os.getCodename() + "/" + getLongFlutterFilename(flutterVersion) + "." + os.getArchiveExtension();
    }

}
