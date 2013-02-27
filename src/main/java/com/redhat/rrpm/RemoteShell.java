/*
    Copyright 2013 Red Hat, Inc. and/or its affiliates.

    This file is part of rrpm.

    rrpm is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    rrpm is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with rrpm.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.redhat.rrpm;

import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Runs commands remotely. 
 *
 * @author Burak Serdar (bserdar@redhat.com)
 */
public class RemoteShell implements Shell {

    private final String host;
    private final String user;

    private static final class ReaderThread extends Thread {
        private final InputStream stream;
        private final StringBuffer buf=new StringBuffer();

        public ReaderThread(InputStream stream) {
            this.stream=stream;
        }

        public void run() {
            InputStreamReader reader=new InputStreamReader(stream);
            int i;
            try {
                while((i=reader.read())!=-1)
                    buf.append((char)i);
            } catch (Exception e) {}
        }

        public String toString() {
            return buf.toString();
        }
    }

    public RemoteShell(String hostName,String userName) {
        this.host=hostName;
        this.user=userName;
    }

    public ProcessResult exec(String[] rargs) throws Exception {
        Runtime runtime=Runtime.getRuntime();
        String[] cmdArray=new String[rargs.length+2];
        cmdArray[0]="ssh";
        cmdArray[1]=user==null?host:(user+"@"+host);
        for(int i=0;i<rargs.length;i++)
            cmdArray[i+2]=rargs[i];

        ProcessResult result=new ProcessResult();
        Process p=runtime.exec(cmdArray);
        ReaderThread outReader=new ReaderThread(p.getInputStream());
        outReader.start();
        ReaderThread errReader=new ReaderThread(p.getErrorStream());
        errReader.start();
        
        result.setReturnCode(p.waitFor());
        outReader.join();
        errReader.join();
        result.setStdOut(outReader.toString());
        result.setStdErr(errReader.toString());
        return result;
    }

    public static void main(String[] args) throws Exception {
        RemoteShell shell=new RemoteShell(args[0],args[1]);
        String[] cmdArgs=new String[args.length-2];
        for(int i=0;i<cmdArgs.length;i++)
            cmdArgs[i]=args[i+2];
        ProcessResult result=shell.exec(cmdArgs);
        System.out.println("return code:"+result.getReturnCode());
        System.out.println("out:"+result.getStdOut());
        System.out.println("err:"+result.getStdErr());
    }

}
