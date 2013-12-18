package org.jahia.modules.serversettings.forge;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.bin.Jahia;
import org.jahia.commons.Version;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.notification.HttpClientService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Service to manage Forges
 */

public class ForgeService {

    static Logger logger = LoggerFactory.getLogger(ForgeService.class);

    private HttpClientService httpClientService;
    private Set<Forge> forges = new HashSet<Forge>();
    private List<Module> modules = new ArrayList<Module>();

    public ForgeService() {
        loadForges();
    }

    public Set<Forge> getForges() {
        return forges;
    }

    public List<Module> getModules() {
        return modules;
    }

    public void addForge(Forge forge) {
        for (Forge f : forges) {
            if (StringUtils.equals(forge.getId(), f.getId())) {
                f.setUser(forge.getUser());
                f.setUrl(forge.getUrl());
                f.setPassword(forge.getPassword());
                return;
            }
        }
        forges.add(forge);
    }

    public void removeForge(Forge forge) {
        for (Forge f : forges) {
            if (StringUtils.equals(forge.getId(), f.getId())) {
                forges.remove(f);
                return;
            }
        }
    }

    public void loadForges() {
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                @Override
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    if (session.itemExists("/settings/forgesSettings")) {
                        Node forgesRoot = session.getNode("/settings/forgesSettings");
                        if (forgesRoot != null) {
                            NodeIterator ni = forgesRoot.getNodes();
                            while (ni.hasNext()) {
                                Node n = ni.nextNode();
                                Forge f = new Forge();
                                f.setId(n.getIdentifier());
                                f.setUrl(n.getProperty("j:url").getString());
                                f.setUser(n.getProperty("j:user").getString());
                                f.setPassword(n.getProperty("j:password").getString());
                                forges.add(f);
                            }
                        }
                    }
                    return null;
                }
            });

        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    public void saveForges() {
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                @Override
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {

                    Node forgesRoot;

                    // delete all previous nodes
                    try {
                        forgesRoot = session.getNode("/settings/forgesSettings");
                        forgesRoot.remove();
                    } catch (PathNotFoundException e) {
                        // do nothing
                    }

                    if (!session.getNode("/").hasNode("settings")) {
                        session.getNode("/").addNode("settings", "jnt:globalSettings");
                        session.save();
                    }
                    if (!session.getNode("/settings").hasNode("forgesSettings")) {
                        session.getNode("/settings").addNode("forgesSettings", "jnt:forgesServerSettings");
                        session.save();
                    }

                    forgesRoot = session.getNode("/settings/forgesSettings");
                    // write all forges
                    for (Forge forge : forges) {
                        Node forgeNode = forgesRoot.addNode(JCRContentUtils.generateNodeName(forge.getUrl()), "jnt:forgeServerSettings");
                        forgeNode.setProperty("j:url", forge.getUrl());
                        forgeNode.setProperty("j:user", forge.getUser());
                        forgeNode.setProperty("j:password", forge.getPassword());
                        forge.setId(forgeNode.getIdentifier());
                    }
                    session.save();
                    return null;
                }
            });
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    public Module findModule(String name, String groupId) {
        for (Module m : modules) {
            if (StringUtils.equals(name, m.getId()) && m.getGroupId().equals(groupId)) {
                return m;
            }
        }
        return null;
    }


    public List<Module> loadModules() {
        modules.clear();
        for (Forge forge : forges) {
            String url = forge.getUrl() + "/contents/forge-modules-repository.forgeModuleList.json";
            Map<String, String> headers = new HashMap<String, String>();
            if (!StringUtils.isEmpty(forge.getUser())) {
                headers.put("Authorization", "Basic " + Base64.encode((forge.getUser() + ":" + forge.getPassword()).getBytes()));
            }
            headers.put("accept", "application/json");

            String jsonModuleList = httpClientService.executeGet(url, headers);
            try {
                JSONArray modulesRoot = new JSONArray(jsonModuleList);

                JSONArray moduleList = modulesRoot.getJSONObject(0).getJSONArray("modules");
                for (int i = 0; i < moduleList.length(); i++) {
                    boolean add = true;

                    final JSONObject moduleObject = moduleList.getJSONObject(i);
                    for (Module m : modules) {
                        if (StringUtils.equals(m.getId(), moduleObject.getString("name")) && StringUtils.equals(m.getGroupId(), moduleObject.getString("groupId"))) {
                            add = false;
                            break;
                        }
                    }
                    if (add) {
                        final JSONArray moduleVersions = moduleObject.getJSONArray("versions");

                        SortedMap<Version, JSONObject> sortedVersions = new TreeMap<Version, JSONObject>();

                        final Version jahiaVersion = new Version(Jahia.VERSION);

                        for (int j = 0; j < moduleVersions.length(); j++) {
                            JSONObject object = moduleVersions.getJSONObject(j);
                            Version version = new Version(object.getString("version"));
                            Version requiredVersion = new Version(StringUtils.substringAfter(object.getString("requiredVersion"), "version-"));
                            if (requiredVersion.compareTo(jahiaVersion) <= 0) {
                                sortedVersions.put(version, object);
                            }
                        }
                        if (!sortedVersions.isEmpty()) {
                            Module module = new Module();
                            JSONObject versionObject = sortedVersions.get(sortedVersions.lastKey());
                            module.setRemoteUrl(moduleObject.getString("remoteUrl"));
                            module.setRemotePath(moduleObject.getString("path"));
                            if (moduleObject.has("icon")) {
                                module.setIcon(moduleObject.getString("icon"));
                            }
                            module.setVersion(versionObject.getString("version"));
                            module.setName(moduleObject.getString("title"));
                            module.setId(moduleObject.getString("name"));
                            module.setGroupId(moduleObject.getString("groupId"));
                            module.setDownloadUrl(versionObject.getString("downloadUrl"));
                            module.setForgeId(forge.getId());
                            modules.add(module);
                        }
                    }
                }
            } catch (JSONException e) {
                logger.error("unable to parse JSON return string for " + url);
            }
        }
        Collections.sort(modules);
        return modules;
    }

    public File downloadModuleFromForge(String forgeId, String url) {
        for (Forge forge : forges) {
            if (forgeId.equals(forge.getId())) {
                GetMethod httpMethod = new GetMethod(url);
                httpMethod.addRequestHeader("Authorization", "Basic " + Base64.encode((forge.getUser() + ":" + forge.getPassword()).getBytes()));
                HttpClient httpClient = httpClientService.getHttpClient();
                try {
                    int status = httpClient.executeMethod(httpMethod);
                    if (status == HttpServletResponse.SC_OK) {
                        File f = File.createTempFile("module", "." + StringUtils.substringAfterLast(url, "."));
                        FileUtils.copyInputStreamToFile(httpMethod.getResponseBodyAsStream(), f);
                        return f;
                    }
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }


            }
        }
        return null;
    }

    public void setHttpClientService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }
}
