/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.serversettings.flow;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.commons.Version;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.exceptions.JahiaException;
import org.jahia.modules.sitesettings.users.management.UserProperties;
import org.jahia.osgi.BundleResource;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRObservationManager;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.importexport.ImportExportBaseService;
import org.jahia.services.importexport.ImportExportService;
import org.jahia.services.importexport.ImportUpdateService;
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
import org.jahia.utils.i18n.Messages;
import org.jahia.utils.zip.DirectoryZipInputStream;
import org.osgi.framework.Bundle;
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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

/**
 * Handle creation of Web projects in webflow.
 */
public class WebprojectHandler implements Serializable {

    private static final Comparator<ImportInfo> IMPORTS_COMPARATOR =
                    new Comparator<ImportInfo>() {
                        public int compare(ImportInfo o1, ImportInfo o2) {
                            Integer rank1 = RANK.get(o1.getImportFileName());
                            Integer rank2 = RANK.get(o2.getImportFileName());
                            rank1 = rank1 != null ? rank1 : 100;
                            rank2 = rank2 != null ? rank2 : 100;
                            return rank1.compareTo(rank2);
                        }
                    };

    private static final Pattern LANGUAGE_RANK_PATTERN =
                    Pattern.compile("(?:language.)(\\w+)(?:.rank)");

    static Logger logger = LoggerFactory.getLogger(WebprojectHandler.class);
    private static final HashSet<String> NON_SITE_IMPORTS =
                    new HashSet<>(Arrays.asList("serverPermissions.xml",
                                    "users.xml", "users.zip",
                                    JahiaSitesService.SYSTEM_SITE_KEY + ".zip",
                                    "references.zip", "roles.zip",
                                    "mounts.zip"));
    private static final Map<String, Integer> RANK;

    private static final long serialVersionUID = -6643519526225787438L;

    static {
        RANK = new HashMap<>(8);
        RANK.put("mounts.zip", 4);
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

    private Map<String, ImportInfo> importsInfos = Collections.emptyMap();
    private boolean deleteFilesAtEnd = true;

    private Map<String, String> prepackagedSites =
                    new HashMap<String, String>();

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
        prepackagedSites = new HashMap<String, String>();
        File[] files = new File(SettingsBean.getInstance().getJahiaVarDiskPath()
                        + "/prepackagedSites").listFiles();
        if (files != null) {
            for (File file : files) {
                prepackagedSites.put(file.getAbsolutePath(),
                                Messages.get("resources.JahiaServerSettings",
                                                "serverSettings.manageWebProjects.importprepackaged."
                                                                + file.getName(),
                                                LocaleContextHolder.getLocale(),
                                                file.getName()));
            }
        }
        for (final JahiaTemplatesPackage aPackage : ServicesRegistry
                        .getInstance().getJahiaTemplateManagerService()
                        .getAvailableTemplatePackages()) {
            final Bundle bundle = aPackage.getBundle();
            if (bundle != null) {
                final Enumeration<URL> resourceEnum = bundle.findEntries(
                                "META-INF/prepackagedSites/", "*", false);
                if (resourceEnum == null)
                    continue;
                while (resourceEnum.hasMoreElements()) {
                    BundleResource bundleResource = new BundleResource(
                                    resourceEnum.nextElement(), bundle);
                    try {
                        String title = bundleResource.getFilename();
                        try {
                            String titleKey =
                                            "serverSettings.manageWebProjects.importprepackaged."
                                                            + bundleResource.getFilename();
                            title = Messages.get(aPackage, titleKey,
                                            LocaleContextHolder.getLocale(),
                                            bundleResource.getFilename());
                        } catch (MissingResourceException e) {
                            logger.warn("unable to get resource key "
                                            + "serverSettings.manageWebProjects.importprepackaged."
                                            + bundleResource.getFilename()
                                            + " in package"
                                            + bundle.getSymbolicName());
                        }
                        prepackagedSites.put(
                                        bundleResource.getURI().toString() + "#"
                                                        + bundle.getSymbolicName(),
                                        title);
                    } catch (IOException e) {
                        logger.warn("unable to read prepackaged site "
                                        + bundleResource.getFilename());
                    }
                }
            }
        }
    }

