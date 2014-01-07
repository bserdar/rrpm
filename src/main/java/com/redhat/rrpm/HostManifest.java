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

import java.io.FileReader;
import java.io.File;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.StringTokenizer;


/**
 * Keeps the RPMs required on a host
 *
 * @author Burak Serdar (bserdar@redhat.com)
 */
public class HostManifest {

    private final String host;
    private final Map<String,RPMRequest> requests=new HashMap<String,RPMRequest>();

    private static final String[] reservedNames={"host","import"};

    public HostManifest(String file) throws Exception {
        Properties p=read(file);

        host=p.getProperty("host");
        if(host==null)
            throw new RuntimeException("host name is required in "+file);

        for(Object x:p.keySet()) {
            String name=(String)x;
            if(!reservedName(name)) {
                // This is an rpm
                String version=p.getProperty(name);
                RPMRequest request=new RPMRequest(name,version);
                if(requests.get(name)!=null)
                    throw new RuntimeException("Duplicate RPM name in "+file+":"+name);
                requests.put(name,request);
            }
        }

        HashSet<String> ctx=new HashSet<String>();
        p=processImports(file,p.getProperty("import"),ctx);
        if(p!=null) {
            for(Object x:p.keySet()) {
                String name=(String)x;
                if(!reservedName(name))
                    if(!requests.containsKey(name))
                        requests.put(name,new RPMRequest(name,p.getProperty(name)));
            }
        }
    }

    private Properties inherit(Properties sub,Properties zuper) {
        Properties p=new Properties();
        for(Object x:zuper.keySet())
            if(!reservedName((String)x))
                p.setProperty((String)x,zuper.getProperty((String)x));
        for(Object x:sub.keySet())
            if(!reservedName((String)x))
                p.setProperty((String)x,sub.getProperty((String)x));
        return p;
    }

    private Properties processImports(String parentFile,String imports,HashSet<String> ctx) throws Exception {
        Properties properties=null;
        if(imports!=null) {
            StringTokenizer tok=new StringTokenizer(imports,", ");
            while(tok.hasMoreTokens()) {
                String importFile=tok.nextToken();
                Properties p=processImport(parentFile,importFile,ctx);
                if(properties==null)
                    properties=p;
                else
                    properties=inherit(p,properties);
            }
        }
        return properties;
    }

    private Properties processImport(String parentFile,String importFile,HashSet<String> ctx) throws Exception {
        if(!ctx.add(importFile))
            throw new RuntimeException("Cyclic inheritance involving "+importFile);

        File dir=new File(parentFile);
        dir=dir.getParentFile();
        File ifile=dir==null?new File(importFile):new File(dir,importFile);
        Properties p=read(ifile);
        Properties importedProperties=processImports(ifile.toString(),p.getProperty("import"),ctx);
        if(importedProperties!=null)
            p=inherit(p,importedProperties);
        return p;
    }
    
    private Properties read(String file) throws Exception {
        return read(new File(file));
    }
    
    private Properties read(File file) throws Exception {
        FileReader reader=new FileReader(file);
        Properties p=new Properties();
        p.load(reader);
        reader.close();
        return p;
    }

    public String getHost() {
        return host;
    }

    public RPMRequest[] getRPMs() {
        Collection<RPMRequest> c=requests.values();
        return c.toArray(new RPMRequest[c.size()]);
    }

    public String toString() {
        return requests.toString();
    }

    private boolean reservedName(String s) {
        for(String x:reservedNames)
            if(x.equals(s))
                return true;
        return false;
    }
}
