/*
 * Copyright 2011- the original author or authors.
 *
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
 */
package no.entitas.gradle.svn;

import java.util.logging.Level;

import org.gradle.api.Project;
import org.tmatesoft.svn.core.internal.util.DefaultSVNDebugLogger;
import org.tmatesoft.svn.util.SVNLogType;

public class NullSVNDebugLog extends DefaultSVNDebugLogger {
    Project project;
            
    public NullSVNDebugLog(Project project) {
        this.project = project;
    }

    public void logError(SVNLogType logType, String message) {
        project.getLogger().error("ERROR: "+message);
    }
    
    public void logError(SVNLogType logType, Throwable th){
        th.printStackTrace();        
    };

    public void logSevere(SVNLogType logType, String message){
        project.getLogger().error("SEVERE: "+message);
    };

    public void logSevere(SVNLogType logType, Throwable th){
        th.printStackTrace();
    };

    public void logFine(SVNLogType logType, Throwable th){};

    public void logFine(SVNLogType logType, String message){};

    public void logFiner(SVNLogType logType, Throwable th){};

    public void logFiner(SVNLogType logType, String message){};

    public void logFinest(SVNLogType logType, Throwable th){};

    public void logFinest(SVNLogType logType, String message){};
    
    public void log(SVNLogType logType, Throwable th, Level logLevel){};
    
    public void log(SVNLogType logType, String message, Level logLevel){};
    
    public void log(SVNLogType logType, String message, byte[] data){};
}
