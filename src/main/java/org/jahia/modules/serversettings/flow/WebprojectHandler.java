/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.modules.serversettings.flow;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.listeners.JahiaContextLoaderListener;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.exceptions.JahiaException;
import org.jahia.modules.serversettings.users.management.UserProperties;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.importexport.ImportExportBaseService;
import org.jahia.services.importexport.NoCloseZipInputStream;
import org.jahia.services.importexport.validation.ValidationResults;
import org.jahia.services.search.spell.CompositeSpellChecker;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.LanguageCodeConverters;
import org.jahia.utils.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.binding.validation.ValidationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.webflow.execution.RequestContext;

/**
 * Handle creation of Web projects in webflow.
 */
public class WebprojectHandler implements Serializable {

    private static final Comparator<ImportInfo> IMPORTS_COMPARATOR = new Comparator<ImportInfo>() {
        public int compare(ImportInfo o1, ImportInfo o2) {
            Integer rank1 = RANK.get(o1.getImportFileName());
            Integer rank2 = RANK.get(o2.getImportFileName());
            rank1 = rank1 != null ? rank1 : 100;
            rank2 = rank2 != null ? rank2 : 100;
            return rank1.compareTo(rank2);
        }
    };

    private static final Pattern LANGUAGE_RANK_PATTERN = Pattern.compile("(?:language.)(\\w+)(?:.rank)");

    static Logger logger = LoggerFactory.getLogger(WebprojectHandler.class);
    private static final HashSet<String> NON_SITE_IMPORTS = new HashSet<String>(Arrays.asList("serverPermissions.xml",
            "users.xml", "users.zip", JahiaSitesService.SYSTEM_SITE_KEY + ".zip", "references.zip", "roles.zip"));
    private static final Map<String, Integer> RANK;

    private static final long serialVersionUID = -6643519526225787438L;

    static {
        RANK = new HashMap<String, Integer>(3);
        RANK.put("roles.xml", 5);
        RANK.put("roles.zip", 5);
        RANK.put("users.zip", 10);
        RANK.put("users.xml", 10);
        RANK.put("serverPermissions.xml", 20);
        RANK.put("shared.zip", 30);
        RANK.put(JahiaSitesService.SYSTEM_SITE_KEY + ".zip", 40);
    }

    @Autowired
    private transient JahiaGroupManagerService groupManagerService;

    @Autowired
    private transient ImportExportBaseService importExportBaseService;

    private transient MultipartFile importFile;
    private String importPath;
    private Properties importProperties;

    private Map<String, ImportInfo> importsInfos;

    private List<File> prepackagedSites;

    private String selectedPrepackagedSite;

    private transient List<JahiaSite> sites;

    private List<String> sitesKey;

    @Autowired
    private transient JahiaSitesService sitesService;

    @Autowired
    private transient JahiaTemplateManagerService templateManagerService;

    @Autowired
    private transient JahiaUserManagerService userManagerService;


    @Autowired
    private transient JCRTemplate template;


    private boolean validityCheckOnImport = true;

    public WebprojectHandler() {
        File[] files = new File(SettingsBean.getInstance().getJahiaVarDiskPath() + "/prepackagedSites").listFiles();
        if (files != null) {
            prepackagedSites = Arrays.asList(files);
        }
    }

    public void createSite(final SiteBean bean) {

        try {
            template.doExecuteWithSystemSession(new JCRCallback<Object>() {
                @Override
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    try {
                        JahiaSite site = sitesService
                                .addSite(JCRSessionFactory.getInstance().getCurrentUser(), bean.getTitle(),
                                        bean.getServerName(), bean.getSiteKey(), bean.getDescription(),
                                        LanguageCodeConverters.getLocaleFromCode(bean.getLanguage()),
                                        bean.getTemplateSet(), bean.getModules().toArray(
                                        new String[bean.getModules().size()]), null, null, null, null, null, null, null,
                                        null, session);

                        // set as default site
                        if (bean.isDefaultSite()) {
                            sitesService.setDefaultSite(site,session);
                            session.save();
                        }

                        if (bean.isCreateAdmin()) {
                            UserProperties admin = bean.getAdminProperties();
                            JahiaUser adminSiteUser = userManagerService.createUser(admin.getUsername(), admin.getPassword(),
                                    admin.getUserProperties());
                            groupManagerService.getAdministratorGroup(site.getSiteKey()).addMember(adminSiteUser);
                        }
                    } catch (JahiaException e) {
                        logger.error(e.getMessage(), e);
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }
            });
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }

    }

