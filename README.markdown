# rrpm

rrpm maintains RPMs on remote hosts using host manifests defined as
text files. To install/update RPMs on hosts, you simply run:

```
 rrpm -l root -r repositories host-manifest-1 host-manifest-2 host-manifest-3...
```

## Defining Repositories

rrpm uses yum repositories. Define your repositories in a text file,
one repository for each line, using the following format:

```
reponame=url [precedence]
````

 *'reponame' is a short identifier for the repository. It can be used to select an rpm
  from a perticular repository.
 * 'url' is the URL for the yum repository.
 * 'precedence' is optional. Repositories with lower precedence are processed first.
  If an rpm is located in more than one repository, the copy in the lower precedence repository will be used.

Example:

```
production-repo=http://myhost/prodrepo 1
dev-repo=http://myhost/devrepo 2
```

## Host Manifests

A host manifest gives the name of the host, any base manifests, and a list of RPMs:

```
host=hostname
import=file1[,file2...]
rpmname=arch:architecture [epoch:n] [version:ver] [release:rel] [repo:reponame] [action:install|erase]
rpmname=...
```

 * The 'host' decleration give the remote host name.
 
 * The 'import' decleration lists a comma separated list of manifest
   files to import from. Redefinition of an RPM overrides the previous
   definition, so anything redefined in the current host manifest
   overrides the previous definitions. Imported host manifests can
   import from other files. This way, base profiles can be prepared,
   and any changes can be overriden in the individual host files.
 
 * The RPM declerations are the RPMs to be installed or removed
   on/from this host. The following elements are defined:
    * arch: RPM Architecture. This is the only mandatory element.
    * epoch: RPM epoch. If omitted, LATEST is assumed.
    * version: RPM version. If omitted, LATEST is assumed.
    * release: RPM release. If omitted, LATEST is assumed.
    * repo: The repository to resolve the RPM. If omitted, all repositories are scanned
       to find the best matching RPM.
    * action: Install or erase. If omitted, install is assumed.

## Usage

rrpm is run as follows:

```
  rrpm -r repofile [-a] [-s] [-p] hostfile1 hostfile2 ...
```  

The options are:

  -a : Update all RPMs. If omitted, only the missing RPMs will be installed, and RPMs to be deleted will be deleted.

  -s : Update all snapshots. If given, all RPMs containing SNAPSHOT in its version will be reinstalled

  -p : Print what will be updated, but don't update

rrpm uses ssh to remotely run rpm. Passwordless ssh login for all the
hosts is required to prevent asking passwords during execution.

## Using RRPM to maintain development environments

Development environments usually have several systems for different
work streams. Work from different groups are deployed on to separate
sytems for testing. rrpm can be used effectively to maintain such
development environments.
          
As an example, suppose production versions of RPMs are stored in a
production yum repository, and development versions of RPMs are stored
in a development repository. The development RPMs have "SNAPSHOT" in
their versions.

Repositories file:
```
production=http://repositoryhost/production 1
development=http://repositoryhost/development 2
```

With this repository file, RPMs will be first looked up in the
production repository, and if they are not found there, they will be
looked up in the development repository

The 'production' manifest file lists the RPMs deployed to production:
```
module1=arch:noarch version:1.0
module2=arch:noarch version:2.2
module3=arch:noarch version:3.1
```

The production hosts use the 'production' manifest:
```
host=productionhost.company.com
import=production
```

Development hosts use the 'production' manifest as well,
but override the component that are under development:

devhost1:
```
host=devhost1.company.com
import=production

module2=arch:noarch version:2.3.SNAPSHOT
```

With this configuration, rrpm installs version 1.0 of 'module1',
version 2.3.SNAPSHOT of 'module2' and version 3.1 of 'module3' to the
host 'devhost1'. Whenever development group makes builds a new RPM
for 'module3', then publish that RPM to the development repository,
and run rrpm with -s flag to update all snapshots on that host.

Using a similar process, one can create named releases that contain a set of artifacts:

release2:
```
module2=arch:noarch version:2.3.SNAPSHOT<br/>
module3=arch:noarch version:3.2.SNAPSHOT
```

Then, a host can be quickly configured to contain the artifacts for this release:

```
host=devhost1.company.com
import=production,release2
```

With this configuration, all RPMs that are redefined in 'release2'
along with all the RPMs that are in 'production' but that are not
redefined in 'release2' will be installed to the host.

Once all development host files are setup, simply running
```
    rrpm -r repositories -l root -s devhost*
```
will update all the snapshot RPMS on all development hosts.

          
