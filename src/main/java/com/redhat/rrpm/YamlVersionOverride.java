package com.redhat.rrpm;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Loads RPM versions from a YAML file, and overrides versions in RPMRequests.
 *
 * @author Kevin Howell (khowell@redhat.com)
 */
public class YamlVersionOverride {

    Map<?,?> overrides;

    public YamlVersionOverride(String path) throws FileNotFoundException {
        Reader file = new FileReader(path);
        Yaml yaml = new Yaml();
        Object object = yaml.load(file);
        if (!(object instanceof Map)) {
            throw new RuntimeException("yaml not in expected format.");
        }
        else {
            overrides = (Map<?,?>) object;
        }
    }

    public RPMRequest[] overrideVersions(RPMRequest[] requests) {
        RPMRequest[] newRequests = new RPMRequest[requests.length];
        for (int i = 0; i < newRequests.length; i++) {
            RPMRequest request = requests[i];
            if (overrides.containsKey(request.name)) {
                String[] override = overrides.get(request.name).toString().split("-");
                String newVersion = override[0];
                String newRelease = null;
                if (override.length > 1) {
                    newRelease = override[1];
                }
                newRequests[i] = new RPMRequest(request, newVersion, newRelease);
            }
            else {
                newRequests[i] = request;
            }
        }
        return newRequests;
    }

}