    public void deleteSites() {
        if (sitesKey != null) {
            JahiaSite defSite = sitesService.getDefaultSite();
            String siteKey = defSite.getSiteKey();
            for (String site : sitesKey) {
                try {
                    sitesService.removeSite(sitesService.getSiteByKey(site));
                } catch (JahiaException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            if (sitesKey.contains(siteKey)) {
                try {
                    List<JCRSiteNode> sitesNodeList = sitesService.getSitesNodeList();
                    sitesService.setDefaultSite(null);
                    for (JCRSiteNode siteNode : sitesNodeList) {
                        if (!siteNode.getName().equals("systemsite")) {
                            sitesService.setDefaultSite(sitesService.getSiteByKey(siteNode.getName()));
                            break;
                        }
                    }
                } catch (JahiaException e) {
                    logger.error(e.getMessage(), e);
                } catch (RepositoryException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

    }

    private Locale determineDefaultLocale(Locale defaultLocale, ImportInfo infos) {
        SortedMap<Integer, String> activeLanguageCodesByRank = new TreeMap<Integer, String>();
        Map<Object, Object> map = infos.asMap();
        for (Map.Entry<Object, Object> info : map.entrySet()) {
            if (info.getKey() instanceof String) {
                Matcher m = LANGUAGE_RANK_PATTERN.matcher((String) info.getKey());
                if (m.find()) {
                    String languageCode = m.group(1);
                    boolean activated = Boolean.parseBoolean((String) map
                            .get("language." + languageCode + ".activated"));

                    if (activated) {
                        if ("1".equals(info.getValue())) {
                            return LanguageCodeConverters.languageCodeToLocale(languageCode);
                        } else {
                            activeLanguageCodesByRank.put(new Integer((String) info.getValue()), languageCode);
                        }
                    }
                }
            }
        }
        if (!activeLanguageCodesByRank.isEmpty()) {
            defaultLocale = LanguageCodeConverters.languageCodeToLocale(activeLanguageCodesByRank
                    .get(activeLanguageCodesByRank.firstKey()));
        }
        return defaultLocale;
    }

    public void exportSites(RequestContext requestContext) {
        // HttpServletResponse response = (HttpServletResponse) requestContext.getExternalContext().getNativeResponse();
        // HttpServletRequest request = (HttpServletRequest) requestContext.getExternalContext().getNativeRequest();
        // RenderContext renderContext = (RenderContext) request.getAttribute("renderContext");
        // renderContext.setRedirect("/cms/export/default/my_export.zip?exportformat=site&live=true&sitebox=mySite");
        // return;

        // "localhost:8080/cms/export/default/my_export.zip?exportformat=site&live=true&sitebox=mySite"
        // response.reset();
        // response.setContentType("application/zip");
        // response.setContentType("text/plain");
        // make sure this file is not cached by the client (or a proxy middleman)
        // WebUtils.setNoCacheHeaders(response);
        // try {
        // response.getWriter().append("Here is a test text");
        // response.getWriter().flush();
        // } catch (IOException e) {
        // logger.error(e.getMessage(), e);
        // }

        // Map<String,Object> params = new LinkedHashMap<String, Object>();
        // params.put(ImportExportService.INCLUDE_ALL_FILES, Boolean.TRUE);
        // params.put(ImportExportService.INCLUDE_TEMPLATES, Boolean.TRUE);
        // params.put(ImportExportService.INCLUDE_SITE_INFOS, Boolean.TRUE);
        // params.put(ImportExportService.INCLUDE_DEFINITIONS, Boolean.TRUE);
        // if (request.getParameter("live") == null || Boolean.valueOf(request.getParameter("live"))) {
        // params.put(ImportExportService.INCLUDE_LIVE_EXPORT, Boolean.TRUE);
        // }
        // // if (request.getParameter("users") == null || Boolean.valueOf(request.getParameter("users"))) {
        // params.put(ImportExportService.INCLUDE_USERS, Boolean.TRUE);
        // // }
        // params.put(ImportExportService.INCLUDE_ROLES, Boolean.TRUE);
        // params.put(ImportExportService.VIEW_WORKFLOW, Boolean.TRUE);
        // params.put(ImportExportService.XSL_PATH, cleanupXsl);
        //
        // try {
        // OutputStream outputStream = response.getOutputStream();
        // importExportBaseService.exportSites(outputStream, params, sites);
        // outputStream.close();
        // requestContext.getExternalContext().recordResponseComplete();
        // } catch (IOException e) {
        // logger.error(e.getMessage(), e);
        // } catch (RepositoryException e) {
        // logger.error(e.getMessage(), e);
        // } catch (SAXException e) {
        // logger.error(e.getMessage(), e);
        // } catch (TransformerException e) {
        // logger.error(e.getMessage(), e);
        // }
    }

    public MultipartFile getImportFile() {
        return importFile;
    }

    public String getImportPath() {
        return importPath;
    }

    public Map<String, ImportInfo> getImportsInfos() {
        return importsInfos;
    }

    public SiteBean getNewSite() {
        return new SiteBean();
    }

    public List<File> getPrepackagedSites() {
        return prepackagedSites;
    }

    public String getSelectedPrepackagedSite() {
        return selectedPrepackagedSite;
    }

    public SiteBean getSelectedSiteBean() {
        getSites();
        if (sites.isEmpty()) {
            return null;
        }
        JahiaSite site = sites.get(0);

        SiteBean siteBean = new SiteBean();
        siteBean.setDefaultSite(site.isDefault());
        siteBean.setDescription(site.getDescription());
        siteBean.setSiteKey(site.getSiteKey());
        siteBean.setServerName(site.getServerName());
        siteBean.setTitle(site.getTitle());
        siteBean.setTemplatePackageName(site.getTemplatePackageName());
        siteBean.setTemplateFolder(site.getTemplateFolder());
        List<String> installedModules = site.getInstalledModules();
        siteBean.setModules(installedModules.size() > 1 ? new LinkedList<String>(installedModules.subList(1,
                installedModules.size())) : new LinkedList<String>());

        return siteBean;
    }

    public List<JahiaSite> getSites() {
        if(sites==null && sitesKey !=null) {
            setSitesKey(sitesKey);
        }
        return sites;
    }

    private void prepareFileImports(File f, String name, MessageContext messageContext) {
        if (f != null && f.exists()) {
            ZipInputStream zis = null;
            try {
                importProperties = new Properties();
                zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(f)));
                ZipEntry z;
                Map<File, String> imports = new HashMap<File, String>();
                List<File> importList = new ArrayList<File>();
                while ((z = zis.getNextEntry()) != null) {
                    File i = File.createTempFile("import", ".zip");
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(i));
                    try {
                        IOUtils.copy(zis, os);
                    } finally {
                        IOUtils.closeQuietly(os);
                    }

                    String n = z.getName();
                    if (n.equals("export.properties")) {
                        InputStream is = new BufferedInputStream(new FileInputStream(i));
                        try {
                            importProperties.load(is);
                        } finally {
                            IOUtils.closeQuietly(is);
                            FileUtils.deleteQuietly(i);
                        }
                    } else if (n.equals("classes.jar")) {
                        FileUtils.deleteQuietly(i);
                    } else if (n.equals("site.properties") || ((n.startsWith("export_") && n.endsWith(".xml")))) {
                        // this is a single site import, stop everything and import
                        FileUtils.deleteQuietly(i);
                        for (File file : imports.keySet()) {
                            FileUtils.deleteQuietly(file);
                        }
                        imports.clear();
                        importList.clear();
                        File tempFile = File.createTempFile("import", ".zip");
                        FileUtils.copyFile(f, tempFile);
                        imports.put(tempFile, name);
                        importList.add(tempFile);
                        break;
                    } else {
                        imports.put(i, n);
                        importList.add(i);
                    }
                }

                this.importsInfos = new LinkedHashMap<String, ImportInfo>();
                List<ImportInfo> importsInfosList = new ArrayList<ImportInfo>();
                for (File i : importList) {
                    ImportInfo value = prepareSiteImport(i, imports.get(i), messageContext);
                    if (value != null) {
                        if (value.isLegacyImport()) {
                            final String defaultMappingsFolderPath = JahiaContextLoaderListener.getServletContext()
                                    .getRealPath("/WEB-INF/var/legacyMappings");
                            final File defaultMappingsFolder = defaultMappingsFolderPath != null ? new File(
                                    defaultMappingsFolderPath) : null;
                            Collection<File> legacyMappings = null;
                            Collection<File> legacyDefinitions = null;
                            if (defaultMappingsFolder != null && defaultMappingsFolder.exists()) {
                                try {
                                    legacyMappings = FileUtils.listFiles(defaultMappingsFolder, new String[] { "map" },
                                            false);
                                } catch (Exception e) {
                                    logger.debug("Legacy mappings not found", e);
                                }
                                try {
                                    legacyDefinitions = FileUtils.listFiles(defaultMappingsFolder,
                                            new String[] { "cnd" }, false);
                                } catch (Exception e) {
                                    logger.debug("Legacy definitions not found", e);
                                }
                            }

                            Resource[] modulesLegacyMappings = SpringContextSingleton.getInstance().getResources(
                                    "/modules/**/META-INF/legacyMappings/*.map");
                            if (legacyMappings == null && modulesLegacyMappings.length > 0) {
                                legacyMappings = new ArrayList<File>();
                            }
                            for (Resource modulesLegacyMapping : modulesLegacyMappings) {
                                legacyMappings.add(modulesLegacyMapping.getFile());
                            }

                            Resource[] modulesLegacyDefinitions = SpringContextSingleton.getInstance().getResources(
                                    "/modules/**/META-INF/legacyMappings/*.cnd");
                            if (legacyDefinitions == null && modulesLegacyDefinitions.length > 0) {
                                legacyDefinitions = new ArrayList<File>();
                            }
                            for (Resource modulesLegacyDefinition : modulesLegacyDefinitions) {
                                legacyDefinitions.add(modulesLegacyDefinition.getFile());
                            }

                            if (legacyMappings != null && !legacyMappings.isEmpty()) {
                                value.setLegacyMappings(legacyMappings);
                            }
                            if (legacyDefinitions != null && !legacyDefinitions.isEmpty()) {
                                value.setLegacyDefinitions(legacyDefinitions);
                            }
                        }
                        importsInfosList.add(value);
                    }
                }

                Collections.sort(importsInfosList, IMPORTS_COMPARATOR);

                for (ImportInfo info : importsInfosList) {
                    importsInfos.put(info.getImportFileName(), info);
                }

            } catch (IOException e) {
                logger.error("Cannot read import file :" + e.getMessage());
            } finally {
                IOUtils.closeQuietly(zis);
            }
        }
    }

    public void prepareImport(MessageContext messageContext) {
        if (!StringUtils.isEmpty(importPath)) {
            File f = new File(importPath);

            if (f.exists()) {
                prepareFileImports(f, f.getName(), messageContext);
            }
        } else if (!importFile.isEmpty()) {
            File file = null;
            try {
                file = File.createTempFile(importFile.getOriginalFilename(), ".tmp");
                importFile.transferTo(file);
                prepareFileImports(file, importFile.getName(), messageContext);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } finally {
                FileUtils.deleteQuietly(file);
            }
        }
    }

    public void preparePrepackageImport(MessageContext messageContext) {
        if (!StringUtils.isEmpty(selectedPrepackagedSite)) {
            File f = new File(selectedPrepackagedSite);

            if (f.exists()) {
                prepareFileImports(f, f.getName(), messageContext);
            }
        }
    }

    private ImportInfo prepareSiteImport(File i, String filename, MessageContext messageContext) throws IOException {
        ImportInfo importInfos = new ImportInfo();
        importInfos.setImportFile(i);
        importInfos.setImportFileName(filename);
        importInfos.setSelected(Boolean.TRUE);
        if (importProperties != null) {
            importInfos.setOriginatingJahiaRelease(importProperties.getProperty("JahiaRelease"));
        }
        if (filename.endsWith(".xml")) {
            importInfos.setType("xml");
        } else if (filename.endsWith("systemsite.zip")) {
            importInfos.setType("files");
        } else {
            List<String> installedModules = readInstalledModules(i);
            org.jahia.utils.zip.ZipEntry z;
            NoCloseZipInputStream zis2 = new NoCloseZipInputStream(new BufferedInputStream(new FileInputStream(i)));

            boolean isSite = false;
            boolean isLegacySite = false;
            if ("6.1".equals(importInfos.getOriginatingJahiaRelease())) isLegacySite = true;
            try {
                while ((z = zis2.getNextEntry()) != null) {
                    if ("site.properties".equals(z.getName())) {
                        Properties p = new Properties();
                        p.load(zis2);
                        importInfos.loadSiteProperties(p);

                        importInfos.setTemplates("");
                        if (p.containsKey("templatePackageName")) {
                            JahiaTemplatesPackage pack = templateManagerService.getTemplatePackageById((String) p
                                    .get("templatePackageName"));
                            if (pack == null) {
                                pack = templateManagerService.getTemplatePackage((String) p.get("templatePackageName"));
                            }
                            if (pack != null) {
                                importInfos.setTemplates(pack.getId());
                            }
                        }
                        importInfos.setOldSiteKey(importInfos.getSiteKey());
                        isSite = true;
                    } else if (z.getName().startsWith("export_")) {
                        isLegacySite = true;
                    } else if (validityCheckOnImport && !isLegacySite && z.getName().contains("repository.xml")) {
                        try {
                            long timer = System.currentTimeMillis();
                            ValidationResults validationResults = importExportBaseService.validateImportFile(
                                    JCRSessionFactory.getInstance().getCurrentUserSession(), zis2, "application/xml",
                                    installedModules);
                            if (!validationResults.isSuccessful()) {
                                logger.error(
                                        "Failed Import {}/{} validated in {} ms: {}",
                                        new String[] { filename, z.getName(),
                                                String.valueOf((System.currentTimeMillis() - timer)),
                                                validationResults.toString() });
                            } else {
                                logger.info("Successful Import {}/{} validated in {} ms: {}", new String[] { filename,
                                        z.getName(), String.valueOf((System.currentTimeMillis() - timer)),
                                        validationResults.toString() });
                            }
                            if (!validationResults.isSuccessful()) {
                                if (importInfos.getValidationResult() != null) {
                                    // merge results
                                    importInfos.setValidationResult(importInfos.getValidationResult().merge(
                                            validationResults));
                                } else {
                                    importInfos.setValidationResult(validationResults);
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error when validating import file", e);
                        }
                    }
                    zis2.closeEntry();
                }
            } finally {
                zis2.reallyClose();
            }
            importInfos.setSite(isSite);
            // todo import ga parameters
            if (isSite || isLegacySite) {
                importInfos.setType("site");
                importInfos.setLegacyImport(isLegacySite);
                if (importInfos.getSiteKey() == null) {
                    importInfos.setSiteKey("");
                    importInfos.setSiteServername("");
                    importInfos.setSiteTitle("");
                    importInfos.setDescription("");
                    importInfos.setMixLanguage(Boolean.FALSE);
                    importInfos.setTemplates("");
                } else {
                    validateSite(messageContext, importInfos);
                }
            } else {
                importInfos.setType("files");
            }

        }
        return importInfos;
    }

    public boolean processImport(final JahiaUser user, MessageContext context) {
        logger.info("Processing Import");

        boolean doImportServerPermissions = false;
        for (ImportInfo infos : importsInfos.values()) {
            if (infos.isSelected() && infos.getImportFileName().equals("serverPermissions.xml")) {
                doImportServerPermissions = true;
                break;
            }
        }

        for (ImportInfo infos : importsInfos.values()) {
            File file = infos.getImportFile();
            if (infos.isSelected() && infos.getImportFileName().equals("users.xml")) {
                try {
                    importExportBaseService.importUsers(file);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    FileUtils.deleteQuietly(file);
                }
                break;
            }
        }

        try {
            for (final ImportInfo infos : importsInfos.values()) {
                final File file = infos.getImportFile();
                if (infos.isSelected()) {
                    String type = infos.getType();
                    if (type.equals("files")) {
                        try {
                            final JahiaSite system = sitesService.getSiteByKey(JahiaSitesService.SYSTEM_SITE_KEY);

                            Map<String, String> pathMapping = JCRSessionFactory.getInstance().getCurrentUserSession()
                                    .getPathMapping();
                            pathMapping.put("/shared/files/", "/sites/" + system.getSiteKey() + "/files/");
                            pathMapping.put("/shared/mashups/", "/sites/" + system.getSiteKey() + "/portlets/");
                            JCRTemplate.getInstance().doExecuteWithSystemSession(user.getUsername(), new JCRCallback<Object>() {
                                @Override
                                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                                    try {
                                        importExportBaseService.importSiteZip(file == null ? null : new FileSystemResource(file),
                                                system, infos.asMap(),null,null,session);
                                    } catch (Exception e) {
                                        logger.error("Error when getting templates", e);
                                    }
                                    return null;
                                }
                            });
                        } catch (Exception e) {
                            logger.error("Error when getting templates", e);
                        }
                    } else if (type.equals("xml")
                            && (infos.getImportFileName().equals("serverPermissions.xml") || infos.getImportFileName()
                            .equals("users.xml"))) {

                    } else if (type.equals("site")) {
                        // site import
                        String tpl = infos.getTemplates();
                        if ("".equals(tpl)) {
                            tpl = null;
                        }
                        String legacyImportFilePath = null;
                        String legacyDefinitionsFilePath = null;
                        if (infos.isLegacyImport()) {
                            legacyImportFilePath = infos.getSelectedLegacyMapping();
                            if (legacyImportFilePath != null && "".equals(legacyImportFilePath.trim())) {
                                legacyImportFilePath = null;
                            }
                            legacyDefinitionsFilePath = infos.getSelectedLegacyDefinitions();
                            if (legacyDefinitionsFilePath != null && "".equals(legacyDefinitionsFilePath.trim())) {
                                legacyDefinitionsFilePath = null;
                            }
                        }
                        final Locale defaultLocale = determineDefaultLocale(LocaleContextHolder.getLocale(), infos);
                        try {
                            try {
                                final String finalTpl = tpl;
                                final String finalLegacyImportFilePath = legacyImportFilePath;
                                final String finalLegacyDefinitionsFilePath = legacyDefinitionsFilePath;

                                final boolean finalDoImportServerPermissions = doImportServerPermissions;
                                JCRObservationManager.doWithOperationType(null, JCRObservationManager.IMPORT,
                                        new JCRCallback<Object>() {
                                            public Object doInJCR(JCRSessionWrapper jcrSession)
                                                    throws RepositoryException {
                                                try {
                                                    sitesService.addSite(user, infos.getSiteTitle(), infos
                                                            .getSiteServername(), infos.getSiteKey(), "",
                                                            defaultLocale, finalTpl, null, "fileImport",
                                                            file == null ? null : new FileSystemResource(file), infos
                                                            .getImportFileName(), false,
                                                            finalDoImportServerPermissions, infos
                                                            .getOriginatingJahiaRelease(),
                                                            finalLegacyImportFilePath, finalLegacyDefinitionsFilePath);
                                                } catch (JahiaException e) {
                                                    throw new RepositoryException(e);
                                                } catch (IOException e) {
                                                    throw new RepositoryException(e);
                                                }
                                                return null;
                                            }
                                        });
                            } catch (RepositoryException e) {
                                if (e.getCause() != null
                                        && (e.getCause() instanceof JahiaException || e.getCause() instanceof IOException)) {
                                    throw (Exception) e.getCause();
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Cannot create site " + infos.getSiteTitle(), e);
                            context.addMessage(new MessageBuilder()
                                    .error()
                                    .defaultText(
                                            "Cannot create site " + infos.getSiteTitle() + ".<br/>" + e.getMessage())
                                    .build());
                            return false;
                        }
                    }
                }
            }
        } finally {
            for (ImportInfo infos : importsInfos.values()) {
                FileUtils.deleteQuietly(infos.getImportFile());
            }
        }

        CompositeSpellChecker.updateSpellCheckerIndex();

        return true;
    }

    private List<String> readInstalledModules(File i) throws IOException {
        List<String> modules = new LinkedList<String>();
        org.jahia.utils.zip.ZipEntry z;
        NoCloseZipInputStream zis2 = new NoCloseZipInputStream(new BufferedInputStream(new FileInputStream(i)));

        try {
            while ((z = zis2.getNextEntry()) != null) {
                try {
                    if ("site.properties".equals(z.getName())) {
                        Properties p = new Properties();
                        p.load(zis2);
                        Map<Integer, String> im = new TreeMap<Integer, String>();
                        for (Object k : p.keySet()) {
                            String key = String.valueOf(k);
                            if (key.startsWith("installedModules.")) {
                                try {
                                    im.put(Integer.valueOf(StringUtils.substringAfter(key, ".")), p.getProperty(key));
                                } catch (NumberFormatException e) {
                                    logger.warn("Unable to parse installed module from key {}", key);
                                }
                            }
                        }
                        modules.addAll(im.values());
                    }
                } finally {
                    zis2.closeEntry();
                }
            }
        } finally {
            zis2.reallyClose();
        }
        return modules;
    }

    public void setGroupManagerService(JahiaGroupManagerService groupManagerService) {
        this.groupManagerService = groupManagerService;
    }

    public void setImportFile(MultipartFile importFile) {
        this.importFile = importFile;
    }

    public void setImportPath(String importPath) {
        this.importPath = importPath;
    }

    public void setImportsInfos(Map<String, ImportInfo> importsInfos) {
        this.importsInfos = importsInfos;
    }

    public void setSelectedPrepackagedSite(String selectedPrepackagedSite) {
        this.selectedPrepackagedSite = selectedPrepackagedSite;
    }

    public void setSitesKey(List<String> sites) {
        this.sites = new ArrayList<JahiaSite>();
        this.sitesKey = sites;
        if (sites != null) {
            for (String site : sites) {
                try {
                    this.sites.add(sitesService.getSiteByKey(site));
                } catch (JahiaException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    public void setSitesService(JahiaSitesService sitesService) {
        this.sitesService = sitesService;
    }

    public void setUserManagerService(JahiaUserManagerService userManagerService) {
        this.userManagerService = userManagerService;
    }

    public void updateSite(SiteBean bean, MessageContext messages) {
        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
            JahiaSite site = sitesService.getSiteByKey(getSites().get(0).getSiteKey(), session);
            if (!StringUtils.equals(site.getServerName(), bean.getServerName())
                    || !StringUtils.equals(site.getTitle(), bean.getTitle())
                    || !StringUtils.equals(site.getDescription(), bean.getDescription())) {
                site.setServerName(bean.getServerName());
                site.setTitle(bean.getTitle());
                site.setDescription(bean.getDescription());

                sitesService.updateSystemSitePermissions(site, session);
            }

            if (!site.isDefault() && bean.isDefaultSite()) {
                sitesService.setDefaultSite(site);
            }
            session.save();
            messages.addMessage(new MessageBuilder().info().code("label.changeSaved").build());
        } catch (Exception e) {
            messages.addMessage(new MessageBuilder().error()
                    .code("serverSettings.manageWebProjects.webProject.error")
                    .arg(e.getMessage()).build());
            logger.error(e.getMessage(), e);
        }
    }

    public void validateDisplayImportContent(ValidationContext context) {
        context.getMessageContext();

        for (ImportInfo infos : importsInfos.values()) {
            if (infos.isSelected()) {
                if (NON_SITE_IMPORTS.contains(infos.getImportFileName())) {

                } else {
                    validateSite(context.getMessageContext(), infos);
                }
            }
        }

    }

    public void validateView(ValidationContext context) {
        if(context.getUserEvent().equals("import")) {
            MessageContext messages = context.getMessageContext();
            if(StringUtils.isEmpty(getImportPath()) && getImportFile().isEmpty()) {
                messages.addMessage(new MessageBuilder().error().source("importPath").
                        code("serverSettings.manageWebProjects.fileImport.error").build());
            }
        }
    }

    private void validateSite(MessageContext messageContext, ImportInfo infos) {
        try {
            infos.setSiteTitleInvalid(StringUtils.isEmpty((String) infos.getSiteTitle()));

            String siteKey = (String) infos.getSiteKey();
            if (infos.isSite()) {
                boolean valid = sitesService.isSiteKeyValid(siteKey);
                if (!valid) {
                    messageContext.addMessage(new MessageBuilder()
                            .error()
                            .source("siteKey")
                            .code("serverSettings.manageWebProjects.invalidSiteKey").build());
                }
                if (valid && sitesService.getSiteByKey(siteKey) != null) {
                    messageContext.addMessage(new MessageBuilder()
                            .error()
                            .source("siteKey")
                            .code("serverSettings.manageWebProjects.siteKeyExists").build());
                }

                String serverName = infos.getSiteServername();
                if (infos.isLegacyImport() && (StringUtils.startsWithIgnoreCase(serverName, "http://") || StringUtils.startsWithIgnoreCase(serverName, "https://"))) {
                    serverName = StringUtils.substringAfter(serverName, "://");
                    infos.setSiteServername(serverName);
                }
                valid = sitesService.isServerNameValid(serverName);
                if (!valid) {
                    messageContext.addMessage(new MessageBuilder()
                            .error()
                            .source("siteKey")
                            .code("serverSettings.manageWebProjects.invalidServerName").build());
                }

                if (valid && !Url.isLocalhost(serverName) && sitesService.getSite(serverName) != null) {
                    messageContext.addMessage(new MessageBuilder()
                            .error()
                            .source("siteKey")
                            .code("serverSettings.manageWebProjects.serverNameExists").build());
                }
            }
        } catch (JahiaException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Returns the ID of the default template set.
     *
     * @return the ID of the default template set
     */
    public String getDefaultTemplateSetId() {
        String id = null;
        String defTemplateSet = StringUtils.defaultIfBlank(
                SettingsBean.getInstance().lookupString("default_templates_set"), StringUtils.EMPTY).trim();
        if (defTemplateSet.length() > 0) {
            JahiaTemplatesPackage pkg = templateManagerService.getTemplatePackage(defTemplateSet);
            if (pkg == null) {
                pkg = templateManagerService.getTemplatePackageById(defTemplateSet);
            }
            if (pkg == null) {
                logger.warn("Unable to find default template set \"{}\","
                        + " specified via default_templates_set (jahia.properties)");
            } else {
                id = pkg.getId();
            }
        }
        return id;
    }

    /**
     * Returns the total number of sites in Jahia.
     *
     * @return the total number of sites in Jahia
     * @throws JahiaException
     *             in case of an error
     */
    public int getNumberOfSites() throws JahiaException {
        return sitesService.getNbSites();
    }

}
