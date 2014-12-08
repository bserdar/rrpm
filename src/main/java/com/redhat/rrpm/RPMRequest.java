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

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Valid RPM requests:
 * 
 * <pre>
 *    name=[arch:<arch>] [epoch:<epoch>] [version:<version>] [release:<release>] [repo:<repo>] [action:install|erase]
 * </pre>
 * <ul>
 * <li>epoch: optional. If not given, LATEST is assumed.</li>
 * <li>ver: required. LATEST also works.</li>
 * <li>rel: optional. If not given, LATEST is assumed.</li>
 * <li>repo: optional. If given, the rpm is picked from this repo.</li>
 * <li>install|erase: optional action. If not given, rpm is installed.</li>
 * </ul>
 *
 * @author Burak Serdar (bserdar@redhat.com)
 */
public class RPMRequest {

    public static final String LATEST="LATEST";

    public enum Action {install,erase};

    public final String name;
    public final String arch;
    public final String epoch;
    public final String version;
    public final String release;
    public final Action action;
    public final String repo;
    
    public RPMRequest(String name,String versionReq) throws Exception {
        this.name=name;

        Map<String,String> fieldMap=parse(versionReq);
        arch=fieldMap.get("arch");

        String s=fieldMap.get("epoch");
        if(s==null)
            s=LATEST;
        epoch=s;

        s=fieldMap.get("version");
        if(s==null)
            s=LATEST;
        version=s;

        s=fieldMap.get("release");
        if(s==null)
            s=LATEST;
        release=s;

        repo=fieldMap.get("repo");
        
        s=fieldMap.get("action");
        if(s==null)
            s="install";
        action=Action.valueOf(s);
    }

    /**
     * Creates a copy of an RPMRequest, but overrides the version and release
     * @param r request to copy
     * @param versionOverride version to override with
     * @param releaseOverride release to override with
     */
    public RPMRequest(RPMRequest r, String versionOverride, String releaseOverride) {
        name = r.name;
        arch = r.arch;
        epoch = r.epoch;
        version = versionOverride;
        release = releaseOverride != null ? releaseOverride : LATEST;
        action = r.action;
        repo = r.repo;
    }

    private Map<String,String> parse(String s) {
        StringBuffer buf=new StringBuffer();
        int state=0;
        int n=s.length();
        HashMap<String,String> ret=new HashMap<String,String>();
        String fieldName=null;
        for(int i=0;i<n;i++) {
            char c=s.charAt(i);
            switch(state) {
            case 0: // between fields
                if(Character.isWhitespace(c))
                    ;
                else if(Character.isLetter(c)) {
                    state=1;
                    buf=new StringBuffer();
                    buf.append(c);
                } else
                    throw new RuntimeException("Cannot parse:"+s);
                break;

            case 1: // Parsing a field name
                if(Character.isLetter(c)) {
                    buf.append(c);
                } else if(c==':') {
                    state=3;
                    fieldName=buf.toString();
                } else if(Character.isWhitespace(c)) {
                    state=2;
                    fieldName=buf.toString();
                } else
                    throw new RuntimeException("Cannot parse:"+s);
                break;

            case 2: // Field name completed, waiting ':'
                if(c==':')
                    state=3;
                else if(!Character.isWhitespace(c))
                    throw new RuntimeException("Cannot parse:"+s);
                break;

            case 3: // Field name parsed, ':' seen
                if(Character.isWhitespace(c))
                    ;
                else {
                    buf=new StringBuffer();
                    state=4;
                    buf.append(c);
                }
                break;

            case 4: // Field value
                if(!Character.isWhitespace(c)) {
                    buf.append(c);
                } else {
                    ret.put(fieldName,buf.toString());
                    state=0;
                }
                break;
            }
        }
        if(state==4)
            ret.put(fieldName,buf.toString());
        else if(state!=0)
            throw new RuntimeException("Cannot parse:"+s);
        return ret;
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        buf.append(name);
        if(arch!=null)
            buf.append('.').append(arch);
        buf.append(' ');
        buf.append(epoch).append(':').append(version).append('-').append(release);
        if(repo!=null)
            buf.append('@').append(repo);
        buf.append(' ').append(action);
        return buf.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println(new RPMRequest("test-rpm","epoch : x arch:noarch version:1.x.SNAPSHOT release:123-2 repo:myrepo action:install").toString());
        System.out.println(new RPMRequest("test-rpm","version:1.0.SNAPSHOT  repo:myrepo").toString());
    }
}
