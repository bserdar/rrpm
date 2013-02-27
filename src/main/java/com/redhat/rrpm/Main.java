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
import java.util.Properties;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Arrays;

import java.io.FileReader;

import java.net.URL;

/**
 * @author Burak Serdar (bserdar@redhat.com)
 * @author Naveen Malik (nmalik@redhat.com)
 */
public class Main {
    
    private static final class Options {
        boolean refreshSnapshots;
        boolean refreshEverything;
        boolean printOnly;
        boolean noNewInstalls;
        boolean noDeletions;
        boolean noUpdates;

    }

    public static void main(String[] args) throws Exception {
        Options options=new Options();
        if(args.length==0) {
            System.out.println("rrpm [-r repofile ...] [-s] [-a] [-l username] manifest ...\n"+
                               " where \n"+
                               "   -s : refresh SNAPSHOTs\n"+
                               "   -a : refresh everything\n"+
                               "   -n : No new installs, fail if any new RPM is to be installed\n"+
                               "   -d : No deletions, fail if any RPM is to be deleted\n"+
                               "   -u : No updates, fail if not all RPMs are all new or to be deleted\n"+
                               "   -l username : username\n"+
                               "   -p : Only print changes, don't update\n"+
                               "Repo file format:\n"+
                               "   reponame=url [sequence]\n"+
                               "where optional sequence gives the sequence with which repository will be used to resolve RPMs.\n"+
                               "\n"+
                               "Manifest file format:\n"+
                               "host=url\n"+
                               "import=file,file,...\n"+
                               "package_name=arch:<arch> [epoch:<epoch>] [version:<version>] [release:<release>] [repo:<reponame>] [action:install|erase]\n"+
                               " where:\n"+
                               "  package_name: The RPM package name\n"+
                               "  arch: architecture. Required\n"+
                               "  epoch: Optional. If not given, LATEST assumed\n"+
                               "  version: Optional. If not given, LATEST assumed\n"+
                               "  release: Optional. If not given, LATEST assumed\n"+
                               "  repo: Optional. If given, only this repo is used to resolve the RPM\n"+
                               "  action: Optional. If not given, install is assumed.\n\n"+
                               "Any RPM imported will be overriden by the RPM definitions in the current file.");
        } else {
            List<YumRepo> repositories=new ArrayList<YumRepo>();
            
            ArrayList<String> newArgs=new ArrayList();
            for(String x:args)
                newArgs.add(x);

            String userName=null;
            boolean done=false;
            int index;
            do {
                index=newArgs.indexOf("-r");
                if(index!=-1) {
                    addRepoFile(repositories,newArgs.get(index+1));
                    newArgs.remove(index);
                    newArgs.remove(index);
                } else
                    done=true;
            } while(!done);

            index=newArgs.indexOf("-l");
            if(index!=-1) {
                userName=newArgs.get(index+1);
                newArgs.remove(index);
                newArgs.remove(index);
            }

            options.printOnly=getFlag(newArgs,"-p");
            options.refreshSnapshots=getFlag(newArgs,"-s");
            options.refreshEverything=getFlag(newArgs,"-a");
            options.noNewInstalls=getFlag(newArgs,"-n");
            options.noDeletions=getFlag(newArgs,"-d");
            options.noUpdates=getFlag(newArgs,"-u");

            for(String host:newArgs) {
                processHost(host,userName,repositories,options);
            }
        }
    }

    private static boolean getFlag(List<String> args,String flag) {
        if(args.contains(flag)) {
            args.remove(flag);
            return true;
        } else
            return false;
    }

