package no.entitas.gradle;

import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.NullOutputStream;

import java.util.logging.Level;
import org.tmatesoft.svn.util.SVNLogType;

import org.tmatesoft.svn.util.ISVNDebugLog;

public class NullSVNDebugLog implements ISVNDebugLog {
    public void logError(SVNLogType logType, String message) {
        System.out.println("ERROR: "+message);
    }
    
    public void logError(SVNLogType logType, Throwable th){
        th.printStackTrace();        
    };

    public void logSevere(SVNLogType logType, String message){
        System.out.println("SEVERE: "+message);
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

    public InputStream createLogStream(SVNLogType logType, InputStream is){
        return new NullInputStream(0);
    };
    
    public OutputStream createLogStream(SVNLogType logType, OutputStream os){
        return new NullOutputStream();
    };

    public OutputStream createOutputLogStream() {
        return new NullOutputStream();
    }

    public OutputStream createInputLogStream(){
        return new NullOutputStream();
    }

    public void flushStream(Object stream){};
}
