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

import java.util.StringTokenizer;

/**
 * @author Burak Serdar (bserdar@redhat.com)
 */
public class RPMInfo {

    public String name;
    public String arch;
    public String epoch;
    public String version;
    public String release;
    public String repo;
    public String repoDir;
    
    public String toString() {
        String s=name+"."+arch+":"+epoch+":"+version+"-"+release;
        if(repo!=null)
            return s+"@"+repo;
        else
            return s;
    }

    private int compare(String thisStr,String rpmStr,String reqStr) {
        boolean thisEq=thisStr.equals(reqStr);
        boolean rpmEq=rpmStr.equals(reqStr);
        if(thisEq&&!rpmEq)
            return 1;
        else if(!thisEq&&rpmEq)
            return 0;
        else if(!thisEq&&!rpmEq)
            return -1;
        else
            return -2;
    }

    // Returns true if this RPM matches the request
    public boolean isMatch(RPMRequest req) {
        if(req.name.equals(name)&&
           req.arch.equals(arch)) {
            if( (req.epoch.equals(RPMRequest.LATEST)||
                 req.epoch.equals(epoch)) &&
                (req.version.equals(RPMRequest.LATEST)||
                 req.version.equals(version)) &&
                (req.release.equals(RPMRequest.LATEST)||
                 req.release.equals(release)))
                return true;
        }
        return false;
    }

    // Return true if this is a better match to req than rpm. Returns null if neither this nor rpm is a match
    public Boolean betterMatch(RPMInfo rpm,RPMRequest req) {
        if(rpm.name.equals(name)&&
           rpm.arch.equals(arch)) {
            

            if(req.epoch.equals(RPMRequest.LATEST)) {
                switch(compare(rpm.epoch,epoch)) {
                case -1: return true;
                case 1: return false;
                }
            } else {
                switch(compare(epoch,rpm.epoch,req.epoch)) {
                case 1: return true;
                case 0: return false;
                case -1: return null;
                }
            }
            // Same epoch, compare versions


            if(req.version.equals(RPMRequest.LATEST)) {
                switch(compare(rpm.version,version)) {
                case -1: return true;
                case 1: return false;
                }
            } else {
                // Wants exact version
                switch(compare(version,rpm.version,req.version)) {
                case 1: return true;
                case 0: return false;
                case -1: return null;
                }
            }
            // Same version, compare revisions

            if(req.release.equals(RPMRequest.LATEST)) {
                switch(compare(rpm.release,release)) {
                case -1:return true;
                case 1:return false;
                }
            } else {
                switch(compare(release,rpm.release,req.release)) {
                case 1: return true;
                case 0: return false;
                case -1: return null;
                }
            }
            // Same everything
            return false;
        } else
            return false;
    }

    private int compare(String s1,String s2) {
        if(s1.equals(s2))
            return 0;
        
        String[] x1=split(s1);
        String[] x2=split(s2);
        
        int min=x1.length;
        if(x2.length<min)
            min=x2.length;
        for(int i=0;i<min;i++) {
            if(!x1[i].equals(x2[i])) {
                Long i1=num(x1[i]);
                Long i2=num(x2[i]);
                if(i1!=null&&i2!=null) {
                    if(i1.equals(i2)) // Both numeric, but different strings, string comparison
                        return x1[i].compareTo(x2[i]);
                    else {
                        if(i1>i2)
                            return 1;
                        else 
                            return -1;
                    }
                } else if(i1!=null&&i2==null) { // s1 numeric, s2 not, s2 is greater
                    return -1;
                } else if(i1==null&&i2!=null) { // s1 non numeric, s2 is, s1 greater
                    return 1;
                } else { // both string
                    return x1[i].compareTo(x2[i]);
                }
            }
        }
        // Everything equal up to here, longer string is greater
        return x1.length>x2.length?1:-1;
    }

    private static final String delims=",.:- ;";
    private String[] split(String s) {
        StringTokenizer t=new StringTokenizer(s,delims);
        String[] x1=new String[t.countTokens()];
        for(int i=0;i<x1.length;i++)
            x1[i]=t.nextToken();
        return x1;
    }

    private static Long num(String s) {
        try {
            return Long.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        RPMInfo r1=new RPMInfo();
        RPMInfo r2=new RPMInfo();
        
        r1.name=r2.name="name";
        r1.arch=r2.arch="arch";

        r1.epoch="1";
        r2.epoch="1";

        r1.version="1.0.0";
        r2.version="1.1.0";

        r1.release="123";
        r2.release="124";

        
        RPMRequest req=new RPMRequest("name","arch:arch epoch:2");
        System.out.println(r2.betterMatch(r1,req));

        r2.epoch="1";
        System.out.println(r2.betterMatch(r1,req));
        
    }
}