    private static void processHost(String hostfile,
                                    String userName,
                                    List<YumRepo> repositories,
                                    Options options) throws Exception {
        HostManifest hostManifest=new HostManifest(hostfile);
        RemoteShell rsh=new RemoteShell(hostManifest.getHost(),userName);
        System.out.println("Processing "+hostManifest.getHost());

        RPMRequest[] requests=hostManifest.getRPMs();
        RPMInfo[] resolvedRequests=resolve(requests,repositories);
        Comparator<RPMInfo> rpminfoComparator=new Comparator<RPMInfo>() {
            public int compare(RPMInfo r1,RPMInfo r2) {
                return r1.name.compareTo(r2.name);
            }
        };
        Arrays.sort(resolvedRequests,rpminfoComparator);

        if(resolvedRequests.length>0) {
            System.out.println("RPMs requested on this host:");
            for(RPMInfo x:resolvedRequests)
                System.out.println(x);
        }

        RPM rpm=new RPM(rsh);
        // Get the RPMs on this host
        RPMInfo[] existingRpms=rpm.getRPMInfo(requests);
        Arrays.sort(existingRpms,rpminfoComparator);
        if(existingRpms.length>0) {
            System.out.println("RPMs on this host:");
            for(RPMInfo x:existingRpms)
                System.out.println(x);
        }

        List<RPMInfo> deleteList=new ArrayList<RPMInfo>();
        List<RPMInfo> installList=new ArrayList<RPMInfo>();

        boolean hasInstalls=false;
        boolean hasUpdates=false;
        boolean hasDeletes=false;
        for(RPMInfo requestedRpm:resolvedRequests) {
            RPMInfo hostCopy=null;
            RPMRequest req=null;
            for(RPMRequest k:requests)
                if(k.name.equals(requestedRpm.name)) {
                    req=k;
                    break;
                }
            for(RPMInfo e:existingRpms)
                if(e.name.equals(requestedRpm.name)&&
                   e.arch.equals(requestedRpm.arch)) {
                    hostCopy=e;
                    break;
                }
            // If marked to be deleted, delete it
            if(req.action==RPMRequest.Action.erase) {
                // If exists, delete it
                if(hostCopy!=null) {
                    deleteList.add(hostCopy);
                    System.out.println("Delete "+hostCopy);
                    hasDeletes=true;
                }
            } else {
                // We will install this RPM
                if(hostCopy==null) {
                    // Does not exist, install it
                    installList.add(requestedRpm);
                    System.out.println("Install "+requestedRpm);
                    hasInstalls=true;
                } else {
                    // Exists on the host
                    if(options.refreshEverything||
                       (options.refreshSnapshots&&requestedRpm.version.toUpperCase().indexOf("SNAPSHOT")!=-1)||
                       requestedRpm.betterMatch(hostCopy,req)) {
                        deleteList.add(hostCopy);
                        installList.add(requestedRpm);
                        System.out.println("Update "+requestedRpm.name+"."+requestedRpm.arch+" ["+
                                           hostCopy.epoch+":"+hostCopy.version+"-"+hostCopy.release+" -> "+
                                           requestedRpm.epoch+":"+requestedRpm.version+"-"+requestedRpm.release+"@"+
                                           requestedRpm.repo+"]");
                        hasUpdates=true;
                    } 
                }
            }
        }
        if(hasInstalls&&options.noNewInstalls)
            throw new RuntimeException("There are new RPMs to be installed");
        if(hasDeletes&&options.noDeletions)
            throw new RuntimeException("There are RPMs to be deleted");
        if(hasUpdates&&options.noUpdates)
            throw new RuntimeException("There are RPMs to be updated");

        for(RPMInfo existingRpm:existingRpms) {
            RPMRequest req=null;
            for(RPMRequest k:requests)
                if(k.name.equals(existingRpm.name)) {
                    req=k;
                    break;
                }
            // erase if not in delete list already and is on the host
            if(req.action==RPMRequest.Action.erase) {
                boolean deleted=false;
                for (RPMInfo d:deleteList) {
                    if (d.name.equals(existingRpm.name)) {
                        deleted=true;
                        break;
                    }
                }
                if (!deleted) {
                    deleteList.add(existingRpm);
                    System.out.println("Delete "+existingRpm);
                }
            }
        }

        if(!options.printOnly) {
            // Delete the RPMs that need to be deleted
            if(!deleteList.isEmpty()) {
                String[] packages=new String[deleteList.size()];
                int i=0;
                for(RPMInfo x:deleteList)
                    packages[i++]=x.name;
                rpm.delete(packages);
            }
            
            // Install the RPMs that need to be installed
            if(!installList.isEmpty()) {
                String[] packages=new String[installList.size()];
                int i=0;
                for(RPMInfo x:installList) {
                    YumRepo repo=YumRepo.find(repositories,x.repo);
                    if(repo==null)
                        throw new RuntimeException("Cannot find repo "+x.repo);
                    packages[i++]=repo.repoUrl.toString()+"/"+x.repoDir;
                }
                rpm.install(packages);
            }
        }
    }

    /**
     * Resolve RPM requests to full version and repo info
     */
    private static RPMInfo[] resolve(RPMRequest[] req,List<YumRepo> repositories) throws Exception {
        ArrayList<RPMInfo> ret=new ArrayList<RPMInfo>();
        for(int i=0;i<req.length;i++)
            if(req[i].action==RPMRequest.Action.install)
                ret.add(resolve(req[i],repositories));
        return ret.toArray(new RPMInfo[ret.size()]);
    }

    private static RPMInfo resolve(RPMRequest req,List<YumRepo> repositories) throws Exception {
        List<YumRepo> repos;
        if(req.repo!=null) {
            YumRepo repo=YumRepo.find(repositories,req.repo);
            if(repo==null)
                throw new RuntimeException("Cannot find repo "+req.repo);
            repos=new ArrayList<YumRepo>();
            repos.add(repo);
        } else
            repos=repositories;

        RPMInfo resolved=null;
        for(YumRepo repo:repos) {
            RPMInfo r=repo.resolve(req);
            if(r!=null)
                if(resolved==null||r.betterMatch(resolved,req))
                    resolved=r;
        }
        if(resolved==null)
            throw new RuntimeException("Cannot find "+req);
        return resolved;
    }

    private static void addRepoFile(List<YumRepo> repositories,
                                    String file) throws Exception {
        Properties p=new Properties();
        FileReader f=new FileReader(file);
        p.load(f);
        f.close();
        int seq=1000;
        for(Object x:p.keySet()) {
            String value=p.getProperty((String)x);
            value=value.trim();
            int index=value.indexOf(' ');
            URL url;
            int sequence;
            if(index!=-1) {
                url=new URL(value.substring(0,index));
                sequence=Integer.valueOf(value.substring(index+1)).intValue();
            } else {
                url=new URL(value);
                sequence=seq++;
            }
            YumRepo repo=new YumRepo((String)x,url,sequence);
            repositories.add(repo);
        }
        Collections.sort(repositories,new Comparator<YumRepo> () {
                             public int compare(YumRepo r1,YumRepo r2) {
                                 if(r1.sequence>r2.sequence)
                                     return 1;
                                 else if(r1.sequence<r2.sequence)
                                     return -1;
                                 else
                                     return 0;
                             }
                         });
    }

    
}