    public List<JCRSiteNode> getAllSites() {
        try {
            Function<JCRSiteNode, String> getTitle =
                            new Function<JCRSiteNode, String>() {
                                public String apply(
                                                @Nullable JCRSiteNode input) {
                                    return input != null ? input.getTitle()
                                                    : "";
                                }
                            };
            Predicate<JCRSiteNode> notSystemSite =
                            new Predicate<JCRSiteNode>() {
                                @Override
                                public boolean apply(JCRSiteNode jcrSiteNode) {
                                    return !jcrSiteNode.getName()
                                                    .equals("systemsite");
                                }
                            };
            return Ordering.natural().onResultOf(getTitle)
                            .sortedCopy(Iterables.filter(
                                            sitesService.getSitesNodeList(),
                                            notSystemSite));
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    public void createSite(final SiteBean bean) {

        try {
            template.doExecuteWithSystemSession(new JCRCallback<Object>() {
                @Override
                public Object doInJCR(JCRSessionWrapper session)
                                throws RepositoryException {
                    try {
                        JahiaSite site = sitesService.addSite(
                                        JCRSessionFactory.getInstance()
                                                        .getCurrentUser(),
                                        bean.getTitle(), bean.getServerName(),
                                        bean.getSiteKey(),
                                        bean.getDescription(),
                                        LanguageCodeConverters
                                                        .getLocaleFromCode(
                                                                        bean.getLanguage()),
                                        bean.getTemplateSet(),
                                        bean.getModules()
                                                        .toArray(new String[bean
                                                                        .getModules()
                                                                        .size()]),
                                        null, null, null, null, null, null,
                                        null, null, session);

                        // set as default site
                        if (bean.isDefaultSite()) {
                            sitesService.setDefaultSite(site, session);
                            session.save();
                        }

                        if (bean.isCreateAdmin()) {
                            UserProperties admin = bean.getAdminProperties();
                            JCRUserNode adminSiteUser = userManagerService
                                            .createUser(admin.getUsername(),
                                                            admin.getPassword(),
                                                            admin.getUserProperties(),
                                                            session);
                            groupManagerService.getAdministratorGroup(
                                            site.getSiteKey(), session)
                                            .addMember(adminSiteUser);
                            session.save();
                        }
                    } catch (JahiaException | IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                    return null; // To change body of implemented methods use File | Settings | File
                                 // Templates.
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
                    List<JCRSiteNode> sitesNodeList =
                                    sitesService.getSitesNodeList();
                    sitesService.setDefaultSite(null);
                    for (JCRSiteNode siteNode : sitesNodeList) {
                        if (!siteNode.getName().equals("systemsite")) {
                            sitesService.setDefaultSite(sitesService
                                            .getSiteByKey(siteNode.getName()));
                            break;
                        }
                    }
                } catch (JahiaException | RepositoryException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

    }

    private Locale determineDefaultLocale(Locale defaultLocale,
                    ImportInfo infos) {
        SortedMap<Integer, String> activeLanguageCodesByRank = new TreeMap<>();
        Map<Object, Object> map = infos.asMap();
        for (Map.Entry<Object, Object> info : map.entrySet()) {
            if (info.getKey() instanceof String) {
                Matcher m = LANGUAGE_RANK_PATTERN
                                .matcher((String) info.getKey());
                if (m.find()) {
                    String languageCode = m.group(1);
                    boolean activated = Boolean.parseBoolean((String) map.get(
                                    "language." + languageCode + ".activated"));

                    if (activated) {
                        if ("1".equals(info.getValue())) {
                            return LanguageCodeConverters
                                            .languageCodeToLocale(languageCode);
                        } else {
                            activeLanguageCodesByRank.put(
                                            new Integer((String) info
                                                            .getValue()),
                                            languageCode);
                        }
                    }
                }
            }
        }
        if (!activeLanguageCodesByRank.isEmpty()) {
            defaultLocale = LanguageCodeConverters
                            .languageCodeToLocale(activeLanguageCodesByRank
                                            .get(activeLanguageCodesByRank
                                                            .firstKey()));
        }
        return defaultLocale;
    }

    public void exportToFile(RequestContext requestContext, boolean staging)
                    throws Exception {
        String exportPath =
                        requestContext.getRequestParameters().get("exportPath");
        if (StringUtils.isEmpty(exportPath)) {
            return;
        }
        Map<String, Object> params = new HashMap<String, Object>(6);

        params.put(ImportExportService.VIEW_CONTENT, true);
        params.put(ImportExportService.VIEW_VERSION, false);
        params.put(ImportExportService.VIEW_ACL, true);
        params.put(ImportExportService.VIEW_METADATA, true);
        params.put(ImportExportService.VIEW_JAHIALINKS, true);
        params.put(ImportExportService.VIEW_WORKFLOW, true);
        params.put(ImportExportService.SERVER_DIRECTORY, exportPath);
        params.put(ImportExportService.INCLUDE_ALL_FILES, true);
        params.put(ImportExportService.INCLUDE_TEMPLATES, true);
        params.put(ImportExportService.INCLUDE_SITE_INFOS, true);
        params.put(ImportExportService.INCLUDE_DEFINITIONS, true);
        params.put(ImportExportService.INCLUDE_LIVE_EXPORT, !staging);
        params.put(ImportExportService.INCLUDE_USERS, true);
        params.put(ImportExportService.INCLUDE_ROLES, true);
        String cleanupXsl = ((ServletContext) requestContext
                        .getExternalContext().getNativeContext()).getRealPath(
                                        "/WEB-INF/etc/repository/export/cleanup.xsl");
        params.put(ImportExportService.XSL_PATH, cleanupXsl);

        List<JCRSiteNode> sites = new ArrayList<JCRSiteNode>();
        String[] sitekeys = requestContext.getRequestParameters()
                        .getArray("sitesKey");
        if (sitekeys != null) {
            for (String sitekey : sitekeys) {
                JahiaSite site = ServicesRegistry.getInstance()
                                .getJahiaSitesService().getSiteByKey(sitekey);
                sites.add((JCRSiteNode) site);
            }
        }

        importExportBaseService.exportSites(new ByteArrayOutputStream(), params,
                        sites);
    }

    public void exportSites(RequestContext requestContext) {
        // HttpServletResponse response = (HttpServletResponse)
        // requestContext.getExternalContext().getNativeResponse();
        // HttpServletRequest request = (HttpServletRequest)
        // requestContext.getExternalContext().getNativeRequest();
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
        // if (request.getParameter("live") == null ||
        // Boolean.valueOf(request.getParameter("live"))) {
        // params.put(ImportExportService.INCLUDE_LIVE_EXPORT, Boolean.TRUE);
        // }
        // // if (request.getParameter("users") == null ||
        // Boolean.valueOf(request.getParameter("users"))) {
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

    public Map<String, String> getPrepackagedSites() {
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
        siteBean.setModules(installedModules.size() > 1
                        ? new LinkedList<>(installedModules.subList(1,
                                        installedModules.size()))
                        : new LinkedList<String>());

        return siteBean;
    }

    public List<JahiaSite> getSites() {
        if (sites == null && sitesKey != null) {
            setSitesKey(sitesKey);
        }
        return sites;
    }

    private void prepareFileImports(File f, String name,
                    MessageContext messageContext) {
        ZipInputStream zis = null;
        if (f != null && f.exists()) {
            ZipEntry z = null;
            try {
                if (f != null && f.isDirectory()) {
                    zis = new DirectoryZipInputStream(f);
                } else {
                    zis = new ZipInputStream(new BufferedInputStream(
                                    new FileInputStream(f)));
                }
                prepareFileImports(zis, name, messageContext);
            } catch (FileNotFoundException e) {
                logger.error("Cannot read import file :" + e.getMessage());
            } finally {
                IOUtils.closeQuietly(zis);
            }
        }
    }

    private void prepareFileImports(ZipInputStream zis, String name,
                    MessageContext messageContext) {
        try {
            ZipEntry z;
            importProperties = new Properties();
            Map<File, String> imports = new HashMap<>();
            List<File> importList = new ArrayList<>();
            List<String> emptyFiles = new ArrayList<>();
            deleteFilesAtEnd = !(zis instanceof DirectoryZipInputStream);

            while ((z = zis.getNextEntry()) != null) {
                String n = z.getName();

                File i;
                if (!(zis instanceof DirectoryZipInputStream)) {
                    i = File.createTempFile("import", ".zip");
                    OutputStream os = new BufferedOutputStream(
                                    new FileOutputStream(i));
                    try {
                        final int numberOfBytesCopied = IOUtils.copy(zis, os);
                        if (numberOfBytesCopied == 0) {
                            emptyFiles.add(n);
                        }
                    } finally {
                        IOUtils.closeQuietly(os);
                    }
                } else {
                    DirectoryZipInputStream directoryZipInputStream =
                                    (DirectoryZipInputStream) zis;
                    if (n.indexOf('/') > -1
                                    && n.indexOf('/') != n.length() - 1) {
                        continue;
                    }
                    i = new File(directoryZipInputStream.getSourceDirectory(),
                                    n);
                    if (z.isDirectory()) {
                        n = n.replace("/", ".zip");
                    }
                }

                if (!emptyFiles.isEmpty()) {
                    // we've detected empty files, issue an error message and exit
                    messageContext.addMessage(new MessageBuilder().error()
                                    .code("serverSettings.manageWebProjects.import.emptyFiles")
                                    .arg(emptyFiles).build());
                    return;
                }

                if (n.equals("export.properties")) {
                    InputStream is = new BufferedInputStream(
                                    new FileInputStream(i));
                    try {
                        importProperties.load(is);
                    } finally {
                        IOUtils.closeQuietly(is);
                        if (deleteFilesAtEnd) {
                            FileUtils.deleteQuietly(i);
                        }
                    }
                } else if (n.equals("classes.jar")) {
                    if (deleteFilesAtEnd) {
                        FileUtils.deleteQuietly(i);
                    }
                } else if (n.equals("site.properties")
                                || ((n.startsWith("export_")
                                                && n.endsWith(".xml")))) {
                    // this is a single site import, stop everything and import
                    if (deleteFilesAtEnd) {
                        FileUtils.deleteQuietly(i);
                        for (File file : imports.keySet()) {
                            FileUtils.deleteQuietly(file);
                        }
                    }
                    imports.clear();
                    importList.clear();
                    File tempFile = File.createTempFile("import", ".zip");
                    FileUtils.copyInputStreamToFile(zis, tempFile);
                    imports.put(tempFile, name);
                    importList.add(tempFile);
                    break;
                } else {
                    imports.put(i, n);
                    importList.add(i);
                }
            }

            this.importsInfos = new LinkedHashMap<>();
            List<ImportInfo> importsInfosList = new ArrayList<>();
            for (File i : importList) {
                ImportInfo value = prepareSiteImport(i, imports.get(i),
                                messageContext);
                if (value != null) {
                    if (value.isLegacyImport()) {
                        Map<String, Resource> legacyMappings =
                                        getLegacyMappingsInModules("map");
                        Map<String, Resource> legacyDefinitions =
                                        getLegacyMappingsInModules("cnd");

                        if (!legacyMappings.isEmpty()) {
                            value.setLegacyMappings(new HashSet<>(
                                            legacyMappings.keySet()));
                        }
                        if (!legacyDefinitions.isEmpty()) {
                            value.setLegacyDefinitions(new HashSet<>(
                                            legacyDefinitions.keySet()));
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
        }
    }

    public static Map<String, Resource> getLegacyMappingsInModules(
                    final String pattern) {
        File fld = new File(SettingsBean.getInstance().getJahiaVarDiskPath(),
                        "legacyMappings");
        final File defaultMappingsFolder = fld.isDirectory() ? fld : null;

        final Map<String, Resource> resources = new HashMap<>();

        if (defaultMappingsFolder != null && defaultMappingsFolder.exists()) {
            try {
                Collection<File> filesList =
                                FileUtils.listFiles(defaultMappingsFolder,
                                                new String[] {pattern}, false);
                if (filesList != null) {
                    for (File file : filesList) {
                        resources.put(file.getName(),
                                        new FileSystemResource(file));
                    }
                }
            } catch (Exception e) {
                logger.debug("Legacy mappings not found", e);
            }
        }

        for (final JahiaTemplatesPackage aPackage : ServicesRegistry
                        .getInstance().getJahiaTemplateManagerService()
                        .getAvailableTemplatePackages()) {
            final Bundle bundle = aPackage.getBundle();
            if (bundle != null) {
                final Enumeration<URL> resourceEnum =
                                bundle.findEntries("META-INF/legacyMappings",
                                                "*." + pattern, false);
                if (resourceEnum == null)
                    continue;
                while (resourceEnum.hasMoreElements()) {
                    BundleResource bundleResource = new BundleResource(
                                    resourceEnum.nextElement(), bundle);
                    resources.put(bundleResource.getFilename(), bundleResource);
                }
            }
        }

        return resources;
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
                file = File.createTempFile(importFile.getOriginalFilename(),
                                ".tmp");
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
            if (StringUtils.startsWith(selectedPrepackagedSite, "bundle")) {
                String[] bundleInfos =
                                StringUtils.split(selectedPrepackagedSite, "#");
                ZipInputStream zis = null;
                try {
                    BundleResource resource = new BundleResource(
                                    new URL(bundleInfos[0]),
                                    ServicesRegistry.getInstance()
                                                    .getJahiaTemplateManagerService()
                                                    .getTemplatePackageById(
                                                                    bundleInfos[1])
                                                    .getBundle());
                    zis = new ZipInputStream(new BufferedInputStream(
                                    resource.getInputStream()));
                    prepareFileImports(zis, resource.getFilename(),
                                    messageContext);
                } catch (MalformedURLException e) {
                    logger.error("Unable to parse url from "
                                    + selectedPrepackagedSite);
                } catch (IOException e) {
                    logger.error("Unable to read file from "
                                    + selectedPrepackagedSite);
                } finally {
                    IOUtils.closeQuietly(zis);
                }

            } else {
                File f = new File(selectedPrepackagedSite);

                if (f.exists()) {
                    prepareFileImports(f, f.getName(), messageContext);
                }
            }
        }
    }

    private ImportInfo prepareSiteImport(File i, String filename,
                    MessageContext messageContext) throws IOException {
        ImportInfo importInfos = new ImportInfo();
        importInfos.setImportFile(i);
        importInfos.setImportFileName(filename);
        importInfos.setSelected(Boolean.TRUE);
        if (importProperties != null && !importProperties.isEmpty()) {
            importInfos.setOriginatingJahiaRelease(
                            importProperties.getProperty("JahiaRelease"));
            final String buildNumber =
                            importProperties.getProperty("BuildNumber");
            if (buildNumber != null) {
                importInfos.setOriginatingBuildNumber(
                                Integer.parseInt(buildNumber));
            }
        }
        if (filename.endsWith(".xml")) {
            importInfos.setType("xml");
        } else if (filename.endsWith("systemsite.zip")) {
            importInfos.setType("files");
        } else {
            List<String> installedModules = readInstalledModules(i);
            ZipEntry z;
            ZipInputStream zis2 = i.isDirectory()
                            ? new DirectoryZipInputStream(i)
                            : new NoCloseZipInputStream(new BufferedInputStream(
                                            new FileInputStream(i)));

            boolean isSite = false;
            boolean isLegacySite = false;
            boolean isLegacyImport = false;
            if ("6.1".equals(importInfos.getOriginatingJahiaRelease()))
                isLegacyImport = true;
            try {
                while ((z = zis2.getNextEntry()) != null) {
                    final String name = z.getName();
                    if ("site.properties".equals(name)) {
                        Properties p = new Properties();
                        p.load(zis2);
                        importInfos.loadSiteProperties(p);

                        importInfos.setTemplates("");
                        if (p.containsKey("templatePackageName")) {
                            JahiaTemplatesPackage pack = templateManagerService
                                            .getTemplatePackageById((String) p
                                                            .get("templatePackageName"));
                            if (pack == null) {
                                pack = templateManagerService
                                                .getTemplatePackage((String) p
                                                                .get("templatePackageName"));
                            }
                            if (pack != null) {
                                importInfos.setTemplates(pack.getId());
                            }
                        }
                        importInfos.setOldSiteKey(importInfos.getSiteKey());
                        isSite = true;
                    } else if (name.startsWith("export_")) {
                        isLegacySite = true;
                    } else if (validityCheckOnImport && !isLegacyImport
                                    && name.contains("repository.xml")
                                    && !name.contains("/")) {
                        try {
                            long timer = System.currentTimeMillis();
                            ValidationResults validationResults =
                                            importExportBaseService
                                                            .validateImportFile(
                                                                            JCRSessionFactory
                                                                                            .getInstance()
                                                                                            .getCurrentUserSession(),
                                                                            zis2,
                                                                            "application/xml",
                                                                            installedModules);
                            final String[] messageParams = {filename, name,
                                    String.valueOf((System.currentTimeMillis()
                                                    - timer)),
                                    validationResults.toString()};
                            final boolean hasValidationErrors =
                                            !validationResults.isSuccessful();
                            if (hasValidationErrors) {
                                logger.error("Failed validation {}/{} validated in {} ms: {}",
                                                messageParams);
                            } else {
                                logger.info("Successful Import {}/{} validated in {} ms: {}",
                                                messageParams);
                            }
                            if (hasValidationErrors) {
                                if (importInfos.getValidationResult() != null) {
                                    // merge results
                                    importInfos.setValidationResult(importInfos
                                                    .getValidationResult()
                                                    .merge(validationResults));
                                } else {
                                    importInfos.setValidationResult(
                                                    validationResults);
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error when validating import file",
                                            e);
                        }
                    }
                    zis2.closeEntry();
                }
            } finally {
                if (zis2 instanceof NoCloseZipInputStream) {
                    ((NoCloseZipInputStream) zis2).reallyClose();
                }
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

		boolean successful = true;
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
                } catch (RepositoryException | IOException e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    FileUtils.deleteQuietly(file);
                }
                break;
            }
        }

        Set<File> files = new HashSet<>();
        for (ImportInfo infos : importsInfos.values()) {
            files.add(infos.getImportFile());
        }

        try {
            for (final ImportInfo infos : importsInfos.values()) {
                if (infos.isSelected()) {
                    String type = infos.getType();
                    if (type.equals("files")) {
                        try {
                            final File file = ImportUpdateService.getInstance().updateImport(
                                    infos.getImportFile(),
                                    infos.getImportFileName(), infos.getType(), new Version(infos.getOriginatingJahiaRelease()),
                                    infos.getOriginatingBuildNumber());
                            files.add(file);
                            final JahiaSite system = sitesService.getSiteByKey(JahiaSitesService.SYSTEM_SITE_KEY);

                            final Map<String, String> pathMapping = JCRSessionFactory.getInstance().getCurrentUserSession()
                                    .getPathMapping();
                            pathMapping.put("/shared/files/", "/sites/" + system.getSiteKey() + "/files/");
                            pathMapping.put("/shared/mashups/", "/sites/" + system.getSiteKey() + "/portlets/");
                            for (final ImportInfo infos2 : importsInfos.values()) {
                                if (infos2.getOldSiteKey() != null && infos2.getSiteKey() != null && !infos2.getOldSiteKey().equals(infos2.getSiteKey())) {
                                    pathMapping.put("/sites/" + infos2.getOldSiteKey(), "/sites/" + infos2.getSiteKey());
                                }
                            }
                            JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(user, null, null, new JCRCallback<Object>() {
                                @Override
                                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                                    try {
                                        session.getPathMapping().putAll(pathMapping);
                                        importExportBaseService.importSiteZip(file == null ? null : new FileSystemResource(file),
                                                system, infos.asMap(), null, null, session);
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
                        // todo: shouldn't something been done here?

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
                            final File file = ImportUpdateService.getInstance().updateImport(
                                    infos.getImportFile(),
                                    infos.getImportFileName(), infos.getType(), new Version(infos.getOriginatingJahiaRelease()),
                                    infos.getOriginatingBuildNumber());
                            files.add(file);
                            try {
                                final String finalTpl = tpl;

                                final Resource finalLegacyMappingFilePath = getLegacyMappingsInModules("map").get(legacyImportFilePath);
                                final Resource finalLegacyDefinitionsFilePath = getLegacyMappingsInModules("cnd").get(legacyDefinitionsFilePath);

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
                                                            finalLegacyMappingFilePath, finalLegacyDefinitionsFilePath);
                                                } catch (JahiaException | IOException e) {
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
                            successful = false;
                        }
                    }
                }
            }
        } finally {
            for (ImportInfo infos : importsInfos.values()) {
                if (deleteFilesAtEnd) {
                    FileUtils.deleteQuietly(infos.getImportFile());
                }
            }
        }

        CompositeSpellChecker.updateSpellCheckerIndex();

        return successful;
    }

    private List<String> readInstalledModules(File i) throws IOException {
        List<String> modules = new LinkedList<>();
        ZipEntry z;

        ZipInputStream zis2 = i.isDirectory() ? new DirectoryZipInputStream(i)
                        : new NoCloseZipInputStream(new BufferedInputStream(
                                        new FileInputStream(i)));

        try {
            while ((z = zis2.getNextEntry()) != null) {
                try {
                    if ("site.properties".equals(z.getName())) {
                        Properties p = new Properties();
                        p.load(zis2);
                        Map<Integer, String> im = new TreeMap<>();
                        for (Object k : p.keySet()) {
                            String key = String.valueOf(k);
                            if (key.startsWith("installedModules.")) {
                                try {
                                    im.put(Integer.valueOf(StringUtils
                                                    .substringAfter(key, ".")),
                                                    p.getProperty(key));
                                } catch (NumberFormatException e) {
                                    logger.warn("Unable to parse installed module from key {}",
                                                    key);
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
            if (zis2 instanceof NoCloseZipInputStream) {
                ((NoCloseZipInputStream) zis2).reallyClose();
            }
        }
        return modules;
    }

    public void setGroupManagerService(
                    JahiaGroupManagerService groupManagerService) {
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
        this.sites = new ArrayList<>();
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

    public void setUserManagerService(
                    JahiaUserManagerService userManagerService) {
        this.userManagerService = userManagerService;
    }

    public void updateSite(SiteBean bean, MessageContext messages) {
        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance()
                            .getCurrentUserSession();
            final String siteKey = getSites().get(0).getSiteKey();
            JCRSiteNode site = sitesService.getSiteByKey(siteKey, session);
            boolean serverNameChanged = !StringUtils
                            .equals(site.getServerName(), bean.getServerName());
            if (serverNameChanged
                            || !StringUtils.equals(site.getTitle(),
                                            bean.getTitle())
                            || !StringUtils.equals(site.getDescription(),
                                            bean.getDescription())) {
                site.setServerName(bean.getServerName());
                site.setTitle(bean.getTitle());
                site.setDescription(bean.getDescription());

                sitesService.updateSystemSitePermissions(site, session);
            }

            if (!site.isDefault() && bean.isDefaultSite()) {
                sitesService.setDefaultSite(site);
            }

            // update modules
            // todo: check that module list has changed before updating it
            final List<String> modules = bean.getModules();
            sitesService.updateModules(site, modules, session);

            session.save();

            if (serverNameChanged) {
                CacheHelper.flushOutputCachesForPath(site.getJCRLocalPath(),
                                true);
                JahiaTemplateManagerService jahiaTemplateManagerService =
                                ServicesRegistry.getInstance()
                                                .getJahiaTemplateManagerService();
                for (String moduleId : site.getAllInstalledModules()) {
                    JahiaTemplatesPackage templatePackage =
                                    jahiaTemplateManagerService
                                                    .getTemplatePackageById(
                                                                    moduleId);
                    CacheHelper.flushOutputCachesForPath("/modules/"
                                    + templatePackage.getIdWithVersion()
                                    + "/templates", true);
                }
            }

            messages.addMessage(new MessageBuilder().info()
                            .code("label.changeSaved").build());
        } catch (Exception e) {
            messages.addMessage(new MessageBuilder().error()
                            .code("serverSettings.manageWebProjects.webProject.error")
                            .arg(e.getMessage()).build());
            logger.error(e.getMessage(), e);
        }
    }

    public void validateDisplayImportContent(ValidationContext context) {
        for (ImportInfo infos : importsInfos.values()) {
            if (infos.isSelected() && !NON_SITE_IMPORTS
                            .contains(infos.getImportFileName())) {
                validateSite(context.getMessageContext(), infos);
            }
        }
    }

    public void validateView(ValidationContext context) {
        if (context.getUserEvent().equals("import")) {
            MessageContext messages = context.getMessageContext();
            if (StringUtils.isEmpty(getImportPath())
                            && getImportFile().isEmpty()) {
                messages.addMessage(new MessageBuilder().error()
                                .source("importPath")
                                .code("serverSettings.manageWebProjects.fileImport.error")
                                .build());
            }
        }
    }

    private void validateSite(MessageContext messageContext, ImportInfo infos) {
        try {
            infos.setSiteTitleInvalid(
                            StringUtils.isEmpty(infos.getSiteTitle()));

            String siteKey = infos.getSiteKey();
            if (infos.isSite()) {
                boolean valid = sitesService.isSiteKeyValid(siteKey);
                if (!valid) {
                    messageContext.addMessage(new MessageBuilder().error()
                                    .source("siteKey")
                                    .code("serverSettings.manageWebProjects.invalidSiteKey")
                                    .build());
                }
                if (valid && sitesService.getSiteByKey(siteKey) != null) {
                    messageContext.addMessage(new MessageBuilder().error()
                                    .source("siteKey")
                                    .code("serverSettings.manageWebProjects.siteKeyExists")
                                    .build());
                }

                String serverName = infos.getSiteServername();
                if (infos.isLegacyImport() && (StringUtils
                                .startsWithIgnoreCase(serverName, "http://")
                                || StringUtils.startsWithIgnoreCase(serverName,
                                                "https://"))) {
                    serverName = StringUtils.substringAfter(serverName, "://");
                    infos.setSiteServername(serverName);
                }
                valid = sitesService.isServerNameValid(serverName);
                if (!valid) {
                    messageContext.addMessage(new MessageBuilder().error()
                                    .source("siteKey")
                                    .code("serverSettings.manageWebProjects.invalidServerName")
                                    .build());
                }

                if (valid && !Url.isLocalhost(serverName)
                                && sitesService.getSite(serverName) != null) {
                    messageContext.addMessage(new MessageBuilder().error()
                                    .source("siteKey")
                                    .code("serverSettings.manageWebProjects.serverNameExists")
                                    .build());
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
                        SettingsBean.getInstance()
                                        .lookupString("default_templates_set"),
                        StringUtils.EMPTY).trim();
        if (defTemplateSet.length() > 0) {
            JahiaTemplatesPackage pkg = templateManagerService
                            .getTemplatePackage(defTemplateSet);
            if (pkg == null) {
                pkg = templateManagerService
                                .getTemplatePackageById(defTemplateSet);
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
     * @throws JahiaException in case of an error
     */
    public int getNumberOfSites() throws JahiaException {
        return sitesService.getNbSites() - 1;
    }

}
