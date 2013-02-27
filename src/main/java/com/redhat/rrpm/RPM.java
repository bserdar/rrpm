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

import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.StringTokenizer;

/**
 * @author Burak Serdar (bserdar@redhat.com)
 */
public class RPM {

    private final Shell shell;
    
    public RPM(Shell shell) {
        this.shell=shell;
    }

    public RPMInfo[] getRPMInfo(RPMRequest[] packages) throws Exception {
        ArrayList<String> argList=new ArrayList();
        argList.add("rpm");
        argList.add("-q");
        argList.add("--qf");
        argList.add("'%{NAME} %{EPOCH} %{VERSION} %{RELEASE} %{ARCH}\\n'");
        for(RPMRequest x:packages) {
            if(x.arch==null)
                argList.add(x.name);
            else
                argList.add(x.name+"."+x.arch);
        }
        ProcessResult result=exec(argList,false);
        BufferedReader rd=new BufferedReader(new StringReader(result.getStdOut()));
        String line;
        ArrayList<RPMInfo> list=new ArrayList<RPMInfo>();
        while((line=rd.readLine())!=null) {
            if(line.startsWith("package ")&&line.endsWith(" not installed")) {
            } else {
                RPMInfo info=new RPMInfo();
                StringTokenizer tok=new StringTokenizer(line," ");
                info.name=tok.nextToken();
                info.epoch=tok.nextToken();
                info.version=tok.nextToken();
                info.release=tok.nextToken();
                info.arch=tok.nextToken();
                list.add(info);
            }
        }
        return list.toArray(new RPMInfo[list.size()]);
    }

    public void delete(String[] packages) throws Exception {
        ArrayList<String> argList=new ArrayList();
        argList.add("rpm");
        argList.add("-e");
        argList.add("--nodeps");
        for(String x:packages)
            argList.add(x);
        exec(argList,true);
    }

    public void install(String[] packages) throws Exception {
        ArrayList<String> argList=new ArrayList();
        boolean first=true;
        for(String x:packages) {
            if(first)
                first=false;
            else
                argList.add("&&");
            argList.add("rpm");
            argList.add("-i");
            argList.add("--nodeps");
            argList.add(x);
        }
        exec(argList,true);
    }

    private ProcessResult exec(List<String> argList,boolean strictErr) throws Exception {
        for(String x:argList) {
            System.out.print(x);
            System.out.print(' ');
        }
        System.out.println("");
        ProcessResult result=shell.exec(argList.toArray(new String[argList.size()]));
        if(strictErr) {
            if(result.getReturnCode()!=0)
                if(result.getStdErr().trim().length()>0||
                   result.getStdOut().trim().length()>0)
                    throw new RuntimeException(result.toString());
        } else
            if(result.getStdErr().trim().length()>0&&result.getReturnCode()!=0)
                throw new RuntimeException(result.toString());
        return result;
    }
}
