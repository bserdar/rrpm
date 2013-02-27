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

import java.net.URL;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Collection;

import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Burak Serdar (bserdar@redhat.com)
 */
public class YumRepo {

    public final String repoName;
    public final int sequence;
    public final URL repoUrl;

    private Map<String,List<RPMInfo>> rpmInfo=null;

    public YumRepo(String repoName,URL url,int sequence) throws Exception {
        this.repoName=repoName;
        this.repoUrl=url;
        this.sequence=sequence;
    }

    /**
     * Tries to find the best matching rpms in this repo 
     */
    public RPMInfo resolve(RPMRequest req) throws Exception {
        loadRepoInfo();

        List<RPMInfo> rpms=rpmInfo.get(req.name);
        if(rpms!=null&&!rpms.isEmpty()) {
            RPMInfo resolved=null;
            for(RPMInfo info:rpms) {
                if(req.arch==null||info.arch.equals(req.arch)) {
                    if(resolved==null) {
                        if(info.isMatch(req))
                            resolved=info;
                    }  else {
                        Boolean b=info.betterMatch(resolved,req);
                        if(b!=null&&b)
                            resolved=info;
                    }
                }
            }
            return resolved;
        } else
            return null;
    }


    private void loadRepoInfo() throws Exception {
        if(rpmInfo==null) {
            System.out.print("Loading repomd:"+repoName+"...");
            System.out.flush();
            rpmInfo=new HashMap<String,List<RPMInfo>>();
            RPMInfo info[]=getYumRepoInfo(repoUrl);
            for(RPMInfo x:info) {
                List l=rpmInfo.get(x.name);
                if(l==null)
                    rpmInfo.put(x.name,l=new ArrayList<RPMInfo>());
                x.repo=repoName;
                l.add(x);
            }
            System.out.println("Done");
        }
    }

    public static RPMInfo[] getYumRepoInfo(URL url) throws Exception {
        URL file=new URL(url.toString()+"/repodata/primary.xml.gz");
        InputStream stream=file.openStream();
        GZIPInputStream gzi=new GZIPInputStream(stream);

        Document doc=DocumentBuilderFactory.
            newInstance().newDocumentBuilder().parse(gzi);

        Element root=doc.getDocumentElement();
        if(root.getTagName().equals("metadata")) {
            ArrayList<RPMInfo> list=new ArrayList<RPMInfo>();
            Node pkg;
            for(pkg=root.getFirstChild();pkg!=null;pkg=pkg.getNextSibling()) {
                if(pkg.getNodeType()==Node.ELEMENT_NODE&&
                   pkg.getNodeName().equals("package")&&
                   "rpm".equals(((Element)pkg).getAttribute("type"))) {
                    RPMInfo info=new RPMInfo();
                    Node inpkg;
                    for(inpkg=pkg.getFirstChild();inpkg!=null;inpkg=inpkg.getNextSibling()) {
                        if(inpkg.getNodeType()==Node.ELEMENT_NODE) {
                            if(inpkg.getNodeName().equals("name"))
                                info.name=inpkg.getTextContent().trim();
                            else if(inpkg.getNodeName().equals("arch"))
                                info.arch=inpkg.getTextContent().trim();
                            else if(inpkg.getNodeName().equals("version")) {
                                info.epoch=((Element)inpkg).getAttribute("epoch");
                                info.version=((Element)inpkg).getAttribute("ver");
                                info.release=((Element)inpkg).getAttribute("rel");
                            } else if(inpkg.getNodeName().equals("location")) 
                                info.repoDir=((Element)inpkg).getAttribute("href");
                        }
                    }
                    list.add(info);
                }
            }
            stream.close();
            return list.toArray(new RPMInfo[list.size()]);
        } else
            throw new RuntimeException("Unexpected element:"+root.getTagName());
    }

    public static YumRepo find(Collection<YumRepo> coll,String name) {
        for(YumRepo x:coll)
            if(x.repoName.equals(name))
                return x;
        return null;
    }

    public static void main(String[] args) throws Exception {
        RPMInfo[] info=getYumRepoInfo(new URL(args[0]));
        for(RPMInfo x:info)
            System.out.println(x);
    }
}
